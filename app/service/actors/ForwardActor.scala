package service.actors

import akka.actor.{ActorSystem, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout

import models.{Payload, Response, Message}

import play.Logger
import play.api.libs.concurrent.Execution.Implicits._

import Intercom.GetMessage

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
        case "placeuser-creation" => (msg.payload.user, msg.payload.place) match {
          case (u: User, p: Com)
        }

          Logger.info("Forwarding message to intercom...")
          (context.actorOf(Intercom.props) ? GetMessage(msg)).mapTo[Response].onComplete {
            case Success(response) => Logger.info(response.body)
            case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}")
          }
        case _ =>
          Logger.warn(s"Service ${msg.event} not implemented yet")
      }
  }
}