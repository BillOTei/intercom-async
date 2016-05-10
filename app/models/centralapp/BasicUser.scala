package models.centralapp

trait BasicUser {
  def email: String
  def optName: Option[String] = None
  def optLang: Option[String] = None
  def optPhone: Option[String] = None
  def optIntercomId: Option[String] = None
}
