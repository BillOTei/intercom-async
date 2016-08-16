package models.intercom

import play.api.libs.json._

case class Event(
                name: String,
                createdAt: Long,
                userEmail: String,
                userId: Long,
                placeId: Option[Long]
                )

object Event {
  implicit val jsonWrites: Writes[Event] = new Writes[Event] {
    override def writes(o: Event): JsValue =
      Json.obj(
        "event_name" -> o.name,
        "created_at" -> o.createdAt,
        "email" -> o.userEmail,
        "user_id" -> o.userId.toString,
        "metadata" -> {
          Json.obj(
            "centralapp_id" -> o.userId
          ) ++ {
            if (o.placeId.isDefined) Json.obj("centralapp_place_id" -> o.placeId.get)
            else Json.obj()
          }
        }
      )
  }
}