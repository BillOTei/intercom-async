package service.actors

import akka.actor.{Actor, Props}
import helpers.HttpClient
import models.centralapp.BasicUser
import models.centralapp.contacts.LeadContact
import models.centralapp.places.{BasicPlace, Place}
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User => CentralAppUser}
import models.intercom._
import models.intercom.bulk.Bulk
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import service.actors.ForwardActor.Answer

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object IntercomActor {
  def props = Props[IntercomActor]

  case class PlaceUserMessage(user: CentralAppUser, place: Place) extends IntercomMessage

  case class PlaceMessage(place: Place) extends IntercomMessage

  case class BasicPlaceUserMessage(placeUser: BasicPlaceUser) extends IntercomMessage

  case class LeadMessage(user: BasicUser, optPlaceUser: Option[BasicPlaceUser] = None) extends IntercomMessage

  case class UserMessage(user: CentralAppUser) extends IntercomMessage

  case class TagMessage(tag: Tag) extends IntercomMessage

  case class EventMessage(name: String, createdAt: Long, user: CentralAppUser, optPlace: Option[Place]) extends IntercomMessage

  case class ConversationInitMessage(conversationInit: ConversationInit) extends IntercomMessage

  case class DeleteAllPlaceUsersMessage(ownerEmail: String, placeId: Long) extends IntercomMessage

  case class BulkUserIdUpdate()
}

class IntercomActor extends Actor {
  import IntercomActor._

  io.intercom.api.Intercom.setApiKey(current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case PlaceUserMessage(user: CentralAppUser, place: Place) => (User.isValid(user), Company.isValid(place)) match {
      case (false, false) => sender ! Failure(new Throwable(s"Intercom user & company invalid: ${user.toString} ${place.toString}"))
      case (true, false) => sender ! Failure(new Throwable(s"Intercom company invalid: ${place.toString}"))
      case (false, true) => sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))
      case _ => HttpClient.postDataToIntercomApi("users", User.toJson(user, Some(place)), sender)
    }

    case PlaceMessage(place: Place) =>
      if (Company.isValid(place)) HttpClient.postDataToIntercomApi("companies", Company.toJson(place), sender)
      else sender ! Failure(new Throwable(s"Intercom company invalid: ${place.toString}"))

    case UserMessage(user: CentralAppUser) =>
      if (User.isValid(user)) HttpClient.postDataToIntercomApi("users", User.toJson(user, None), sender)
      else sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))

    case DeleteAllPlaceUsersMessage(ownerEmail, placeId) =>
      // No way of doing that in one go with Intercom API,
      // First fetch all the users belonging to this place then update each one in a bulk
      HttpClient.getAllPagedFromIntercom("companies", "users", sender, "company_id" -> placeId.toString, "type" -> "user") map {
        _ map {
          jsonUsers => {
            implicit val needAnswer = true
            HttpClient.postDataToIntercomApi(
              "bulk/users",
              Json.toJson(Bulk.getForCompanyUserDeletion(placeId, jsonUsers)).as[JsObject],
              sender
            )
          }
        }
      }

    case BulkUserIdUpdate =>

    case EventMessage(name, createdAt, user, optPlace) =>
      if ("""([\w\.]+)@([\w\.]+)""".r.unapplySeq(user.email).isDefined) {
        HttpClient.postDataToIntercomApi(
          "events",
          Json.toJson(Event(name, createdAt, user.email, user.centralAppId, optPlace.map(_.centralAppId))).as[JsObject],
          sender
        )
      } else sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))

    case ConversationInitMessage(conversationInit) =>
      // In the case of lead contact, need to create it before
      // If there is a business name and location, we add the company data
      // If not, just the user data
      {
        {
          for {
            leadContact <- conversationInit.optLeadContact
            businessName <- leadContact.businessName
            location <- leadContact.location
          } yield HttpClient.postDataToIntercomApi(
            "contacts",
            Lead.toJson(
              LeadContact.getBasicUser(leadContact),
              Some(
                BasicPlaceUser(
                  new BasicPlace {
                    override def name: String = businessName
                    override def locality: String = location
                    override def lead: Boolean = true
                  },
                  LeadContact.getBasicUser(leadContact)
                )
              )
            ),
            sender
          )
        } orElse {
          conversationInit.optLeadContact.map(lc => HttpClient.postDataToIntercomApi("contacts", Lead.toJson(LeadContact.getBasicUser(lc), None), sender))
        }
      } map(
        futureTry => futureTry.map {
          case Success(leadJson) =>
            implicit val needAnswer = true
            HttpClient.postDataToIntercomApi(
              "messages",
              Json.toJson(conversationInit.copy(optLeadId = (leadJson \ "user_id").asOpt[String])).as[JsObject],
              sender
            )
            // Tag the new created lead
            conversationInit.optLeadContact.foreach(
              leadContact => self ! TagMessage(
                Tag(
                  leadContact.subject,
                  List(LeadContact.getBasicUser(leadContact, (leadJson \ "id").asOpt[String]))
                )
              )
            )
          case Failure(e) => sender ! Answer(Failure(e))
        }
      ) getOrElse HttpClient.postDataToIntercomApi("messages", Json.toJson(conversationInit).as[JsObject], sender)

    case BasicPlaceUserMessage(placeUser) =>
      if ("""([\w\.]+)@([\w\.]+)""".r.unapplySeq(placeUser.user.email).isDefined) {
        HttpClient.postDataToIntercomApi("users", User.basicToJson(placeUser), sender)
      } else sender ! Failure(new Throwable(s"Intercom basic user invalid: ${placeUser.user.toString}"))

    case LeadMessage(user, optPlaceUser) =>
      if ("""([\w\.]+)@([\w\.]+)""".r.unapplySeq(user.email).isDefined) {
        HttpClient.postDataToIntercomApi("contacts", Lead.toJson(user, optPlaceUser), sender)
      } else sender ! Failure(new Throwable(s"Intercom basic user invalid: ${user.toString}"))

    case TagMessage(tag) =>
      HttpClient.postDataToIntercomApi("tags", Json.toJson(tag).as[JsObject], sender)

    case _ => sender ! Failure(new Throwable(s"Intercom message received unknown"))
  }
}