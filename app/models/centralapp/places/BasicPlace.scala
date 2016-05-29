package models.centralapp.places

trait BasicPlace {
  def name: String
  def locality: Option[String]
  def lead: Boolean = false
}
