package service.actors

import akka.actor.{Actor, Props}
import helpers.HttpClient
import models.centralapp.BasicUser
import models.centralapp.contacts.LeadContact
import models.centralapp.places.{BasicPlace, Place}
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User => CentralAppUser}
import models.intercom._
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import service.actors.ForwardActor.Answer

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object IntercomActor {
  def props = Props[IntercomActor]

  case class PlaceUserMessage(user: CentralAppUser, place: Place)

  case class PlaceMessage(place: Place)

  case class BasicPlaceUserMessage(placeUser: BasicPlaceUser)

  case class LeadMessage(user: BasicUser, optPlaceUser: Option[BasicPlaceUser] = None)

  case class UserMessage(user: CentralAppUser)

  case class EventMessage(name: String, createdAt: Long, user: CentralAppUser, optPlace: Option[Place])

  case class ConversationInitMessage(conversationInit: ConversationInit)
}

// Todo add persistence system if needed
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
              "messges",
              Json.toJson(conversationInit.copy(optLeadId = (leadJson \ "user_id").asOpt[String])).as[JsObject],
              sender
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

    case _ => sender ! Failure(new Throwable(s"Intercom message received unknown"))
  }
}