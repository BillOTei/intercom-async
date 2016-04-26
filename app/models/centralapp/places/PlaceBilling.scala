package models.centralapp.places

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PlaceBilling(
  planName: String,
  couponUsed: Boolean = false,
  yearlySpent: Double,
  paymentMethod: String,
  planPayed: Boolean,
  planExpirationDate: Long
)

object PlaceBilling {
  implicit val billingReads: Reads[PlaceBilling] = (
    (JsPath \ "planName").read[String] and
    (JsPath \ "couponUsed").read[Boolean] and
    (JsPath \ "yearlySpent").read[Double] and
    (JsPath \ "paymentMethod").read[String] and
    (JsPath \ "planPayed").read[Boolean] and
    (JsPath \ "planExpirationDate").read[Long]
  ) (PlaceBilling.apply _)
}