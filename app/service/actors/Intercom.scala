package service.actors

import akka.actor.{Props, Actor}

import models.Message
import models.intercom.User

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

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

  def receive = {
    case GetMessage(msg: Message) =>
      sender ! "Hello, "

  }
}