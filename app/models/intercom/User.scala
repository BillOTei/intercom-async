package models.intercom

import io.intercom.api.{User => IntercomUser, CompanyCollection, CustomAttribute}
import models.centralapp.{Place, User => CentralAppUser}
import org.joda.time.DateTime

import scala.util.Try
import scala.collection.JavaConverters._

object User {
  /**
    * Deal with the java uglyness to perform some basic formatting of the intercom user
    * default timezone is used for timestamp conversion
    *
    * @param user: the user wrapper here
    * @param companies: the places list, could be as well embedded into the user but not atm to avoid mutability
    * @return
    */
  def getBasicIntercomUser(user: CentralAppUser, companies: Option[List[Place]]): IntercomUser = new IntercomUser().
    setName(user.firstName + " " + user.lastName).
    setEmail(user.email).
    addCustomAttribute(CustomAttribute.newStringAttribute("phone", user.mobilePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("ui_lang", user.uiLang)).
    addCustomAttribute(CustomAttribute.newStringAttribute("browser_lang", user.browserLang.getOrElse(""))).
    setSignedUpAt(user.signupDate / 1000).
    //setLastRequestAt(user.lastSeenDate / 1000).
    //addCustomAttribute(CustomAttribute.newStringAttribute("signup_date_db", new DateTime(user.signupDate).toString("yyyy-MM-dd"))).
    addCustomAttribute(CustomAttribute.newStringAttribute("last_seen_date_db", new DateTime(user.lastSeenDate).toString("yyyy-MM-dd"))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_pending_places", user.nbOfPendingPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_managed_places", user.nbOfManagedPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_viewable_places", user.nbOfViewablePlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_owned_places", user.nbOfOwnedPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newLongAttribute("centralapp_id", user.centralAppId)).
    setCompanyCollection(new CompanyCollection(companies.getOrElse(List.empty).map(Company.getBasicIntercomCompany).asJava))

  /**
    * Try to create a basic user on intercom's side
    * it is possible to add a place user relation by adding a companies list
    * inside the user actor
    *
    * @param user: the user wrapper here
    * @param companies: the places list, could be as well embedded into the user but not atm to avoid mutability
    * @return
    */
  def createBasicIntercomUser(user: CentralAppUser, companies: Option[List[Place]]): Try[IntercomUser] = Try(IntercomUser.create(getBasicIntercomUser(user, companies)))

  /**
    * Valid the user data
    * add Telecom validation or better user validation if necessary
    * not the best email regex but just a sanity check
    *
    * @param user: the user wrapper
    * @return
    */
  def isValid(user: CentralAppUser): Boolean = {
    !user.firstName.isEmpty && !user.lastName.isEmpty && (user.mobilePhone.isEmpty || user.mobilePhone.get.startsWith("+")) &&
    """([\w\.]+)@([\w\.]+)""".r.unapplySeq(user.email).isDefined
    //&& (user.places.isEmpty || user.places.get.map(Company.isValid).forall(_ == true))
    //&& (user.companies.isEmpty || user.companies.get.map(_.attribution.creatorCentralAppId.getOrElse(0) == user.centralAppId).forall(_ == true))
  }
}