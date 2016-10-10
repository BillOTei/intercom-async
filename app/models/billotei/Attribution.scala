package models.billotei

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Attribution(
  distribName: Option[String],
  distribRel: Option[String],
  creatorCentralAppId: Option[Long],
  creatorEmail: Option[String],
  utmCampaign: Option[String] = Some(""),
  utmTerm: Option[String] = Some(""),
  utmMedium: Option[String] = Some(""),
  utmSource: Option[String] = Some(""),
  utmContent: Option[String] = Some("")
)

object Attribution {
  implicit val attributionReads: Reads[Attribution] = (
    (JsPath \ "distribName").readNullable[String] and
    (JsPath \ "distribRel").readNullable[String] and
    (JsPath \ "creatorCentralAppId").readNullable[Long] and
    (JsPath \ "creatorEmail").readNullable[String] and
    (JsPath \ "utmCampaign").readNullable[String] and
    (JsPath \ "utmTerm").readNullable[String] and
    (JsPath \ "utmMedium").readNullable[String] and
    (JsPath \ "utmSource").readNullable[String] and
    (JsPath \ "utmContent").readNullable[String]
  )(Attribution.apply _)

}
