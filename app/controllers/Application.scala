package controllers

import models.Message
import play.api.mvc._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward
import scala.concurrent.ExecutionContext.Implicits.global

class Application extends Controller {
  val system = Akka.system()

  def index = Action {
    Ok("Index Centralapp Events Service")
  }

  def event = Action.async {
    implicit request => scala.concurrent.Future(
      {
        for {
          json <- request.body.asJson
          msg <- json.validate(Message.messageReads).asOpt
        } yield {
          system.actorOf(ForwardActor.props) ! Forward(msg)
          Ok
        }
      } getOrElse BadRequest("MESSAGE.INVALID")
    )
  }
}
