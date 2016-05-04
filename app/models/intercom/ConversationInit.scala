package models.intercom

import models.centralapp.contacts.LeadContact
import play.api.libs.json._

case class ConversationInit(
    message: String,
    optUserEmail: Option[String],
    optLeadId: Option[String] = None,
    optLeadContact: Option[LeadContact] = None
)

object ConversationInit {
  implicit val jsonWrites: Writes[ConversationInit] =
    new Writes[ConversationInit] {
      override def writes(o: ConversationInit): JsValue =
        Json.obj(
            "from" -> {
              if (o.optLeadId.isDefined) {
                Json.obj(
                  "type" -> "contact",
                  "user_id" -> o.optLeadId.get
                )
              } else if (o.optUserEmail.isDefined) {
                Json.obj(
                  "type" -> "user",
                  "email" -> o.optUserEmail.get
                )
              } else Json.obj()
            },
            "body" -> o.message
        )
    }
}
