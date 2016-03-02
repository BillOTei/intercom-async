package models.intercom

import io.intercom.api.{Company => IntercomCompany, CustomAttribute}
import models.centralapp.{Category, Place}
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.util.Try

object Company {
  /**
    * Get the Intercom comapny obj
    *
    * @param company: the company wrapper for us
    * @return
    */
  def getBasicIntercomCompany(company: Place): IntercomCompany = new IntercomCompany().
      setCompanyID("centralapp-" + company.centralAppId.toString).
      addCustomAttribute(CustomAttribute.newLongAttribute("place_id", company.centralAppId)).
      setName(company.name).
      addCustomAttribute(CustomAttribute.newStringAttribute("place_email", company.email.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("in_chain", company.chainName.isDefined)).
      addCustomAttribute(CustomAttribute.newStringAttribute("chain_name", company.chainName.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("country_code", company.countryCode)).
      addCustomAttribute(CustomAttribute.newStringAttribute("locality", company.locality)).
      addCustomAttribute(CustomAttribute.newStringAttribute("zip_code", company.zip)).
      addCustomAttribute(CustomAttribute.newStringAttribute("full_address", company.address)).
      addCustomAttribute(CustomAttribute.newStringAttribute("default_language", company.defaultLang.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("opening_date", company.openingDates.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("primary_phone", company.landlinePhone.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("mobile_phone", company.mobilePhone.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("website", company.website.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute(
        "primary_cat_first_lvl",
        Category.getPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")
      )).
      addCustomAttribute(CustomAttribute.newStringAttribute(
        "primary_cat_last_lvl",
        Category.getLastPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")
      )).
      setRemoteCreatedAt(DateTime.parse(company.signupDate).getMillis / 1000).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("verification_status", company.verificationStatus)).
      addCustomAttribute(CustomAttribute.newDoubleAttribute("completion_score", company.completionScore)).
      addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_actions_to_take", company.nbOfActionsToTake.getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("distributor_name", company.attribution.flatMap(_.distribName).getOrElse(""))).
      addCustomAttribute(CustomAttribute.newLongAttribute("owner_user_id", company.attribution.flatMap(_.creatorCentralAppId).getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("owner_user_email", company.attribution.flatMap(_.creatorEmail).getOrElse("")))

  /**
    * Create a company at Intercom
    *
    * @param company: the company wrapper (our place)
    * @return
    */
  def createBasicCompany(company: Place): Try[IntercomCompany] = Try(IntercomCompany.create(getBasicIntercomCompany(company)))

  /**
    * Basic validation of the company data
    * not the best email regex but just sanity check
    *
    * @param company: the place company data
    * @return
    */
  def isValid(company: Place): Boolean = {
    company.centralAppId > 0 && !company.name.isEmpty && !company.countryCode.isEmpty && !company.locality.isEmpty &&
    !company.zip.isEmpty && !company.address.isEmpty && (company.email.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.email.get).isDefined)
    /*&&
    company.attribution.creatorCentralAppId.isDefined &&
    (company.attribution.creatorEmail.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.attribution.creatorEmail.get).isDefined)*/
  }

  /**
    * The company to json obj
    * @param company: the intercom company, meaning a place for centralapp
    * @return
    */
  def toJson(company: Place) = Json.obj(
    "name" -> company.name,
    "company_id" -> ("centralapp-" + company.centralAppId.toString),
    "remote_created_at" -> DateTime.parse(company.signupDate).getMillis / 1000,
    "custom_attributes" -> Json.obj(
      "place_id" -> company.centralAppId,
      "place_email" -> company.email,
      "in_chain" -> company.chainName.isDefined,
      "chain_name" -> company.chainName.getOrElse(""),
      "country_code" -> company.countryCode,
      "locality" -> company.locality,
      "zip_code" -> company.zip,
      "full_address" -> company.address,
      "default_language" -> company.defaultLang.getOrElse(""),
      "opening_date" -> company.openingDates.getOrElse(""),
      "primary_phone" -> company.landlinePhone.getOrElse(""),
      "mobile_phone" -> company.mobilePhone.getOrElse(""),
      "website" -> company.website.getOrElse(""),
      "primary_cat_first_lvl" -> Category.getPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse(""),
      "primary_cat_last_lvl" -> Category.getLastPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse(""),
      "verification_status" -> company.verificationStatus,
      "completion_score" -> company.completionScore,
      "nb_of_actions_to_take" -> company.nbOfActionsToTake.getOrElse(0),
      "distributor_name" -> company.attribution.flatMap(_.distribName).getOrElse(""),
      "owner_user_id" -> company.attribution.flatMap(_.creatorCentralAppId).getOrElse(0).toString,
      "owner_user_email" -> company.attribution.flatMap(_.creatorEmail).getOrElse("")
    )
  )
}