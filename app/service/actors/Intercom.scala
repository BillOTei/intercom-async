package service.actors

import akka.actor.Status.Failure
import akka.actor.{ActorRef, Props, Actor}

import models.{Response, Message}
import models.intercom.{User, Company}

import play.api.Play
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.util.{Try, Success}

object Intercom {
  def props = Props[Intercom]

  case class GetMessage(msg: Message)

  case class Payload(action: String, user: Option[User], place: Option[Company]/*, event: Option[Event]*/)

  implicit val payloadReads: Reads[Payload] = (
      (JsPath \ "action").read[String] and
      (JsPath \ "user").readNullable[User] and
      (JsPath \ "place").readNullable[Company] /*and
      (JsPath \ "event").readNullable[Event]*/
    )(Payload.apply _)
}

// Todo add persistence system
// Todo add controller access
class Intercom extends Actor {
  import Intercom._

  io.intercom.api.Intercom.setApiKey(Play.current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(Play.current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case GetMessage(msg: Message) => msg.payload.validate(payloadReads) match {
      case p: JsSuccess[Payload] =>
        if (p.value.user.isDefined) {

          if (User.isValid(p.value.user.get)) handleIntercomResponse(User.createBasicIntercomUser(p.value.user.get), sender)
          else sender ! Failure(new Throwable(s"Intercom user invalid: ${p.value.toString}"))

        } else if (p.value.place.isDefined) {

          if (Company.isValid(p.value.place.get)) handleIntercomResponse(Company.createBasicCompany(p.value.place.get), sender)
          else sender ! Failure(new Throwable(s"Intercom company place invalid: ${p.value.toString}"))

        } else sender ! Failure(new Throwable(s"Intercom payload unknown: ${p.value.toString}"))

      case e: JsError => sender ! Failure(new Throwable(s"Intercom payload validation failed: ${msg.payload.toString}"))
    }
    case _ => sender ! Failure(new Throwable(s"Intercom message received unknown"))
  }

  /**
    * Just a handler for the Try of intercom interaction
    * @param t: the Try result
    * @param sender: the actor sender ref
    * @tparam T: User, Company, Event...
    */
  def handleIntercomResponse[T](t: Try[T], sender: ActorRef) = t match {
    case Success(u) => sender ! Response(status = true, s"Intercom resource created: ${u.toString}")
    case scala.util.Failure(e) => sender ! Failure(e)
  }
}