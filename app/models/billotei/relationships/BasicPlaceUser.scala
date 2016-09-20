package models.billotei.relationships

import models.billotei.BasicUser
import models.billotei.places.BasicPlace

case class BasicPlaceUser(place: BasicPlace, user: BasicUser)
