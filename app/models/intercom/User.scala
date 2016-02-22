package models.intercom

import io.intercom.api.{User => IntercomUser, CustomAttribute}

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

// Todo Add Telecom validation or better user validation if necessary
case class User(
  firstName: String,
  lastName: String,
  mobilePhone: Option[String],
  email: String,
  uiLang: String = "en",
  browserLang: String = "en",
  attribution: Option[Attribution],
  billingInfo: Option[Billing],
  signupDate: Long = System.currentTimeMillis / 1000,
  lastSeenDate: Long = System.currentTimeMillis / 1000,
  lastContactedDate: Option[Long],
  lastHeardFromDate: Option[Long],
  nbOfPendingPlaces: Int = 0,
  nbOfManagedPlaces: Int = 0,
  nbOfViewablePlaces: Int = 0,
  nbOfOwnedPlaces: Int = 0
)
  extends IntercomUser {
  def isValid: Boolean = {
    !firstName.isEmpty && !lastName.isEmpty && (mobilePhone.isDefined && mobilePhone.get.startsWith("+")) &&
      """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined
  }
}

object User {
  implicit val userReads: Reads[User] = (
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
    (JsPath \ "nbOfOwnedPlaces").read[Int]
  )(User.apply _)

  def getBasicIntercomUser(user: User): IntercomUser = new IntercomUser().setName(user.firstName + " " + user.lastName).
  setEmail(user.email)

  def setIntercomCustomFields = ???
}