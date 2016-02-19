package models

import play.api.libs.json.JsObject
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * Created by BillOTei on 17/02/16
 */
case class Message(service: String, payload: JsObject)

object Message {
  implicit val messageReads: Reads[Message] = (
    (JsPath \ "service").read[String] and
    (JsPath \ "payload").read[JsObject]
  )(Message.apply _)

  def validate(msg: JsValue): Unit = {
    msg.validate[Message] match {
      case s: JsSuccess[Message] =>

      case e: JsError =>
    }
  }
}