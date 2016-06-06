package models.intercom

import io.intercom.api.{CompanyCollection, CustomAttribute, User => IntercomUser}
import models.centralapp.BasicUser.VeryBasicUser
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User => CentralAppUser}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.JavaConverters._
import scala.util.Try

case class User(id: String, email: String, optUserId: Option[String])

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
    setName(user.firstName.getOrElse("not_specified") + " " + user.lastName.getOrElse("not_specified")).
    setEmail(user.email).
    addCustomAttribute(CustomAttribute.newStringAttribute("phone", user.mobilePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("interface_language", user.uiLang)).
    //addCustomAttribute(CustomAttribute.newStringAttribute("browser_language", user.browserLang.getOrElse(""))).
    //setSignedUpAt(user.signupDate / 1000).
    //setLastRequestAt(user.lastSeenDate / 1000).
    //addCustomAttribute(CustomAttribute.newStringAttribute("signup_date_db", new DateTime(user.signupDate).toString("yyyy-MM-dd"))).
    //addCustomAttribute(CustomAttribute.newStringAttribute("last_seen_date_db", new DateTime(user.lastSeenDate).toString("yyyy-MM-dd"))).
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
    (user.mobilePhone.isEmpty || user.mobilePhone.get.startsWith("+")) &&
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r.unapplySeq(user.email).isDefined
    //&& (user.places.isEmpty || user.places.get.map(Company.isValid).forall(_ == true))
    //&& (user.companies.isEmpty || user.companies.get.map(_.attribution.creatorCentralAppId.getOrElse(0) == user.centralAppId).forall(_ == true))
  }

  /**
    * The user to json for ws post service mainly,
    * company is embended for update and creation
    *
    * @param user: the user reference obj
    * @param company: the optional company i.e. place on centralapp to be added to intercom
    * @param removeRelationship: the boolean to remove a company from user on intercom
    * @return
    */
  def toJson(user: CentralAppUser, company: Option[Place], removeRelationship: Boolean = false) = Json.obj(
    "name" -> {
      (user.firstName, user.lastName) match {
        case (Some(firstname), Some(lastname)) => user.firstName.get + " " + user.lastName.get
        case (None, Some(lastname)) => user.lastName.get
        case (Some(firstname), None) => user.firstName.get
        case _ => JsNull
      }
    },
    "user_id" -> user.centralAppId.toString,
    "email" -> user.email,
    "custom_attributes" -> {
      Json.obj(
        "phone" -> JsString(user.mobilePhone.getOrElse("")),
        "interface_language" -> user.uiLang,
        /*"browser_language" -> JsString(user.browserLang.getOrElse("")),
        "nb_of_pending_places" -> Json.toJson(user.nbOfPendingPlaces.getOrElse(0)),
        "nb_of_managed_places" -> Json.toJson(user.nbOfManagedPlaces.getOrElse(0)),
        "nb_of_viewable_places" -> Json.toJson(user.nbOfViewablePlaces.getOrElse(0)),
        "nb_of_owned_places" -> Json.toJson(user.nbOfOwnedPlaces.getOrElse(0)),
        "centralapp_id" -> user.centralAppId,*/
        "confirmed" -> user.enabled
      ) ++ {
        if (user.lastSeenDate.isDefined) Json.obj("last_seen_date_db" -> user.lastSeenDate.get / 1000)
        else Json.obj()
      }
    }
  ) ++ {
    if (company.isDefined) Json.obj("companies" -> Json.arr(Company.toJson(company.get, removeRelationship)))
    else Json.obj()
  } ++ {
    if (user.signupDate.isDefined) Json.obj("signed_up_at" -> user.signupDate.get / 1000)
    else Json.obj()
  }

  /**
    * Gets a basic json to send to intercom. Used when a user contacts from
    * front end
    *
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

  implicit val jsonReads: Reads[User] = (
    (__ \ "id").read[String] and
      (__ \ "email").read[String] and
      (__ \ "user_id").readNullable[String]
    )(User.apply _)

  implicit val jsonListReads: Reads[List[User]] = __.lazyRead(Reads.list[User](jsonReads))

  /**
    * Gets a list of users with the right user_id instead of email or nothing
    *
    * @param userList: the list of Intercom users
    * @param centralAppUserList: the list of central app users
    * @return
    */
  def sanitizeUserIdFromList(userList: List[User], centralAppUserList: List[VeryBasicUser]): List[User] = {
    userList filter(
      u => {
        /*u.optUserId.isDefined && u.optUserId.get.contains("@") &&*/
        centralAppUserList.exists(
          cappUser => {
            cappUser.email.toLowerCase.trim == u.email.toLowerCase.trim || cappUser.email.toLowerCase.trim == u.optUserId.getOrElse("").toLowerCase.trim
          }
        )
      }
      ) flatMap {
      user => centralAppUserList.
        find(cappUser => cappUser.email.toLowerCase.trim == user.email.toLowerCase.trim || cappUser.email.toLowerCase.trim == user.optUserId.getOrElse("").toLowerCase.trim).
        map(u => user.copy(optUserId = Some(u.centralAppId.toString)))
    }
  }
}