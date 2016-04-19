package controllers

import models.Message
import models.centralapp.contacts.UserContact
import play.api.mvc._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactCtrl extends Controller {
  val system = Akka.system()

  def userContact = Action.async(parse.json) {
    implicit request => Future {
      request.body.validate[UserContact].map {
        case uc: UserContact =>

          Ok(uc.message)
      }.recoverTotal {
        e => BadRequest("that's bad")
      }
    }
  }
}
