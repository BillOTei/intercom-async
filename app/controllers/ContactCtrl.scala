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
  /**
    * The contact endpoint for authenticated users
    *
    * @return
    */
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

  @ApiOperation(
    value = "Endpoint to receive a contact request from non authenticated user",
    notes = "Only logged out users can use this endpoint. Logic heavily relies on Intercom conversations endpoints" +
      " as it is the only service used atm. Users also called Leads are created on each request " +
      "and companies are created if name and location parameters are present. " +
      "Conversation is initiated only if message or when to contact are present. Tag is created on the lead user" +
      "with subject.",
    response = classOf[Result],
    httpMethod = "POST",
    nickname = "user contact forwarding endpoint"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "name", value = "Contact supplied name", required = true, dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "email", value = "Contact supplied email", required = true, dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "phone", value = "Contact supplied phone", required = true, dataType = "String", paramType = "body"),
      new ApiImplicitParam(name = "language", value = "Contact optional language", dataType = "String", paramType = "body"),
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
      new ApiResponse(code = 400, message = "Multiple possible formatted msgs, cf core json parsing doc.")
    )
  )
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
