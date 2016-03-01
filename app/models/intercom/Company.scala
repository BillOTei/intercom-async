package models.intercom

import io.intercom.api.{Company => IntercomCompany, CustomAttribute}
import models.centralapp.Place

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
    addCustomAttribute(CustomAttribute.newLongAttribute("central_app_id", company.centralAppId)).
    setName(company.name).
    addCustomAttribute(CustomAttribute.newStringAttribute("email", company.email.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newBooleanAttribute("in_chain", company.inChain)).
    addCustomAttribute(CustomAttribute.newStringAttribute("chain_name", company.chainName.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("country_code", company.countryCode)).
    addCustomAttribute(CustomAttribute.newStringAttribute("locality", company.locality)).
    addCustomAttribute(CustomAttribute.newStringAttribute("zip", company.zip)).
    addCustomAttribute(CustomAttribute.newStringAttribute("address", company.address)).
    addCustomAttribute(CustomAttribute.newStringAttribute("default_lang", company.defaultLang.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("opening_dates", company.openingDates.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("landline_phone", company.landlinePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("mobile_phone", company.mobilePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("website", company.website.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("primary_cat_first_lvl", company.primaryCatFirstLvl.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("primary_cat_last_lvl", company.primaryCatLastLvl.getOrElse(""))).
    setRemoteCreatedAt(company.signupDate / 1000).
    addCustomAttribute(CustomAttribute.newBooleanAttribute("verification_status", company.verificationStatus)).
    addCustomAttribute(CustomAttribute.newDoubleAttribute("completion_score", company.completionScore)).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_actions_to_take", company.nbOfActionsToTake.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newStringAttribute("distrib_name", company.attribution.flatMap(_.distribName).getOrElse(""))).
    addCustomAttribute(CustomAttribute.newLongAttribute("creatorCentralAppId", company.attribution.flatMap(_.creatorCentralAppId).getOrElse(0))).
    addCustomAttribute(CustomAttribute.newStringAttribute("creatorEmail", company.attribution.flatMap(_.creatorEmail).getOrElse("")))

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
}