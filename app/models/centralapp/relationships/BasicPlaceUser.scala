package models.centralapp.relationships

import models.centralapp.BasicUser
import models.centralapp.places.BasicPlace

case class BasicPlaceUser(place: BasicPlace, user: BasicUser)