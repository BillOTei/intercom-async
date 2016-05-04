package helpers

import play.api.libs.json.{JsValue, Reads}
import play.api.libs.ws.WSResponse
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.util.Try

/**
  * WS response extension object for adding some handy service specific
  * operations on WSResponse
  *
  * @author Ashesh Ambasta / Alexandre Teilhet
  */
object WSResponseExtender {

  implicit class WSResponseMapper(res: WSResponse) {
    /**
      * map a response to a single result
      * @param optField the . separated (if nested) field inside the response json that contains
      *                 the list of results from the provider
      * @param reader the reader for the json for this provider
      * @tparam B the result class in which to validate the response
      * @return a Try of the result
      */
    def mapToResult[B](optField: Option[String] = None)(reader: Reads[B]): Try[B] = Try {
      traverseJson(optField) match {
        case None => throw new Throwable(play.api.i18n.Messages("error.path.missing"))
        case Some(resJson) => resJson.validate[B](reader).asOpt match {
          case Some(validRes) => validRes
          case _ => throw new Throwable(play.api.i18n.Messages("error.path.missing"))
        }
      }
    }

    /**
      * traverse a response json given the
      * @param optField optField path ("response.venues" or "response.venue", etc.)
      * @return an optional JsValue if the response has a valid JSON and the path is valid
      */
    def traverseJson(optField: Option[String]): Option[JsValue] = optField match {
      case None => Some(res.json)
      case Some(jsonField) =>
        jsonField.split('.').foldLeft (Option(res.json)) {

          case (Some(json), subField) => (json \ subField).asOpt[JsValue]
          case _ => None
        }
    }
  }

}
