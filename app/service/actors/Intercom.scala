package service.actors

import akka.actor.{Props, Actor}

import models.Message

object Intercom {
  def props = Props[Intercom]

  case class GetMessage(msg: Message)
}

class Intercom extends Actor {
  import Intercom._

  def receive = {
    case GetMessage(msg: Message) =>
      sender ! "Hello, "
  }
}