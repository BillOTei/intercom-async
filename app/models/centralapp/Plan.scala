package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Plan(
                 id: Long,
                 `object`: String,
                 name: String,
                 order: Int,
                 currency: String,
                 pricePerCycle: Double,
                 netPricePerCycle: Double,
                 billingCycle: Int,
                 logo: Option[String],
                 taxPercent: Double,
                 taxDescription: String,
                 featureList: List[String]
               )

object Plan {
  implicit val planReads: Reads[Plan] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "object").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "order").read[Int] and
      (JsPath \ "currency").read[String] and
      (JsPath \ "price_per_cycle").read[Double] and
      (JsPath \ "net_price_per_cycle").read[Double] and
      (JsPath \ "billing_cycle").read[Int] and
      (JsPath \ "logo").readNullable[String] and
      (JsPath \ "tax_percent").read[Double] and
      (JsPath \ "tax_description").read[String] and
      (JsPath \ "feature_list").read[List[String]]
    )(Plan.apply _)
}