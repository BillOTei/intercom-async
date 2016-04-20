package controllers

import helpers.{HttpClient, JsonError}
import models.Message
import models.centralapp.contacts.UserContact
import play.api.Play._
import play.api.mvc._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ContactCtrl extends Controller {
  val system = Akka.system()

  /**
    * The contact endpoint for authenticated users
    *
    * @return
    */
  def userContact = Action.async(parse.json) {
    implicit request =>
      request.body.validate[UserContact].map {
        case uc: UserContact =>
          HttpClient.get(current.configuration.getString("coreservice.user.url").getOrElse(""), "token" -> uc.token) match {
            case Success(f) => f.map(
              response => Ok(response.json)
            )
            case Failure(e) => Future(Unauthorized(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
          }
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
