package service.actors

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, Props}
import models.Response
import models.centralapp.{Place, User => CentralAppUser}
import models.intercom.{Company, User}
import play.api.Play.current
import play.api.libs.json.JsObject
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

object IntercomActor {
  def props = Props[IntercomActor]

  case class PlaceUserMessage(user: CentralAppUser, place: Place)

  case class PlaceMessage(place: Place)

  case class UserMessage(user: CentralAppUser)
}

// Todo add persistence system
class IntercomActor extends Actor {
  import IntercomActor._

  io.intercom.api.Intercom.setApiKey(current.configuration.getString("intercom.apikey").getOrElse(""))
  io.intercom.api.Intercom.setAppID(current.configuration.getString("intercom.appid").getOrElse(""))

  def receive = {
    case PlaceUserMessage(user: CentralAppUser, place: Place) => (User.isValid(user), Company.isValid(place)) match {
      case (false, false) => sender ! Failure(new Throwable(s"Intercom user & company invalid: ${user.toString} ${place.toString}"))
      case (true, false) => sender ! Failure(new Throwable(s"Intercom company invalid: ${place.toString}"))
      case (false, true) => sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))
      case _ => postDataToApi("users", User.toJson(user, Some(place)), sender)
    }

    case PlaceMessage(place: Place) =>
      if (Company.isValid(place)) postDataToApi("companies", Company.toJson(place), sender)
      else sender ! Failure(new Throwable(s"Intercom company invalid: ${place.toString}"))

    case UserMessage(user: CentralAppUser) =>
      if (User.isValid(user)) postDataToApi("users", User.toJson(user, None), sender)
      else sender ! Failure(new Throwable(s"Intercom user invalid: ${user.toString}"))

    case _ => sender ! Failure(new Throwable(s"Intercom message received unknown"))
  }

  /**
    * Post to the intercom api using WS play service
    * @param endpoint: intercom endpoint users, companies, events...
    * @param data: the json data formatted as they want
    * @param sender: the actor ref for error handling
    * @return
    */
  def postDataToApi(endpoint: String, data: JsObject, sender: ActorRef) = {
    // Not used anymore as java lib not async
    //handleIntercomResponse(User.createBasicIntercomUser(user, Some(List(place))), sender)

    // Move this call in a proper helper if needed somewhere else
    handleAsyncIntercomResponse(
      Try(
        WS.url(current.configuration.getString("intercom.apiurl").getOrElse("") + s"/$endpoint").
          withAuth(
            current.configuration.getString("intercom.appid").getOrElse(""),
            current.configuration.getString("intercom.apikey").getOrElse(""),
            WSAuthScheme.BASIC
          ).
          withRequestTimeout(5000).
          withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json").
          post(data)
      ),
      sender
    )
  }

  /**
    * Just a handler for the Try of intercom interaction
    *
    * @param t: the Try result
    * @param sender: the actor sender ref
    * @tparam T: User, Company, Event...
    */
  def handleIntercomResponse[T](t: Try[T], sender: ActorRef) = t match {
    case Success(u) => sender ! Response(status = true, s"Intercom resource upserted: ${u.toString}")
    case scala.util.Failure(e) => sender ! Failure(e)
  }

  /**
    * Handler for intercom WS call
    *
    * @param t: the Try result
    * @param sender: the actor sender ref
    * @return
    */
  def handleAsyncIntercomResponse(t: Try[Future[WSResponse]], sender: ActorRef) = t match {
    case Success(f) => f.map(
      response => {
        if (response.status == 200) sender ! Response(status = true, s"Intercom resource upserted: ${response.json}")
        else sender ! Failure(new Throwable(response.json.toString))
      }
    )
    case scala.util.Failure(e) => sender ! Failure(e)
  }
}