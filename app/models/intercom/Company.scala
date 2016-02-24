package models.intercom

import io.intercom.api.{User => IntercomUser, CustomAttribute}

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

case class Company(
  centralAppId: Long,
  name: String,
  email: String,
  inChain: Boolean,
  chainName: Option[String],
  countryCode: String,
  locality: String,
  zip: String,
  address: String,
  defaultLang: String = "en",
  openDate: Long,
  landlinePhone: Option[String],
  mobilePhone: Option[String],
  website: Option[String],
  primaryCatFirstLvl: Option[String],
  primaryCatLastLvl: Option[String],
  signupDate: Long = System.currentTimeMillis,
  verificationStatus: String,
  completionScore: Double,
  nbOfActionsToTake: Int,
  billing: Option[CompanyBilling],
  attribution: Attribution
)

object Company {
  implicit val companyReads: Reads[Company] = (
    (JsPath \ "centralAppId").read[Long] and
    (JsPath \ "name").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "inChain").read[Boolean] and
    (JsPath \ "chainName").readNullable[String] and
    (JsPath \ "countryCode").read[String] and
    (JsPath \ "locality").read[String] and
    (JsPath \ "zip").read[String] and
    (JsPath \ "address").read[String] and
    (JsPath \ "defaultLang").read[String] and
    (JsPath \ "openDate").read[Long] and
    (JsPath \ "landlinePhone").readNullable[String] and
    (JsPath \ "mobilePhone").readNullable[String] and
    (JsPath \ "website").readNullable[String] and
    (JsPath \ "primaryCatFirstLvl").readNullable[String] and
    (JsPath \ "primaryCatLastLvl").readNullable[String] and
    (JsPath \ "signupDate").read[Long] and
    (JsPath \ "verificationStatus").read[String] and
    (JsPath \ "completionScore").read[Double] and
    (JsPath \ "nbOfActionsToTake").read[Int] and
    (JsPath \ "billing").readNullable[CompanyBilling] and
    (JsPath \ "attribution").read[Attribution]
  )(Company.apply _)
}