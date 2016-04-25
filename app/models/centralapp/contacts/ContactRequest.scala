package models.centralapp.contacts

abstract class ContactRequest {
  def subject: String
  def message: Option[String]
  def whenToContact: Option[Long]
  def businessName: Option[String]
  def location: Option[String]
}
