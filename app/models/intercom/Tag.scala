package models.intercom

import models.centralapp.BasicUser
import play.api.libs.json._

case class Tag(
              name: String,
              users: List[BasicUser]
              )

object Tag {
  implicit val jsonWrites: Writes[Tag] = new Writes[Tag] {
    override def writes(o: Tag): JsValue =
      Json.obj(
        "name" -> o.name,
        "users" -> o.users.map(
          user => if (user.optIntercomId.isDefined) Json.obj("id" -> user.optIntercomId.get) else Json.obj("email" -> user.email)
        )
      )
  }
}