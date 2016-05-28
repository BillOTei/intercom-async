package models.intercom

import io.intercom.api.{CustomAttribute, Company => IntercomCompany}
import models.centralapp.Category
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import org.joda.time.DateTime
import play.api.libs.json.{JsString, Json}

import scala.util.Try

object Company {
  /**
    * Get the Intercom comapny obj
    * to use with caution as it can erase data if pushed empty to intercom
    *
    * @param company: the company wrapper for us
    * @return
    */
  def getBasicIntercomCompany(company: Place): IntercomCompany = new IntercomCompany().
      setCompanyID(company.centralAppId.toString).
      addCustomAttribute(CustomAttribute.newLongAttribute("place_id", company.centralAppId)).
      setName(company.name).
      addCustomAttribute(CustomAttribute.newStringAttribute("place_email", company.email.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("in_chain", company.chainName.isDefined)).
      addCustomAttribute(CustomAttribute.newStringAttribute("chain_name", company.chainName.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("country_code", company.countryCode.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("locality", company.locality.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("zip_code", company.zip.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("full_address", company.address + " " + company.streetNumber)).
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
      setRemoteCreatedAt(company.signupDate.map(DateTime.parse(_).getMillis / 1000).getOrElse(0)).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("verification_status", company.verificationStatus)).
      addCustomAttribute(CustomAttribute.newDoubleAttribute("completion_score", company.completionScore)).
      addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_actions_to_take", company.nbOfActionsToTake.getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("distributor_name", company.attribution.flatMap(_.distribName).getOrElse(""))).
      addCustomAttribute(CustomAttribute.newLongAttribute("owner_user_id", company.attribution.flatMap(_.creatorCentralAppId).getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("owner_user_email", company.attribution.flatMap(_.creatorEmail).getOrElse(""))).
      setPlan(new IntercomCompany.Plan(company.plan.map(_.name).getOrElse("")))

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
    company.centralAppId > 0 && !company.name.isEmpty && (company.email.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.email.get).isDefined)
    /*&&
    company.attribution.creatorCentralAppId.isDefined &&
    (company.attribution.creatorEmail.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.attribution.creatorEmail.get).isDefined)*/
  }

  /**
    * The company to json obj
    *
    * @param company: the intercom company, meaning a place for centralapp
    * @return
    */
  def toJson(company: Place) = {
    val customAttributes = Json.obj(
      "place_id" -> company.centralAppId,
      "place_email" -> company.email,
      "in_chain" -> company.chainName.isDefined,
      "chain_name" -> JsString(company.chainName.getOrElse("")),
      "default_language" -> JsString(company.defaultLang.getOrElse("")),
      "primary_phone" -> JsString(company.landlinePhone.getOrElse("")),
      "mobile_phone" -> JsString(company.mobilePhone.getOrElse("")),
      "website" -> JsString(company.website.getOrElse("")),
      "primary_cat_first_lvl" -> JsString(Category.getPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")),
      "primary_cat_last_lvl" -> JsString(Category.getLastPrimaryOpt(company.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")),
      "verification_status" -> company.verificationStatus,
      "completion_score" -> company.completionScore,
      "nb_of_actions_to_take" -> Json.toJson(company.nbOfActionsToTake.getOrElse(0)),
      "distributor_name" -> JsString(company.attribution.flatMap(_.distribName).getOrElse("")),
      "lead" -> company.lead
    ) ++ {
      if (company.attribution.exists(_.creatorCentralAppId.isDefined)) {
        Json.obj(
          "owner_user_id" -> company.attribution.flatMap(_.creatorCentralAppId).get.toString,
          "owner_user_email" -> JsString(company.attribution.flatMap(_.creatorEmail).getOrElse(""))
        )
      } else Json.obj()
    } ++ {
      if (company.openingDates.isDefined) Json.obj("opening_date" -> DateTime.parse(company.openingDates.get).getMillis / 1000)
      else Json.obj()
    } ++ {
      if (company.locality.isDefined) {
        Json.obj(
          "country_code" -> JsString(company.countryCode.getOrElse("not_specified")),
          "locality" -> company.locality.get,
          "zip_code" -> JsString(company.zip.getOrElse("not_specified")),
          "full_address" -> JsString(company.address.getOrElse("not_specified") + " " + company.streetNumber.getOrElse("not_specified"))
        )
      } else Json.obj()
    }

    Json.obj(
      "name" -> company.name,
      "company_id" -> JsString(company.centralAppId.toString),
      "custom_attributes" -> customAttributes,
      "plan" -> JsString(company.plan.map(_.name).getOrElse(""))
    ) ++ {
      if (company.signupDate.isDefined) {
        Json.obj(
          "remote_created_at" -> DateTime.parse(company.signupDate.get).getMillis / 1000,
          "created_at" -> DateTime.parse(company.signupDate.get).getMillis / 1000
        )
      } else Json.obj()
    }
  }

  /**
    * Gets a json for "lead" places mainly. I.E. the basic ones
    * @param basicPlaceUser: the data
    * @return
    */
  def basicToJson(basicPlaceUser: BasicPlaceUser) = Json.obj(
    "name" -> basicPlaceUser.place.name,
    "company_id" -> JsString("notregistered_" + java.util.UUID.randomUUID.toString),
    "custom_attributes" -> Json.obj(
      "locality" -> JsString(basicPlaceUser.place.locality.getOrElse("not_specified")),
      "owner_user_email" -> JsString(basicPlaceUser.user.email),
      "lead" -> basicPlaceUser.place.lead
    )
  )
}