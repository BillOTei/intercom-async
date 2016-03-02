package service.actors

import akka.actor.{ActorSystem, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout

import models.{Response, Message}
import models.centralapp.{User, Place}

import play.Logger
import play.api.libs.concurrent.Execution.Implicits._

import IntercomActor.PlaceUserMessage
import play.api.libs.json.{JsError, JsSuccess}

import scala.util.{Failure, Success}
import scala.language.postfixOps
import scala.concurrent.duration._

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward(msg: Message)
}

class ForwardActor extends Actor {
  import ForwardActor._

  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case Forward(msg: Message) =>
      msg.event match {
        case "placeuser-creation" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] => (msg.payload \ "place").validate(Place.placeReads(u.value)) match {
            case p: JsSuccess[Place] =>
              Logger.info("Forwarding message to intercom...")
              (context.actorOf(IntercomActor.props) ? PlaceUserMessage(u.value, p.value)).mapTo[Response].onComplete {
                case Success(response) => Logger.info(response.body)
                case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
              }
            case e: JsError => Logger.error(s"Place invalid ${e.toString}")
          }
          case e: JsError => Logger.error(s"User invalid ${e.toString}")
        }
        case _ =>
          Logger.warn(s"Service ${msg.event} not implemented yet")
      }
  }
}