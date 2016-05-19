import helpers.JsonError
import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  val MSG_INTERNAL_SERVER_ERR = "ERR.EVENTS.INTERNAL"
  val MSG_BAD_REQUEST = "ERR.BAD_REQUEST"
  val MSG_UNAUTHORIZED = "ERR.UNAUTHORIZED"
  val MSG_NOT_ALLOWED = "ERR.NOT_ALLOWED"
  val MSG_NOT_FOUND = "ERR.NOT_FOUND"
  val MSG_METHOD_NOT_ALLOWED = "ERR.METHOD_NOT_ALLOWED"
  val MSG_REQUEST_TIMEOUT = "ERR.REQUEST_TIMEOUT"

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful {
      statusCode match {
        case 401 => Status(statusCode)(JsonError.stringError(MSG_UNAUTHORIZED))
        case 403 => Status(statusCode)(JsonError.stringError(MSG_NOT_ALLOWED))
        case 404 => Status(statusCode)(JsonError.stringError(MSG_NOT_FOUND))
        case 405 => Status(statusCode)(JsonError.stringError(MSG_METHOD_NOT_ALLOWED))
        case 408 => Status(statusCode)(JsonError.stringError(MSG_REQUEST_TIMEOUT))
        case _ => Status(statusCode)(JsonError.stringError(MSG_BAD_REQUEST))
      }
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      InternalServerError(JsonError.stringError(MSG_INTERNAL_SERVER_ERR))
    )
  }

}