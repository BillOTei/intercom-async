package controllers.actions

import helpers.WSResponseExtender.WSResponseMapper
import helpers.{HttpClient, JsonError}
import models.centralapp.contacts.UserContact
import models.centralapp.users.User
import play.api.Play._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object UserActions {

  /**
    * The user action for token authentication upon core
    */
  object authenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      {
        authHeader(request) orElse request.getQueryString("token") orElse {
          Try {
            request.body.asInstanceOf[AnyContentAsJson]
          }.toOption flatMap {
            _.asJson
          } flatMap {
            json => (json \ "token").asOpt[String]
          }
        } flatMap {
          token =>
            HttpClient.get(current.configuration.getString("auth-url").getOrElse(""), "token" -> token).toOption map {
              f => f flatMap {
                resp => resp.mapToResult[User](None)(User.userReads) match {
                  case Success(user) => block(new AuthenticatedRequest[A](user, request))
                  case Failure(err) => Future.successful(Results.Unauthorized(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
                }
              }
            }
        }
      } getOrElse Future.successful(Results.Unauthorized(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
    }

    /**
      * get the value in the authorization header
      *
      * @param r the request
      * @tparam A the request type
      * @return an optional string, if the header value is found
      */
    def authHeader[A](r: Request[A]): Option[String] =
      r.headers.get("Authorization").filter(_.matches("^Bearer .+")).map(_.replaceAll("Bearer ", ""))
  }

  class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

}
