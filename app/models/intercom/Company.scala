package models.intercom

import io.intercom.api.{Company => IntercomCompany, CustomAttribute}

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
  openingDates: String,
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
    (JsPath \ "openingDates").read[String] and
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

  /**
    * Get the Intercom comapny obj
    * @param company: the company wrapper for us
    * @return
    */
  def getBasicIntercomCompany(company: Company): IntercomCompany = new IntercomCompany().
    setCompanyID("centralapp-" + company.centralAppId.toString).
    addCustomAttribute(CustomAttribute.newLongAttribute("central_app_id", company.centralAppId)).
    setName(company.name).
    addCustomAttribute(CustomAttribute.newStringAttribute("email", company.email)).
    addCustomAttribute(CustomAttribute.newBooleanAttribute("in_chain", company.inChain)).
    addCustomAttribute(CustomAttribute.newStringAttribute("chain_name", company.chainName.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("country_code", company.countryCode)).
    addCustomAttribute(CustomAttribute.newStringAttribute("locality", company.locality)).
    addCustomAttribute(CustomAttribute.newStringAttribute("zip", company.zip)).
    addCustomAttribute(CustomAttribute.newStringAttribute("address", company.address)).
    addCustomAttribute(CustomAttribute.newStringAttribute("default_lang", company.defaultLang)).
    addCustomAttribute(CustomAttribute.newStringAttribute("opening_dates", company.openingDates)).
    addCustomAttribute(CustomAttribute.newStringAttribute("landline_phone", company.landlinePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("mobile_phone", company.mobilePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("website", company.website.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("primary_cat_first_lvl", company.primaryCatFirstLvl.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("primary_cat_last_lvl", company.primaryCatLastLvl.getOrElse(""))).
    setRemoteCreatedAt(company.signupDate / 1000).
    addCustomAttribute(CustomAttribute.newStringAttribute("verification_status", company.verificationStatus)).
    addCustomAttribute(CustomAttribute.newDoubleAttribute("completion_score", company.completionScore)).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_actions_to_take", company.nbOfActionsToTake)).
    addCustomAttribute(CustomAttribute.newStringAttribute("distrib_name", company.attribution.distribName.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newLongAttribute("creatorCentralAppId", company.attribution.creatorCentralAppId.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newStringAttribute("creatorEmail", company.attribution.creatorEmail.getOrElse("")))

  /**
    * Create a company at Intercom
    * @param company: the company wrapper (our place)
    * @return
    */
  def createBasicCompany(company: Company): Try[IntercomCompany] = Try(IntercomCompany.create(getBasicIntercomCompany(company)))

  /**
    * Basic validation of the company data
    * not the best email regex but just sanity check
    * @param company: the place company data
    * @return
    */
  def isValid(company: Company): Boolean = {
    company.centralAppId > 0 && !company.name.isEmpty && !company.countryCode.isEmpty && !company.locality.isEmpty &&
    !company.zip.isEmpty && !company.address.isEmpty && """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.email).isDefined &&
    company.attribution.creatorCentralAppId.isDefined &&
    (company.attribution.creatorEmail.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.attribution.creatorEmail.get).isDefined)
  }
}