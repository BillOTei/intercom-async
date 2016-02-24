package service.actors

import akka.actor.Status.Failure
import akka.actor.{Props, Actor}

import models.{Response, Message}
import models.intercom.User

import play.Logger
import play.api.Play
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.util.Success

object Intercom {
  def props = Props[Intercom]

  case class GetMessage(msg: Message)

  case class Payload(action: String, user: Option[User]/*, place: Option[Company], event: Option[Event]*/)

  implicit val payloadReads: Reads[Payload] = (
      (JsPath \ "action").read[String] and
      (JsPath \ "user").readNullable[User] /*and
      (JsPath \ "place").readNullable[Company] and
      (JsPath \ "event").readNullable[Event]*/
    )(Payload.apply _)
}

class Intercom extends Actor {
  import Intercom._

  io.intercom.api.Intercom.setApiKey(Play.current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(Play.current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case GetMessage(msg: Message) => msg.payload.validate(payloadReads) match {
      case p: JsSuccess[Payload] =>
        if (p.value.user.isDefined) {
          User.createBasicIntercomUser(p.value.user.get) match {
            case Success(u) => sender ! Response(status = true, s"Intercom user created: ${u.getId}")
            case scala.util.Failure(e) => sender ! Failure(e)
          }
        } else sender ! Failure(new Throwable(s"Intercom payload unknown: ${p.value.toString}"))

      case e: JsError => sender ! Failure(new Throwable(s"Intercom payload validation failed: ${msg.payload.toString}"))
    }
  }
}