package models.billotei.contacts

abstract class ContactRequest {
  def subject: String
  def message: Option[String]
  def whenToContact: Option[String]
  def businessName: Option[String]
  def location: Option[String]
}
