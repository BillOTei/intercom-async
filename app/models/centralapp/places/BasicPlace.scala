package models.centralapp.places

trait BasicPlace {
  def name: String
  def locality: String
  def lead: Boolean = false
}
