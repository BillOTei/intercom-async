package models

import models.centralapp.{Place, User}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Created by BillOTei on 17/02/16
  */
case class Payload(user: Option[User], place: Option[Place]/*, event: Option[Event]*/)

object Payload {
  implicit val payloadReads: Reads[Payload] = (
    (JsPath \ "user").readNullable[User] and
    (JsPath \ "place").readNullable[Place] /*and
    (JsPath \ "event").readNullable[Event]*/
    )(Payload.apply _)
}
