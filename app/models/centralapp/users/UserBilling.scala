package models.centralapp.users

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserBilling(
  companyName: String,
  VAT: Option[String],
  companyAddress: String,
  companyCountry: String,
  paymentMean: String,
  activeSubscriptionsNb: Int
)

object UserBilling {
  implicit val billingReads: Reads[UserBilling] = (
    (JsPath \ "companyName").read[String] and
    (JsPath \ "VAT").readNullable[String] and
    (JsPath \ "companyAddress").read[String] and
    (JsPath \ "companyCountry").read[String] and
    (JsPath \ "paymentMean").read[String] and
    (JsPath \ "activeSubscriptionsNb").read[Int]
  )(UserBilling.apply _)
}