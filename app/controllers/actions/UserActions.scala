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

  object authenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      {
        request.getQueryString("token") orElse {
          Try {
            request.body.asInstanceOf[AnyContentAsJson]
          }.toOption flatMap {
            _.asJson
          } flatMap {
            json => (json \ "token").asOpt[String]
          }
        } flatMap {
          token =>
            HttpClient.get(current.configuration.getString("coreservice.user.url").getOrElse(""), "token" -> token).toOption map {
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
  }

  class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

}