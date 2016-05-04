package models.centralapp.users

trait BasicUser {
  def email: String
  def optName: Option[String] = None
  def optLang: Option[String] = None
  def optPhone: Option[String] = None
}
