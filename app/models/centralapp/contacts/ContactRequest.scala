package models.centralapp.contacts

abstract class ContactRequest {
  def subject: String
  def message: String
  def whenToContact: Long
}
