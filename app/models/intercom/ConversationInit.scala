package models.intercom

import play.api.libs.json._

case class ConversationInit(
    optUserEmail: Option[String],
    optLeadId: Option[Long],
    message: String
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
                  "id" -> o.optLeadId.get
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
