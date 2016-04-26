package service.actors

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.User
import models.intercom.ConversationInit
import models.{Message, Response}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess}
import service.actors.IntercomActor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward[T](msg: Message[T])

}

class ForwardActor extends Actor {

  import ForwardActor._

  implicit val timeout = Timeout(10 seconds)

  /**
    * Main forward method to the clients, so far only intercom, more to be added...
    *
    * @return
    */
  def receive = {
    case Forward(msg: Message[_]) =>
      msg.event match {
        case "placeuser-creation" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] => (msg.payload \ "place").validate(Place.placeReads(msg.payload)) match {
            case p: JsSuccess[Place] =>
              Logger.info("Forwarding placeuser-creation to intercom...")
              (context.actorOf(IntercomActor.props) ? PlaceUserMessage(u.value, p.value)).mapTo[Response].onComplete {
                case Success(response) => Logger.info(response.body)
                case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
              }
            case e: JsError => Logger.error(s"Place invalid ${e.toString}")
          }
          case e: JsError => Logger.error(s"User invalid ${e.toString}")
        }

        case "place-update" => (msg.payload \ "place").validate(Place.placeReads(msg.payload)) match {
          case p: JsSuccess[Place] =>
            Logger.info(s"Forwarding ${msg.event} to intercom...")
            (context.actorOf(IntercomActor.props) ? PlaceMessage(p.value)).mapTo[Response].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
            }
          case e: JsError => Logger.error(s"Place invalid ${e.toString}")
        }

        case "basic-placeuser-creation" => msg.optPayloadObj match {
          case Some(placeUserPayload: BasicPlaceUser) =>
            Logger.info(s"Forwarding ${msg.event} to intercom...")
            (context.actorOf(IntercomActor.props) ? BasicPlaceUserMessage(placeUserPayload)).
              mapTo[Response].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
            }
          case _ => Logger.error("BasicPlaceUser payload invalid")
        }

        case "user-creation" | "user-update" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] =>
            Logger.info(s"Forwarding ${msg.event} to intercom...")
            (context.actorOf(IntercomActor.props) ? UserMessage(u.value)).mapTo[Response].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
            }
          case e: JsError => Logger.error(s"User invalid ${e.toString}")
        }

        case "user-login" | "verification-request" =>
          // See if we need a paypload for this event or if it is possible to use only one case "event"
          (msg.payload \ "user").validate(User.userReads) match {
            case u: JsSuccess[User] =>
              Logger.info(s"Forwarding ${msg.event} event to intercom...")
              (context.actorOf(IntercomActor.props) ? EventMessage(
                msg.event,
                (msg.payload \ "created_at").asOpt[Long].getOrElse(System.currentTimeMillis / 1000),
                u.value,
                (msg.payload \ "place").asOpt(Place.placeReads(msg.payload)))).
                mapTo[Response].onComplete {
                case Success(response) => Logger.info(response.body)
                case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
              }

            case e: JsError => Logger.error(s"User invalid ${e.toString}")
          }

        case "user-contact" =>
          msg.optPayloadObj match {
            case Some(contactPayload: ConversationInit) =>
              // So far only intercom conversation contact
              Logger.info(s"Forwarding ${msg.event} to intercom...")
              (context.actorOf(IntercomActor.props) ? ConversationInitMessage(contactPayload)).
                mapTo[Response].onComplete {
                case Success(response) => Logger.info(response.body)
                case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
              }
            case _ => Logger.error("UserContact payload invalid")
          }

        case _ => Logger.warn(s"Service ${msg.event} not implemented yet")
      }
  }
}