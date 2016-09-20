package models.intercom

import io.intercom.api.{CustomAttribute, Company => IntercomCompany}
import models.billotei.Category
import models.billotei.places.Place
import models.billotei.relationships.BasicPlaceUser
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
      setCompanyID(company.placePart1.centralAppId.toString).
      addCustomAttribute(CustomAttribute.newLongAttribute("place_id", company.placePart1.centralAppId)).
      setName(company.placePart1.name).
      addCustomAttribute(CustomAttribute.newStringAttribute("place_email", company.placePart1.email.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("in_chain", company.placePart1.chainName.isDefined)).
      addCustomAttribute(CustomAttribute.newStringAttribute("chain_name", company.placePart1.chainName.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("country_code", company.placePart1.countryCode.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("locality", company.placePart1.locality.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("zip_code", company.placePart1.zip.getOrElse("not_specified"))).
      addCustomAttribute(CustomAttribute.newStringAttribute("full_address", company.placePart1.address + " " + company.placePart1.streetNumber)).
      addCustomAttribute(CustomAttribute.newStringAttribute("default_language", company.placePart1.defaultLang.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("opening_date", company.placePart1.openingDates.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("primary_phone", company.placePart1.landlinePhone.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("mobile_phone", company.placePart1.mobilePhone.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute("website", company.placePart2.website.getOrElse(""))).
      addCustomAttribute(CustomAttribute.newStringAttribute(
        "primary_cat_last_lvl",
        Category.getPrimaryOpt(company.placePart2.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")
      )).
      addCustomAttribute(CustomAttribute.newStringAttribute(
        "primary_cat_first_lvl",
        Category.getLastPrimaryOpt(company.placePart2.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")
      )).
      setRemoteCreatedAt(company.placePart2.signupDate.map(DateTime.parse(_).getMillis / 1000).getOrElse(0)).
      addCustomAttribute(CustomAttribute.newBooleanAttribute("verification_status", company.placePart2.verificationStatus)).
      addCustomAttribute(CustomAttribute.newDoubleAttribute("completion_score", company.placePart2.completionScore)).
      addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_actions_to_take", company.placePart2.nbOfActionsToTake.getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("distributor_name", company.placePart2.attribution.flatMap(_.distribName).getOrElse(""))).
      addCustomAttribute(CustomAttribute.newLongAttribute("owner_user_id", company.placePart2.attribution.flatMap(_.creatorCentralAppId).getOrElse(0))).
      addCustomAttribute(CustomAttribute.newStringAttribute("owner_user_email", company.placePart2.attribution.flatMap(_.creatorEmail).getOrElse("")))

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
    company.placePart1.centralAppId > 0 && !company.placePart1.name.isEmpty && (company.placePart1.email.isEmpty ||
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r.unapplySeq(company.placePart1.email.get).isDefined)
    /*&&
    company.placePart2.attribution.creatorCentralAppId.isDefined &&
    (company.placePart2.attribution.creatorEmail.isEmpty || """([\w\.]+)@([\w\.]+)""".r.unapplySeq(company.placePart2.attribution.creatorEmail.get).isDefined)*/
  }

  /**
    * The company to json obj
    *
    * @param company: the intercom company, meaning a place for centralapp
    * @param remove: remove the company (i.e. remove the relationship with user and could make it unvisible to UI if last one)
    * @return
    */
  def toJson(company: Place, remove: Boolean = false) = {
    val customAttributes = Json.obj(
      "place_id" -> company.placePart1.centralAppId,
      "place_email" -> company.placePart1.email,
      "in_chain" -> company.placePart1.chainName.isDefined,
      "chain_name" -> JsString(company.placePart1.chainName.getOrElse("")),
      "default_language" -> JsString(company.placePart1.defaultLang.getOrElse("")),
      "primary_phone" -> JsString(company.placePart1.landlinePhone.getOrElse("")),
      "mobile_phone" -> JsString(company.placePart1.mobilePhone.getOrElse("")),
      "website" -> JsString(company.placePart2.website.getOrElse("")),
      "primary_cat_last_lvl" -> JsString(Category.getPrimaryOpt(company.placePart2.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")),
      "primary_cat_first_lvl" -> JsString(Category.getLastPrimaryOpt(company.placePart2.categories.get).flatMap(c => (c \ "name").asOpt[String]).getOrElse("")),
      "verification_status" -> {
        if (company.placePart2.verificationStatus) "yes" else if (company.placePart2.verificationId.isDefined) "requested" else "no"
      },
      "completion_score" -> company.placePart2.completionScore,
      "nb_of_actions_to_take" -> Json.toJson(company.placePart2.nbOfActionsToTake.getOrElse(0)),
      "distributor_name" -> JsString(company.placePart2.attribution.flatMap(_.distribName).getOrElse("")),
      "lead" -> false
    ) ++ {
      if (company.placePart1.openingDates.isDefined) Json.obj("opening_date" -> DateTime.parse(company.placePart1.openingDates.get).getMillis / 1000)
      else Json.obj()
    } ++ {
      if (company.placePart1.locality.isDefined) {
        Json.obj(
          "country_code" -> JsString(company.placePart1.countryCode.getOrElse("not_specified")),
          "locality" -> company.placePart1.locality.get,
          "zip_code" -> JsString(company.placePart1.zip.getOrElse("not_specified")),
          "full_address" -> JsString(company.placePart1.address.getOrElse("not_specified") + " " + company.placePart1.streetNumber.getOrElse("not_specified"))
        )
      } else Json.obj()
    }

    /*++ {
      if (company.placePart2.attribution.exists(_.creatorCentralAppId.isDefined)) {
        Json.obj(
          "owner_user_id" -> company.placePart2.attribution.flatMap(_.creatorCentralAppId).get.toString,
          "owner_user_email" -> JsString(company.placePart2.attribution.flatMap(_.creatorEmail).getOrElse(""))
        )
      } else Json.obj()
    }*/

    Json.obj(
      "name" -> company.placePart1.name,
      "company_id" -> JsString(company.placePart1.centralAppId.toString),
      "custom_attributes" -> customAttributes,
      "plan" -> JsString(company.placePart2.plan.map(_.name).getOrElse(""))
    ) ++ {
      if (company.placePart2.signupDate.isDefined) {
        Json.obj(
          "remote_created_at" -> DateTime.parse(company.placePart2.signupDate.get).getMillis / 1000,
          "created_at" -> DateTime.parse(company.placePart2.signupDate.get).getMillis / 1000
        )
      } else {
        Json.obj()
      }
    } ++ {
      // Ugly but adding remove -> false bugs on Intercom...
      if (remove) Json.obj("remove" -> true)
      else Json.obj()
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
