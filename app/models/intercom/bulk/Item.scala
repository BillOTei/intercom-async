package models.intercom.bulk

import play.api.libs.json.{JsObject, JsValue, Json, Writes}

/**
  * An Item belongs to a Bulk class
  * @param method: either post or delete from intercom api
  * @param dataType: user or event atm
  * @param data: the custom data to update on the user or event
  */
case class Item(
               method: String,
               dataType: String,
               data: JsObject
               )

object Item {

  implicit val jsonWrites: Writes[Item] = new Writes[Item] {
    override def writes(i: Item): JsValue = Json.obj(
      "method" -> i.method,
      "data_type" -> i.dataType,
      "data" -> i.data
    )
  }

}