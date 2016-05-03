package models.intercom

import io.intercom.api.{CompanyCollection, CustomAttribute, User => IntercomUser}
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User => CentralAppUser}
import org.joda.time.DateTime
import play.api.libs.json.{JsString, Json}

import scala.collection.JavaConverters._
import scala.util.Try

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
    addCustomAttribute(CustomAttribute.newStringAttribute("interface_language", user.uiLang)).
    //addCustomAttribute(CustomAttribute.newStringAttribute("browser_language", user.browserLang.getOrElse(""))).
    //setSignedUpAt(user.signupDate / 1000).
    //setLastRequestAt(user.lastSeenDate / 1000).
    //addCustomAttribute(CustomAttribute.newStringAttribute("signup_date_db", new DateTime(user.signupDate).toString("yyyy-MM-dd"))).
    addCustomAttribute(CustomAttribute.newStringAttribute("last_seen_date_db", new DateTime(user.lastSeenDate).toString("yyyy-MM-dd"))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_pending_places", user.nbOfPendingPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_managed_places", user.nbOfManagedPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_viewable_places", user.nbOfViewablePlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_owned_places", user.nbOfOwnedPlaces.getOrElse(0))).
    addCustomAttribute(CustomAttribute.newLongAttribute("centralapp_id", user.centralAppId)).
    addCustomAttribute(CustomAttribute.newBooleanAttribute("confirmed", user.enabled)).
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

  /**
    * The user to json for ws post service mainly
    *
    * @param user: the user reference obj
    * @param company: the optional company i.e. place on centralapp to be added to intercom
    * @return
    */
  def toJson(user: CentralAppUser, company: Option[Place]) = Json.obj(
    "name" -> (user.firstName + " " + user.lastName),
    "email" -> user.email,
    "custom_attributes" -> Json.obj(
      "phone" -> JsString(user.mobilePhone.getOrElse("")),
      "interface_language" -> user.uiLang,
      //"browser_language" -> JsString(user.browserLang.getOrElse("")),
      "last_seen_date_db" -> new DateTime(user.lastSeenDate).getMillis / 1000,
      "nb_of_pending_places" -> Json.toJson(user.nbOfPendingPlaces.getOrElse(0)),
      "nb_of_managed_places" -> Json.toJson(user.nbOfManagedPlaces.getOrElse(0)),
      "nb_of_viewable_places" -> Json.toJson(user.nbOfViewablePlaces.getOrElse(0)),
      "nb_of_owned_places" -> Json.toJson(user.nbOfOwnedPlaces.getOrElse(0)),
      "centralapp_id" -> user.centralAppId,
      "confirmed" -> user.enabled
    )
  ) ++ {
    if (company.isDefined) Json.obj("companies" -> Json.arr(Company.toJson(company.get)))
    else Json.obj()
  } ++ {
    if (user.signupDate.isDefined) Json.obj("signed_up_at" -> user.signupDate.get / 1000)
    else Json.obj()
  }

  /**
    * Gets a basic json to send to intercom. Used when a user contacts from
    * front end
    * @param basicPlaceUser: the data
    * @return
    */
  def basicToJson(basicPlaceUser: BasicPlaceUser) = Json.obj(
    "email" -> basicPlaceUser.user.email,
    "companies" -> Json.arr(Company.basicToJson(basicPlaceUser))
  ) ++ {
    if (basicPlaceUser.user.optName.isDefined) Json.obj("name" -> basicPlaceUser.user.optName.get)
    else Json.obj()
  } ++ {
    if (basicPlaceUser.user.optLang.isDefined) Json.obj(
      "custom_attributes" -> Json.obj(
        "interface_language" -> basicPlaceUser.user.optLang.get,
        "phone" -> JsString(basicPlaceUser.user.optPhone.getOrElse(""))
      )
    )
    else Json.obj()
  }
}