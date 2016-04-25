package controllers

import helpers.{HttpClient, JsonError}
import models.centralapp.contacts.UserContact
import play.api.Play._
import play.api.mvc._
import play.libs.Akka

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
          HttpClient.get(
            current.configuration.getString("coreservice.user.url").getOrElse(""),
            "token" -> uc.token.orElse(request.getQueryString("token")).getOrElse("")
          ) match {
            case Success(f) => f.map(
              response => {
                for {
                  userId <- (response.json \ "id").asOpt[Long]
                  id <- Option(userId == uc.userId).filter(identity)
                  userEmail <- (response.json \ "email").asOpt[String]
                } yield userEmail
              } match {
                case Some(userEmail) =>
                  UserContact.process(uc, userEmail)
                  Accepted
                case _ => BadRequest(JsonError.stringError(UserContact.MSG_USER_INVALID))
              }
            ).recoverWith {
              case _ => Future(InternalServerError) // Content useless here as this case recovers to Failure case below
            }

            case Failure(e) => Future(InternalServerError(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
          }
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
