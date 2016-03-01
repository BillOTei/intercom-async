package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CompanyBilling(
  planName: String,
  couponUsed: Boolean = false,
  yearlySpent: Double,
  paymentMethod: String,
  planPayed: Boolean,
  planExpirationDate: Long
)

object CompanyBilling {
  implicit val billingReads: Reads[CompanyBilling] = (
    (JsPath \ "planName").read[String] and
    (JsPath \ "couponUsed").read[Boolean] and
    (JsPath \ "yearlySpent").read[Double] and
    (JsPath \ "paymentMethod").read[String] and
    (JsPath \ "planPayed").read[Boolean] and
    (JsPath \ "planExpirationDate").read[Long]
  ) (CompanyBilling.apply _)
}