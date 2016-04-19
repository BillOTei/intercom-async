package models.centralapp.contacts

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserContact(
                        userId: Long,
                        token: String,
                        subject: String,
                        message: String,
                        whenToContact: Long
                      ) extends ContactRequest

object UserContact {
  implicit val jsonReads: Reads[UserContact] = (
    (JsPath \ "user_id").read[Long] and
      (JsPath \ "token").read[String] and
      (JsPath \ "subject").read[String] and
      (JsPath \ "message").read[String] and
      (JsPath \ "when_to_contact").read[Long]
    )(UserContact.apply _)
}