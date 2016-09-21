package controllers

import controllers.actions.UserActions.authenticatedAction
import helpers.JsonError
import io.swagger.annotations.{ApiResponse, _}
import models.billotei.contacts.{LeadContact, UserContact}
import play.api.mvc._
import play.libs.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api(value = "/contact")
class ContactCtrl extends Controller {
  val system = Akka.system()

  @ApiOperation(
    value = "Endpoint to receive a contact request from authenticated user in you app",
    notes = """Only auth users with token can use this endpoint. Logic heavily relies on Intercom conversations endpoints
      as it is the only service used atm. Users are upserted
      and companies are created if name and location parameters are present.
      Conversation is initiated only if message or when to contact are present. Tag is created on the user
      with subject.""",
    response = classOf[Result],
    httpMethod = "POST",
    nickname = "user contact forwarding endpoint"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "user_id", value = "Your app user_id", required = true, dataType = "int", paramType = "body"),
      new ApiImplicitParam(name = "token", value = "Your user token query string or body", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "token", value = "Your user token query string or body", dataType = "string", paramType = "body"),
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
      new ApiResponse(code = 400, message = "Multiple possible formatted msgs, cf json parsing doc."),
      new ApiResponse(code = 401, message = "ERR.USER.UNAUTHORIZED")
    )
  )
  def userContact = authenticatedAction.async {
    implicit request =>
      request.body.asJson match {
        case Some(json) =>
          json.validate[UserContact].map {
            uc: UserContact =>
                UserContact.process(uc, request.user.email)
                Future(Accepted)
            }.recoverTotal {
              e => Future(BadRequest(JsonError.jsErrors(e)))
            }
        case _ => Future(BadRequest(JsonError.stringError(UserContact.MSG_USER_INVALID)))
      }
  }

  @ApiOperation(
    value = "Endpoint to receive a contact request from non authenticated user",
    notes = """Only logged out users can use this endpoint. Logic heavily relies on Intercom conversations endpoints
      as it is the only service used atm. Users also called Leads are created on each request
      and companies are created if name and location parameters are present.
      Conversation is initiated only if message or when to contact are present. Tag is created on the lead user
      with subject.""",
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
  def leadContact = Action.async(parse.json) {
    implicit request =>
      request.body.validate[LeadContact].map {
        lc =>
          LeadContact.process(lc)
          Future.successful(Accepted)
      }.recoverTotal {
        e => Future.successful(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
