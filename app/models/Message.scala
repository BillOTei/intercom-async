package models

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Created by BillOTei on 17/02/16
 */
case class Message[+T](event: String, payload: JsObject, optPayloadObj: Option[T] = None)

object Message {
  implicit val messageReads: Reads[Message[Nothing]] = (
    (JsPath \ "event").read[String] and
    (JsPath \ "payload").read[JsObject]
  )((event, payload) => Message[Nothing](event, payload))
}