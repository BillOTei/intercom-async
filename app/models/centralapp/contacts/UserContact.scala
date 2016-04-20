package models.centralapp.contacts

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserContact(
                        userId: Long,
                        token: String,
                        subject: String,
                        message: Option[String],
                        whenToContact: Option[Long]
                      ) extends ContactRequest

object UserContact {
  implicit val jsonReads: Reads[UserContact] = (
    (JsPath \ "user_id").read[Long] and
      (JsPath \ "token").read[String] and
      (JsPath \ "subject").read[String] and
      (JsPath \ "message").readNullable[String] and
      (JsPath \ "when_to_contact").readNullable[Long]
    )(UserContact.apply _)
}