package models.centralapp.users

import models.centralapp.{Attribution, BasicUser}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class User(
                 centralAppId: Long,
                 firstName: Option[String],
                 lastName: Option[String],
                 mobilePhone: Option[String],
                 email: String,
                 uiLang: String = "en",
                 enabled: Boolean,
                 //browserLang: Option[String],
                 attribution: Option[Attribution],
                 billingInfo: Option[UserBilling],
                 signupDate: Option[Long],
                 lastSeenDate: Option[Long],
                 lastContactedDate: Option[Long],
                 lastHeardFromDate: Option[Long],
                 nbOfPendingPlaces: Option[Int],
                 nbOfManagedPlaces: Option[Int],
                 nbOfViewablePlaces: Option[Int],
                 nbOfOwnedPlaces: Option[Int]/*,
                 places: Option[List[Place]]*/
               ) extends BasicUser

object User {
  implicit val userReads: Reads[User] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "firstname").readNullable[String] and
    (JsPath \ "lastname").readNullable[String] and
    (JsPath \ "phone" \ "international").readNullable[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "language" \ "code").read[String] and
    (JsPath \ "enabled").read[Boolean] and
    //(JsPath \ "browserLang").readNullable[String] and
    (JsPath \ "attribution").readNullable[Attribution] and
    (JsPath \ "billingInfo").readNullable[UserBilling] and
    (JsPath \ "created").readNullable[Long] and
    (JsPath \ "updated").readNullable[Long] and
    (JsPath \ "lastContactedDate").readNullable[Long] and
    (JsPath \ "lastHeardFromDate").readNullable[Long] and
    (JsPath \ "nbOfPendingPlaces").readNullable[Int] and
    (JsPath \ "nbOfManagedPlaces").readNullable[Int] and
    (JsPath \ "nbOfViewablePlaces").readNullable[Int] and
    (JsPath \ "nbOfOwnedPlaces").readNullable[Int]
      //and (JsPath \ "places").readNullable[List[Place]]
  ) (User.apply _)

  implicit val userListReads: Reads[List[User]] = (__ \ "users").lazyRead(Reads.list[User](userReads))
}