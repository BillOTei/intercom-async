package models.intercom

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Attribution(
  distribName: String,
  distribRel: String,
  creatorCentralAppId: Option[Long],
  creatorEmail: Option[String],
  utmCampaign: Option[String],
  utmTerm: Option[String],
  utmMedium: Option[String],
  utmSource: Option[String],
  utmContent: Option[String]
)

object Attribution {
  implicit val attributionReads: Reads[Attribution] = (
    (JsPath \ "distribName").read[String] and
    (JsPath \ "distribRel").read[String] and
    (JsPath \ "creatorCentralAppId").readNullable[Long] and
    (JsPath \ "creatorEmail").readNullable[String] and
    (JsPath \ "utmCampaign").readNullable[String] and
    (JsPath \ "utmTerm").readNullable[String] and
    (JsPath \ "utmMedium").readNullable[String] and
    (JsPath \ "utmSource").readNullable[String] and
    (JsPath \ "utmContent").readNullable[String]
  )(Attribution.apply _)
}