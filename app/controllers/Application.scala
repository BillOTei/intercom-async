package controllers

import helpers.JsonError
import models.Message
import play.api.mvc._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application extends Controller {
  val system = Akka.system()

  def index = Action {
    Ok("Index Centralapp Events Service")
  }

  /**
    * Endpoint to receive messages as http
    * @return
    */
  def event = Action.async(parse.json) {
    implicit request =>
      request.body.validate[Message[Nothing]].map {
        case msg: Message[Nothing] =>
          system.actorOf(ForwardActor.props) ! Forward(msg)
          Future(Ok)
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
