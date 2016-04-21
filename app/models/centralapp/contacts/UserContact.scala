package models.centralapp.contacts

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserContact(
                        userId: Long,
                        token: Option[String],
                        subject: String,
                        message: Option[String],
                        whenToContact: Option[Long],
                        businessName: Option[String],
                        city: Option[String]
                      ) extends ContactRequest

object UserContact {
  val MSG_UNAUTHORIZED = "ERR.USER.UNAUTHORIZED"
  val MSG_USER_INVALID = "ERR.USER.INVALID"

  implicit val jsonReads: Reads[UserContact] = (
    (JsPath \ "user_id").read[Long] and
      (JsPath \ "token").readNullable[String] and
      (JsPath \ "subject").read[String] and
      (JsPath \ "message").readNullable[String] and
      (JsPath \ "when_to_contact").readNullable[Long] and
      (JsPath \ "business_name").readNullable[String] and
      (JsPath \ "city").readNullable[String]
    )(UserContact.apply _)

  def process(userContact: UserContact) = {
    if (userContact.whenToContact.isDefined || userContact.message.isDefined) {
      // intercom conversation
    }
  }
}