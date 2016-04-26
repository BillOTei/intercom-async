package models.centralapp.places

import models.centralapp.Attribution

trait BasicPlace {
  def name: String
  def locality: String
  def attribution: Option[Attribution]
}
