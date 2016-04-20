package helpers

import play.api.data.FormError
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.language.postfixOps

/**
  * Created by Bill'O on 19/04/16.
  */
object JsonError {
  /**
    * Return the json errors for the frontend
    * new version formated as specified in the issue https://github.com/centralapp/centralapp-core/issues/20
    *
    * @param errors The FormErrors Seq
    * @return
    */
  def getErrors(errors: Seq[FormError]): JsValue = {
    Json.toJson(ErrorsWrapper("error", errors.map(e => makeError(e, Option(e.key).filter(_.nonEmpty)))))
  }

  /**
    * Outputs centralapp errors from JsResult validation
    * @param errors: the js result error
    * @param fallbackMsg: the message to output in case of empty js validation msg
    * @return
    */
  def jsErrors(errors: JsError, fallbackMsg: String = "ERR.FIELD_TYPE.ANY.INVALID"): JsValue = getErrors(for {
    err <- errors.errors
    msg <- if (err._2.isEmpty) Seq(ValidationError(fallbackMsg)) else err._2
  } yield FormError(err._1.toJsonString, msg.message))

  case class ErrorVal(name: String, value: String)
  implicit val errorValWrites = Json.writes[ErrorVal]
  val errorValNameValueWrites = new Writes[ErrorVal] {
    def writes(error: ErrorVal) = Json.obj(error.name -> error.value)
  }

  case class Error(
                    code: String,
                    `type`: String,
                    field: Option[String] = None,
                    values: Option[List[ErrorVal]] = None
                  )
  implicit val errorWrites: Writes[Error] = new Writes[Error] {
    def writes(error: Error) = {
      Json.obj(
        "code" -> error.code,
        "type" -> error.`type`
      ) ++ {
        if (error.field.isDefined) Json.obj("field" -> error.field) else Json.obj()
      } ++ {
        if (error.values.isDefined) {
          Json.obj(
            "values" -> error.values.get.foldLeft(Json.obj()) {
              case (acc, err) => acc + (err.name.toLowerCase -> JsString(err.value))
            }
          )
        } else Json.obj()
      }
    }
  }

  case class ErrorsWrapper(obj: String, errors: Seq[Error])
  implicit val errorsWrapperWrites: Writes[ErrorsWrapper] = (
    (__ \ "object").write[String] and
      (__ \ "errors").lazyWrite(Writes.traversableWrites[Error](errorWrites))
    )(unlift(ErrorsWrapper.unapply))

  /**
    * Create the Error according to the FormError type (field or global)
    *
    * @param error The FormError
    * @param field The optional field
    * @return
    */
  def makeError(error: FormError, field: Option[String]): Error = field match {
    case None => Error(error.message, "global")
    case Some(_) => makeFieldError(error)

  }

  /**
    * Make a field type error either from a real single field or a custom composed one
    *
    * @param error the FormError
    * @param translate Whether to translate the error message or not (i.e. read from conf file or not)
    * @return
    */
  def makeFieldError(error: FormError, translate: Boolean = true): Error = {
    // Todo maybe add letters here for vars
    val msgVarRegex = """\[[A-Z_]+:([0-9]+)\].?""".r
    // Here the message needs to be translated if we want to get it from the /conf/messages file
    val msg = if (translate) play.api.i18n.Messages(error.message, error.args: _*) else error.message
    msgVarRegex findFirstIn msg match {
      case None => Error(msg, "field", Some(error.key.replace("obj.", "")))
      case Some(_) => Error(
        msgVarRegex replaceAllIn(msg, "") dropRight 1,
        "field",
        Some(error.key.replace("obj.", "")),
        Some(
          // Format the generic message parameters cf: /conf/messages to make it json serializable
          msgVarRegex findAllIn msg
            map (v => {
            v.drop(1).dropRight(1).split(":")
          })
            map (a => ErrorVal(a(0), a(1).replace("]", ""))) toList
        )
      )
    }
  }
}
