package models

import play.api.libs.json.JsObject

/**
 * Created by BillOTei on 17/02/16
 */
case class Message(actor: String, payload: JsObject)