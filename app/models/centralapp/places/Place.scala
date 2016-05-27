package models.centralapp.places

import java.util

import models.centralapp.{Attribution, Plan}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Place(
                    centralAppId: Long,
                    name: String,
                    email: Option[String],
                    chainName: Option[String],
                    countryCode: Option[String],
                    locality: Option[String],
                    zip: Option[String],
                    address: Option[String],
                    streetNumber: Option[String],
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
                  ) extends BasicPlace

object Place {
  val REL_CENTRALAPP_ADMIN = "CA"
  val REL_OWNER = "O"
  val REL_ADMIN = "A"
  val REL_VIEWER = "V"
  val CAN_DELETE_REL_TYPES = util.Arrays.asList(REL_CENTRALAPP_ADMIN, REL_OWNER)

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
      (JsPath \ "location" \ "translated_addresses" \ lang \ "country" \ "short_name").readNullable[String].
        orElse((JsPath \ "location" \ "address" \ "country" \ "short_name").readNullable[String]).
        orElse((JsPath \ "location").readNullable[String]) and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "locality" \ "name").readNullable[String].
        orElse((JsPath \ "location" \ "address" \ "locality" \ "name").readNullable[String]).
        orElse((JsPath \ "location").readNullable[String]) and
      (JsPath \ "location" \ "address" \ "locality" \ "postal_code").readNullable[String].
        orElse((JsPath \ "location").readNullable[String]) and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "street" \ "name").readNullable[String].
        orElse((JsPath \ "location" \ "address" \ "street" \ "name").readNullable[String]).
        orElse((JsPath \ "location").readNullable[String]) and
      (JsPath \ "location" \ "translated_addresses" \ lang \ "street_number").readNullable[String].
        orElse((JsPath \ "location" \ "address" \ "street_number").readNullable[String]).
        orElse((JsPath \ "location").readNullable[String]) and
      (JsPath \ "defaultLang").readNullable[String] and
      (JsPath \ "established").readNullable[String] and
      (JsPath \ "primary_phone" \ "international").readNullable[String] and
      (JsPath \ "mobile" \ "international").readNullable[String] and
      (JsPath \ "website").readNullable[String] and
      (JsPath \ "categories").readNullable[JsArray] and
      (JsPath \ "created").readNullable[String] and
      (JsPath \ "verified").read[Boolean] and
      (JsPath \ "completion_percent").read[Double] and
      (JsPath \ "nbOfActionsToTake").readNullable[Int] and
      (JsPath \ "billing").readNullable[PlaceBilling] and
      new Reads[Option[Attribution]] {
        def reads(json: JsValue) = {
          JsSuccess(Some(Attribution(None, None, (payload \ "user" \ "id").asOpt[Long], (payload \ "user" \ "email").asOpt[String])))
        }
      } and
      (JsPath \ "plan").readNullable[Plan]
      )(Place.apply _)
  }
}