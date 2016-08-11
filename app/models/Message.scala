package models

import com.spingo.op_rabbit.{GenericMarshallingException, InvalidFormat, MismatchedContentType, RabbitUnmarshaller}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

/**
 * Created by BillOTei on 17/02/16
 */
case class Message[+T](event: String, payload: JsObject, optPayloadObj: Option[T] = None)

object Message {

  implicit val messageReads: Reads[Message[Nothing]] = (
    (JsPath \ "event").read[String] and
    (JsPath \ "payload").read[JsObject]
  )((event, payload) => Message[Nothing](event, payload))

  /**
    * Unmarshaller for rabbitmq message
    * @return
    */
  implicit def playJsonRabbitUnmarshaller: RabbitUnmarshaller[Message[Nothing]] = {

    new RabbitUnmarshaller[Message[Nothing]] {
      def unmarshall(value: Array[Byte], contentType: Option[String], charset: Option[String]): Message[Nothing] = {
        contentType match {
          case Some(cs) if cs != "application/json" && cs != "text/json" =>
            throw MismatchedContentType(cs, "application/json")

          case _ => Try(new String(value, charset getOrElse "UTF-8")) match {

              case Success(str) => Json.parse(str).validate(messageReads) match {
                case m: JsSuccess[Message[Nothing]] => m.value

                case e: JsError => throw InvalidFormat(str, e.toString)
              }

              case Failure(e) => throw GenericMarshallingException(s"Conversion to charset of type $charset; ${e.getMessage}")
            }
        }
      }
    }
  }

}