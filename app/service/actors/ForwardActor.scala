package service.actors

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.centralapp.{Place, User}
import models.{Message, Response}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess}
import service.actors.IntercomActor.{PlaceMessage, PlaceUserMessage}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward(msg: Message)
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
    case Forward(msg: Message) =>
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
            Logger.info("Forwarding place-update to intercom...")
            (context.actorOf(IntercomActor.props) ? PlaceMessage(p.value)).mapTo[Response].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
            }
          case e: JsError => Logger.error(s"Place invalid ${e.toString}")
        }

        case _ => Logger.warn(s"Service ${msg.event} not implemented yet")
      }
  }
}