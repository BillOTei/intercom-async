package models

import models.intercom.ConversationInit
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Created by BillOTei on 17/02/16
 */
case class Message(event: String, payload: JsObject, optPayloadObj: Option[ConversationInit] = None)

object Message {
  implicit val messageReads: Reads[Message] = (
    (JsPath \ "event").read[String] and
    (JsPath \ "payload").read[JsObject]
  )((event, payload) => Message(event, payload))
}