package models.intercom

import io.intercom.api.{User => IntercomUser, CompanyCollection, CustomAttribute}

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try
import scala.collection.JavaConverters._

case class User(
  centralAppId: Long,
  firstName: String,
  lastName: String,
  mobilePhone: Option[String],
  email: String,
  uiLang: String = "en",
  browserLang: String = "en",
  attribution: Option[Attribution],
  billingInfo: Option[Billing],
  signupDate: Long = System.currentTimeMillis,
  lastSeenDate: Long = System.currentTimeMillis,
  lastContactedDate: Option[Long],
  lastHeardFromDate: Option[Long],
  nbOfPendingPlaces: Int = 0,
  nbOfManagedPlaces: Int = 0,
  nbOfViewablePlaces: Int = 0,
  nbOfOwnedPlaces: Int = 0,
  companies: Option[List[Company]]
)

object User {
  implicit val userReads: Reads[User] = (
    (JsPath \ "centralAppId").read[Long] and
    (JsPath \ "firstName").read[String] and
    (JsPath \ "lastName").read[String] and
    (JsPath \ "mobilePhone").readNullable[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "uiLang").read[String] and
    (JsPath \ "browserLang").read[String] and
    (JsPath \ "attribution").readNullable[Attribution] and
    (JsPath \ "billingInfo").readNullable[Billing] and
    (JsPath \ "signupDate").read[Long] and
    (JsPath \ "lastSeenDate").read[Long] and
    (JsPath \ "lastContactedDate").readNullable[Long] and
    (JsPath \ "lastHeardFromDate").readNullable[Long] and
    (JsPath \ "nbOfPendingPlaces").read[Int] and
    (JsPath \ "nbOfManagedPlaces").read[Int] and
    (JsPath \ "nbOfViewablePlaces").read[Int] and
    (JsPath \ "nbOfOwnedPlaces").read[Int] and
    (JsPath \ "companies").readNullable[List[Company]]
  )(User.apply _)

  /**
    * Deal with the java uglyness to perform some basic formatting of the intercom user
    * default timezone is used for timestamp conversion
    *
    * @param user: the user wrapper here
    * @return
    */
  def getBasicIntercomUser(user: User): IntercomUser = new IntercomUser().
    setName(user.firstName + " " + user.lastName).
    setEmail(user.email).
    addCustomAttribute(CustomAttribute.newStringAttribute("mobile_phone", user.mobilePhone.getOrElse(""))).
    addCustomAttribute(CustomAttribute.newStringAttribute("ui_lang", user.uiLang)).
    addCustomAttribute(CustomAttribute.newStringAttribute("browser_lang", user.browserLang)).
    setSignedUpAt(user.signupDate / 1000).
    setLastRequestAt(user.lastSeenDate / 1000).
    //addCustomAttribute(CustomAttribute.newStringAttribute("signup_date", new DateTime(user.signupDate).toString("yyyy-MM-dd"))).
    //addCustomAttribute(CustomAttribute.newStringAttribute("last_seen_date", new DateTime(user.lastSeenDate).toString("yyyy-MM-dd"))).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_pending_places", user.nbOfPendingPlaces)).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_managed_places", user.nbOfManagedPlaces)).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_viewable_places", user.nbOfViewablePlaces)).
    addCustomAttribute(CustomAttribute.newIntegerAttribute("nb_of_owned_places", user.nbOfOwnedPlaces)).
    addCustomAttribute(CustomAttribute.newLongAttribute("centralapp_id", user.centralAppId)).
    setCompanyCollection(new CompanyCollection(user.companies.getOrElse(List.empty).map(Company.getBasicIntercomCompany).asJava))

  /**
    * Try to create a basic user on intercom's side
 *
    * @param user: the user wrapper here
    * @return
    */
  def createBasicIntercomUser(user: User): Try[IntercomUser] = Try(IntercomUser.create(getBasicIntercomUser(user)))

  /**
    * Valid the user data
    * add Telecom validation or better user validation if necessary
    * not the best email regex but just a sanity check
 *
    * @param user: the user wrapper
    * @return
    */
  def isValid(user: User): Boolean = {
    !user.firstName.isEmpty && !user.lastName.isEmpty && (user.mobilePhone.isEmpty || user.mobilePhone.get.startsWith("+")) &&
    """([\w\.]+)@([\w\.]+)""".r.unapplySeq(user.email).isDefined &&
    (user.companies.isEmpty || user.companies.get.map(Company.isValid).forall(_ == true)) &&
    (user.companies.isEmpty || user.companies.get.map(_.attribution.creatorCentralAppId.getOrElse(0) == user.centralAppId).forall(_ == true))
  }
}