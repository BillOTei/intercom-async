package controllers

import controllers.actions.UserActions.authenticatedAction
import helpers.JsonError
import io.swagger.annotations.{ApiResponse, _}
import models.centralapp.contacts.{LeadContact, UserContact}
import play.api.mvc._
import play.libs.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api(value = "/contact")
class ContactCtrl extends Controller {
  val system = Akka.system()

  /**
    * The contact endpoint for authenticated users
    *
    * @return
    */
  /*
  Donc scénario 1 :
  Quand un mec s’inscrit —> Si jamais c’était déjà un lead ave qui tu as conversé , etc. Et il arrive en new user out of no-where baaaaahhh tu vas lui envoyer des message discordant —> Donc il faut matcher le lead correspondant

  vanderlindenjc [12:57 PM]
  Scénario 2 (moins important)
  Quand un lead nous contacte et qu’enfet c’est déjà un utilisateur existant —> Il faudrait ajouter sa demande à son historique pour qu’on puisse bien comprendre que c’est pas un mec nouveau out of no-where

  [12:57]
  Dans intercom tu créer des centaines de messages automatiques, etc.

  [12:58]
  et puis le support regarde les profils et appel ou envoie des emails sur base du profil

  [12:58]
  Donc imagine je suis un lead et je demande des infos sur un programme spécifique —> Je discute à travers le chat intercom avec le stagiaires business dev.

  [12:58]
  Je reçois un deal de sa part….

  [12:58]
  2 jours plustard je décide de m'inscrire

  [12:59]
  et la j’arrive dans un intercom comme un nouveau signup. Le mec va recevoir des notif et emails correspondant à un nouvea user + Max qui va l’apeller.

  [12:59]
  Donc en bref si Max ne sait pas qu’on a déjà été en contact (car il ne va pas chercher pour tout les nouveau signup)

  [1:00]
  il aura l’air con… puis il va devoir retrouver les convers, etc.
  */
  @ApiOperation(
    value = "Endpoint to receive a contact request from authenticated user",
    notes = "Only auth users with core token can use this endpoint. Logic heavily relies on Intercom conversations endpoints" +
      " as it is the only service used atm. Users are upserted " +
      "and companies are created if name and location parameters are present. " +
      "Conversation is initiated only if message or when to contact are present. Tag is created on the user" +
      "with subject.",
    response = classOf[Result],
    httpMethod = "POST",
    nickname = "user contact forwarding endpoint"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "user_id", value = "Centralapp user_id", required = true, dataType = "int", paramType = "body"),
      new ApiImplicitParam(name = "token", value = "Centralapp user token query string or body", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "token", value = "Centralapp user token query string or body", dataType = "string", paramType = "body"),
      new ApiImplicitParam(name = "subject", value = "Message subject", required = true, dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "message", value = "Optional message body", dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "when_to_contact", value = "Optional user inputed best time to be contacted", dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "business_name", value = "The optional user provided company name", dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "location", value = "The optional user provided company location", dataType = "String", paramType = "body")
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 202, message = ""),
      new ApiResponse(code = 400, message = "Multiple possible formatted msgs, cf core json parsing doc."),
      new ApiResponse(code = 401, message = "ERR.USER.UNAUTHORIZED")
    )
  )
  def userContact = authenticatedAction.async(parse.json) {
    implicit request =>
      request.body.validate[UserContact].map {
        case uc: UserContact =>
          if (uc.userId == request.user.centralAppId) {

            UserContact.process(uc, request.user.email)
            Future(Accepted)
          } else Future(Unauthorized(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }

  /**
    * The contact endpoint for lead users
    *
    * @return
    */
  def leadContact = Action.async(parse.json) {
    implicit request =>
      request.body.validate[LeadContact].map {
        case lc: LeadContact =>
          // Checks to see whether the user already contacted us or not (users and leads)
          // Not done atm
          /*HttpClient.getFromIntercomApi("users", "email" -> lc.email) map {
            case Success(json) if json == JsNull =>
              // No user found good, let's check leads now
              HttpClient.getFromIntercomApi("contacts", "email" -> lc.email) map {
                case Success(listJson) =>
                case Failure(e) => InternalServerError(JsonError.stringError(e.getMessage))
              }

            case Success(userJson) => Ok(userJson)
              // User found so conversation and data go to him (and delete the potential leads)
              (userJson \ "custom_attributes" \ "centralapp_id").asOpt[Long] match {
                case Some(userId) =>
                case _ =>
                  // Nasty case
                  Logger.error(s"Lead contact: intercom user found but no centralapp_id: ${lc.email}")
                  BadRequest(JsonError.stringError(UserContact.MSG_USER_INVALID))
              }

            case Failure(e) => InternalServerError(JsonError.stringError(e.getMessage))
          }*/
          LeadContact.process(lc)
          Future(Accepted)
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
