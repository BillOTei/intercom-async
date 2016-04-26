package models.centralapp.relationships

import models.centralapp.places.BasicPlace
import models.centralapp.users.BasicUser

case class BasicPlaceUser(place: BasicPlace, user: BasicUser)