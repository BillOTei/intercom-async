package models

import play.api.data.validation.ValidationError
import play.api.libs.json.Reads

trait JsonReadsConstraints {

  /**
    * Reads a non empty string
    *
    * @return
    */
  def nonEmptyString: Reads[String] = Reads.filter(ValidationError("ERR.FIELD_TYPE.STRING.NONEMPTY"))(_.trim.nonEmpty)

}