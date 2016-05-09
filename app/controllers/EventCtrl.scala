package controllers

import helpers.JsonError
import io.swagger.annotations.{ApiResponse, _}
import models.Message
import play.api.mvc._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api(value = "/event")
class EventCtrl extends Controller {
  val system = Akka.system()

  @ApiOperation(
    value = "Endpoint to receive event messages from http requests",
    notes = """Receives an event from any service and forward it (if accepted) to any implemented
      tier service (intercom being the only one as of 05/2016). So far events accepted/forwarded are:
      user creation (user-creation) with user obj as payload /
      user update (user-update) with user obj as payload /
      place creation (placeuser-creation) with place and user objs as payload /
      place update (place-update) with place obj as payload /
      user login (user-login) with user obj as payload /
      verification request (verification-request) with date, place and user objs as payload""",
    response = classOf[Result],
    httpMethod = "POST",
    nickname = "events forwarding endpoint"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "event", value = "Event name", required = true, dataType = "string", paramType = "body"),
      new ApiImplicitParam(name = "payload", value = "Event payload as json object", required = true, dataType = "play.api.libs.json.JsObject", paramType = "body")
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = ""),
      new ApiResponse(code = 400, message = "Multiple possible formatted msgs, cf core json parsing doc.")
    )
  )
  /**
    * Endpoint to receive messages as http
    * @return
    */
  def add = Action.async(parse.json) {
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
