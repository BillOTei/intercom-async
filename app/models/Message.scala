package models

import play.api.Play
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.JavaConversions._

/**
 * Created by BillOTei on 17/02/16
 */
case class Message(service: String, payload: JsObject)

object Message {
  implicit val messageReads: Reads[Message] = (
    (JsPath \ "service").read[String] and
    (JsPath \ "payload").read[JsObject]
  )(Message.apply _)

  def asOption(msg: JsValue): Option[Message] = {
    msg.validate[Message] match {
      case s: JsSuccess[Message] => s.value.service match {
        case clientName if Play.current.configuration.getStringList("clients").getOrElse(java.util.Collections.emptyList()).
          toList.contains(clientName) => Some(s.value)
        case _ => None
      }
      case e: JsError => None
    }
  }
}