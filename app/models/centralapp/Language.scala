package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Language(
                   code: String,
                   default: Boolean
                   )

object Language {
  implicit val languageReads: Reads[Language] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "default").read[Boolean]
  )(Language.apply _)
}