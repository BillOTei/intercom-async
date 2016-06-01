package models.intercom

import models.centralapp.BasicUser
import models.centralapp.users.{User => CentralAppUser}
import models.centralapp.relationships.BasicPlaceUser
import play.api.libs.json.{JsObject, JsString, Json}

object Lead {
  /**
    * Gets a basic json to send to intercom. Used when a user contacts from
    * front end
    *
    * @param basicUser: the lead data
    * @return
    */
  def toJson(basicUser: BasicUser, optCompany: Option[BasicPlaceUser]) = Json.obj(
    "email" -> basicUser.email,
    "name" -> JsString(basicUser.optName.getOrElse("")),
    "custom_attributes" -> Json.obj(
      "interface_language" -> JsString(basicUser.optLang.getOrElse("")),
      "phone" -> JsString(basicUser.optPhone.getOrElse(""))
    )
  ) ++ {
    if (optCompany.isDefined) Json.obj("companies" -> Json.arr(Company.basicToJson(optCompany.get)))
    else Json.obj()
  }

  /**
    * Gets the json obj for lead to user conversion
    * @param leadId: the lead user_id
    * @param user: the user data to write to Intercom
    * @return
    */
  def toJsonForConversion(leadId: String, user: CentralAppUser): JsObject = Json.obj(
    "contact" -> Json.obj("user_id" -> leadId),
    "user" -> User.toJson(user, None)
  )
}