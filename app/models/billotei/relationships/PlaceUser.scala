package models.billotei.relationships

import models.billotei.places.Place
import models.billotei.users.User
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PlaceUser(place: Place, user: User, optActive: Option[Boolean] = None)

object PlaceUser {

  implicit def jsonReads(implicit contextPayload: JsValue): Reads[PlaceUser] = (
    (JsPath \ "place").read[Place](Place.placeReads(contextPayload)) and
      (JsPath \ "user").read[User] and
      (JsPath \ "active").readNullable[Boolean]
    ) (PlaceUser.apply _)

  implicit def jsonListReads(implicit contextPayload: JsValue): Reads[List[PlaceUser]] = __.lazyRead(Reads.list[PlaceUser](jsonReads))

}
