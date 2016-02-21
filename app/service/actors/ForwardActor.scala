package service.actors

import akka.actor.{Props, Actor}

import models.{Response, Message}

import play.Logger

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward(msg: Message)
}

class ForwardActor extends Actor {
  import ForwardActor._

  def receive = {
    case Forward(msg: Message) =>
      msg.service match {
        case "intercom" =>
          Logger.info("Forwarding message to intercom...")

        case _ =>
          Logger.warn(s"Service ${msg.service} not implemented yet")
          sender ! Response(status = false, msg.payload.toString)
      }
  }
}