package models.centralapp.relationships

import models.centralapp.places.Place
import models.centralapp.users.User
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PlaceUser(place: Place, user: User)

object PlaceUser {

  implicit def jsonReads(implicit contextPayload: JsValue): Reads[PlaceUser] = (
    (JsPath \ "place").read[Place](Place.placeReads(contextPayload)) and
      (JsPath \ "user").read[User]
    ) (PlaceUser.apply _)

  implicit def jsonListReads(implicit contextPayload: JsValue): Reads[List[PlaceUser]] = __.lazyRead(Reads.list[PlaceUser](jsonReads))

}