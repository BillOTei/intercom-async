package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class User(
                 centralAppId: Long,
                 firstName: String,
                 lastName: String,
                 mobilePhone: Option[String],
                 email: String,
                 uiLang: String = "en",
                 browserLang: Option[String],
                 attribution: Option[Attribution],
                 billingInfo: Option[UserBilling],
                 signupDate: Long = System.currentTimeMillis,
                 lastSeenDate: Long = System.currentTimeMillis,
                 lastContactedDate: Option[Long],
                 lastHeardFromDate: Option[Long],
                 nbOfPendingPlaces: Option[Int],
                 nbOfManagedPlaces: Option[Int],
                 nbOfViewablePlaces: Option[Int],
                 nbOfOwnedPlaces: Option[Int]/*,
                 places: Option[List[Place]]*/
               )

object User {
  implicit val userReads: Reads[User] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "firstname").read[String] and
    (JsPath \ "lastname").read[String] and
    (JsPath \ "phone").readNullable[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "language" \ "code").read[String] and
    (JsPath \ "browserLang").readNullable[String] and
    (JsPath \ "attribution").readNullable[Attribution] and
    (JsPath \ "billingInfo").readNullable[UserBilling] and
    (JsPath \ "created").read[Long] and
    (JsPath \ "updated").read[Long] and
    (JsPath \ "lastContactedDate").readNullable[Long] and
    (JsPath \ "lastHeardFromDate").readNullable[Long] and
    (JsPath \ "nbOfPendingPlaces").readNullable[Int] and
    (JsPath \ "nbOfManagedPlaces").readNullable[Int] and
    (JsPath \ "nbOfViewablePlaces").readNullable[Int] and
    (JsPath \ "nbOfOwnedPlaces").readNullable[Int]
      //and (JsPath \ "places").readNullable[List[Place]]
  ) (User.apply _)
}