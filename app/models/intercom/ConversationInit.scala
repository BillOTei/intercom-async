package models.intercom

import play.api.libs.json._

case class ConversationInit(
                  userEmail: Option[String],
                  leadId: Long,
                  message: String
                )

object ConversationInit {
  implicit val jsonWrites: Writes[ConversationInit] = new Writes[ConversationInit] {
    override def writes(o: ConversationInit): JsValue =
      Json.obj(
        "from" -> Json.obj (
          "type" -> "user",
          "id" -> "536e564f316c83104c000020"
        ),
        "body" -> "Hey"
      )
  }
}