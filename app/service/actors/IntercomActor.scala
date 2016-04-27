package service.actors

import akka.actor.Status.Failure
import akka.actor.{Actor, Props}
import helpers.HttpClient
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User => CentralAppUser}
import models.intercom.{Company, ConversationInit, Event, User}
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}

object IntercomActor {
  def props = Props[IntercomActor]

  case class PlaceUserMessage(user: CentralAppUser, place: Place)

  case class PlaceMessage(place: Place)

  case class BasicPlaceUserMessage(placeUser: BasicPlaceUser)

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
      HttpClient.postDataToIntercomApi(
        "messages",
        Json.toJson(conversationInit).as[JsObject],
        sender
      )

    case BasicPlaceUserMessage(placeUser) =>
      if ("""([\w\.]+)@([\w\.]+)""".r.unapplySeq(placeUser.user.email).isDefined) {
        HttpClient.postDataToIntercomApi("users", User.basicToJson(placeUser), sender)
      } else sender ! Failure(new Throwable(s"Intercom basic user invalid: ${placeUser.user.toString}"))

    case _ => sender ! Failure(new Throwable(s"Intercom message received unknown"))
  }
}