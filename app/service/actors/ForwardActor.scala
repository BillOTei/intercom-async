package service.actors

import akka.actor.{Props, Actor}

import models.{Response, Message}

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward(msg: Message)
}

class ForwardActor extends Actor {
  import ForwardActor._

  def receive = {
    case Forward(msg: Message) =>
      sender ! Response(200, msg.payload.toString)
  }
}