package models.intercom

import models.billotei.BasicUser
import play.api.libs.json._

case class Tag(
              name: String,
              users: List[BasicUser],
              optCompanies: Option[JsArray] = None
              )

object Tag {
  implicit val jsonWrites: Writes[Tag] = new Writes[Tag] {
    override def writes(o: Tag): JsValue =
      Json.obj(
        "name" -> o.name
      ) ++ {
        if (o.optCompanies.isDefined) o.optCompanies.map(companies => Json.obj("companies" -> companies)).getOrElse(Json.obj())
        else Json.obj(
          "users" -> o.users.map(
            user => if (user.optIntercomId.isDefined) Json.obj("id" -> user.optIntercomId.get) else Json.obj("email" -> user.email)
          )
        )
      }
  }
}
