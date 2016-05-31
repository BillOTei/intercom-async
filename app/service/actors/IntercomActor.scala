package service.actors

import akka.actor.{Actor, Props}
import helpers.HttpClient
import models.centralapp.BasicUser
import models.centralapp.BasicUser.VeryBasicUser
import models.centralapp.contacts.LeadContact
import models.centralapp.places.{BasicPlace, Place}
import models.centralapp.relationships.{BasicPlaceUser, PlaceUser}
import models.centralapp.users.{User => CentralAppUser}
import models.intercom._
import models.intercom.bulk.Bulk
import play.api.{Logger, cache}
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import service.actors.ForwardActor.Answer

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object IntercomActor {
  def props = Props[IntercomActor]

  case class PlaceUserMessage(user: CentralAppUser, place: Place, removeRelationship: Boolean = false) extends IntercomMessage

  case class PlaceMessage(place: Place) extends IntercomMessage

  case class BasicPlaceUserMessage(placeUser: BasicPlaceUser) extends IntercomMessage

  case class LeadMessage(user: BasicUser, optPlaceUser: Option[BasicPlaceUser] = None) extends IntercomMessage

  case class UserMessage(user: CentralAppUser) extends IntercomMessage

  case class TagMessage(tag: Tag) extends IntercomMessage

  case class EventMessage(name: String, createdAt: Long, user: CentralAppUser, optPlace: Option[Place]) extends IntercomMessage

  case class ConversationInitMessage(conversationInit: ConversationInit) extends IntercomMessage

  case class DeleteAllPlaceUsersMessage(ownerEmail: String, placeId: Long) extends IntercomMessage

  case class BulkUserIdUpdate(users: List[VeryBasicUser]) extends IntercomMessage

  case class BulkPlaceUserUpdate(placeUsers: List[PlaceUser]) extends IntercomMessage
}

class IntercomActor extends Actor {
  import IntercomActor._

  io.intercom.api.Intercom.setApiKey(current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case PlaceUserMessage(user: CentralAppUser, place: Place, removeRelationship) => (User.isValid(user), Company.isValid(place)) match {
      case (false, false) => sender ! Failure(new Throwable(s"Intercom user & company invalid: ${user.toString} ${place.toString}"))
      case (true, false) => sender ! Failure(new Throwable(s"Intercom company invalid: ${place.toString}"))
      case (false, true) => sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))
      case _ => HttpClient.postDataToIntercomApi("users", User.toJson(user, Some(place), removeRelationship), sender)
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

    case BulkUserIdUpdate(users) =>
      getAllUsers map {
        _ map {
          usersList => {
            implicit val needAnswer = true
            val sanitizedUsers = User.sanitizeUserIdFromList(usersList, users)
            //Logger.debug(sanitizedUsers.toString)
            HttpClient.postDataToIntercomApi(
              "bulk/users",
              Json.toJson(
                Bulk.getForUserIdUpdate(
                  sanitizedUsers
                )
              ).as[JsObject],
              sender
            )
          }
        }
      }

    case BulkPlaceUserUpdate(placeUsers) =>
      val jsonPlaceUsers = placeUsers map {
        pu => User.toJson(pu.user, Some(pu.place))
      }
      Logger.debug(jsonPlaceUsers.toString)
      HttpClient.postDataToIntercomApi(
        "bulk/users",
        Json.toJson(
          Bulk.getForFullUserUpdate(
            jsonPlaceUsers
          )
        ).as[JsObject],
        sender
      )

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
                    override def locality: Option[String] = Some(location)
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

  /**
    * Get all the Intercom users from cache or API
    * @return
    */
  def getAllUsers: Future[Try[List[User]]] = cache.Cache.getAs[List[User]]("intercom_users").map(l => Future.successful(Success(l))).
      getOrElse {
      Logger.debug("Fetching all users from Intercom API")
      HttpClient.getAllPagedFromIntercom("users", "users", sender) map {
        _ map {
          jsonUsers => {
            val usersList = jsonUsers.flatMap(_.asOpt[User])
            if (usersList.length != usersList.groupBy(_.email.toLowerCase.trim).map(_._2.head).toList.length) {
              val dupEmails = usersList.groupBy(u => u.email.toLowerCase.trim).filter {case (_, lst) => lst.length > 1}.keys
              Logger.warn(
                s"Some duplicated user emails were found into list: " +
                  dupEmails.toString
              )

              Logger.warn(
                "Duplicated users are: " +
                usersList.filter(
                  u => dupEmails.exists(_.toLowerCase.trim == u.email.toLowerCase.trim)
                ).toString
              )
            }
            Logger.debug("Caching all Intercom users for 30mn")
            cache.Cache.set("intercom_users", usersList, 30.minutes)
            usersList
          }
        }
      }
    }

}