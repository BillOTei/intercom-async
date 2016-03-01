package service.actors

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, Props}
import models.Response
import models.centralapp.{Place, User => CentralAppUser}
import models.intercom.{Company, User}
import play.api.Play

import scala.util.{Success, Try}

object Intercom {
  def props = Props[Intercom]

  case class PlaceUserMessage(user: CentralAppUser, place: Place)
}

// Todo add persistence system
// Todo add controller access
class Intercom extends Actor {
  import Intercom._

  io.intercom.api.Intercom.setApiKey(Play.current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(Play.current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case PlaceUserMessage(user: CentralAppUser, place: Place) => (User.isValid(user), Company.isValid(place)) match {
      case (false, false) => sender ! Failure(new Throwable(s"Intercom user & place invalid: ${user.toString} ${place.toString}"))
      case (true, false) => sender ! Failure(new Throwable(s"Intercom place invalid: ${place.toString}"))
      case (false, true) => sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))
      case _ => handleIntercomResponse(User.createBasicIntercomUser(user, Some(List(place))), sender)
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