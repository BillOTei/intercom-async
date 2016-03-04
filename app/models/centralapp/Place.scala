package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Place(
                    centralAppId: Long,
                    name: String,
                    email: Option[String],
                    chainName: Option[String],
                    countryCode: String,
                    locality: String,
                    zip: String,
                    address: String,
                    streetNumber: String,
                    defaultLang: Option[String],
                    openingDates: Option[String],
                    landlinePhone: Option[String],
                    mobilePhone: Option[String],
                    website: Option[String],
                    categories: Option[JsArray],
                    signupDate: Option[String],
                    verificationStatus: Boolean,
                    completionScore: Double,
                    nbOfActionsToTake: Option[Int],
                    billing: Option[PlaceBilling],
                    attribution: Option[Attribution],
                    plan: Option[Plan]
                  )

object Place {
  def placeReads(payload: JsValue): Reads[Place] = {
    val lang = (payload \ "place" \ "languages").asOpt[List[JsObject]].
      map(_.filter(lg => (lg \ "default").asOpt[Boolean].getOrElse(false))).
      getOrElse(List.empty[JsObject]).
      headOption.map(lg => (lg \ "code").as[String]).
      orElse((payload \ "user" \ "language" \ "code").asOpt[String]).getOrElse("")

    (
      (JsPath \ "id").read[Long] and
      (JsPath \ "name").read[String] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "chain" \ "name").readNullable[String] and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "country" \ "short_name").read[String].
        orElse((JsPath \ "location" \ "address" \ "country" \ "short_name").read[String]) and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "locality" \ "name").read[String].
        orElse((JsPath \ "location" \ "address" \ "locality" \ "name").read[String]) and
      (JsPath \ "location" \ "address" \ "locality" \ "postal_code").read[String] and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "street" \ "name").read[String].
        orElse((JsPath \ "location" \ "address" \ "street" \ "name").read[String]) and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "street_number").read[String].
        orElse((JsPath \ "location" \ "address" \ "street_number").read[String]) and
      (JsPath \ "defaultLang").readNullable[String] and
      (JsPath \ "openingDates").readNullable[String] and
      (JsPath \ "primary_phone" \ "international").readNullable[String] and
      (JsPath \ "mobile" \ "international").readNullable[String] and
      (JsPath \ "website").readNullable[String] and
      (JsPath \ "categories").readNullable[JsArray] and
      (JsPath \ "created").readNullable[String] and
      (JsPath \ "verified").read[Boolean] and
      (JsPath \ "completion_percent").read[Double] and
      (JsPath \ "nbOfActionsToTake").readNullable[Int] and
      (JsPath \ "billing").readNullable[PlaceBilling] and
      (JsPath \ "attribution").readNullable[Attribution] and
      (JsPath \ "plan").readNullable[Plan]
      )(Place.apply _)
  }
}