package models.intercom

case class Billing(
  companyName: String,
  VAT: Option[String],
  companyAddress: String,
  companyCountry: String,
  paymentMean: String,
  activeSubscriptionsNb: Int
)