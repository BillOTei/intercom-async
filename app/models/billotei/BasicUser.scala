package models.billotei

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait BasicUser {
  def email: String
  def optName: Option[String] = None
  def optLang: Option[String] = None
  def optPhone: Option[String] = None
  def optIntercomId: Option[String] = None
}

object BasicUser {

  case class VeryBasicUser(email: String, centralAppId: Long)
  implicit val veryBasicUserReads: Reads[VeryBasicUser] = (
    (__ \ "email").read[String] and
      (__ \ "id").read[Long]
    )(VeryBasicUser.apply _)

  implicit val veryBasicUserListReads: Reads[List[VeryBasicUser]] = (__ \ "users").lazyRead(Reads.list[VeryBasicUser](veryBasicUserReads))

}
