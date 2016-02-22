package models.intercom

case class Attribution(
  distribName: String,
  distribRel: String,
  utmCampaign: Option[String],
  utmTerm: Option[String],
  utmMedium: Option[String],
  utmSource: Option[String],
  utmContent: Option[String]
)