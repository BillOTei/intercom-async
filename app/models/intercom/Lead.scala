package models.intercom

import models.centralapp.BasicUser
import models.centralapp.relationships.BasicPlaceUser
import play.api.libs.json.{JsString, Json}

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
}