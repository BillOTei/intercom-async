package models.centralapp.contacts

import models.Message
import models.centralapp.places.BasicPlace
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.BasicUser
import models.intercom.ConversationInit
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

case class LeadContact(
                        subject: String,
                        message: Option[String],
                        whenToContact: Option[String],
                        name: String,
                        email: String,
                        phone: String,
                        language: Option[String],
                        businessName: Option[String],
                        location: Option[String]
                      ) extends ContactRequest

object LeadContact {

  implicit val jsonReads: Reads[LeadContact] = (
      (JsPath \ "subject").read[String] and
      (JsPath \ "message").readNullable[String] and
      (JsPath \ "when_to_contact").readNullable[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "email").read[String](Reads.email) and
      (JsPath \ "phone").read[String] and
      (JsPath \ "language").readNullable[String](Reads.minLength[String](2) keepAnd Reads.maxLength[String](2)) and
      (JsPath \ "business_name").readNullable[String] and
      (JsPath \ "location").readNullable[String]
    )(LeadContact.apply _)

  /**
    * Method that processes a contact request coming from http client
    * @param LeadContact: The parsed user contact data
    */
  def process(LeadContact: LeadContact) = {
    val system = Akka.system()
    
  }

  /*def toUserContact(leadContact: LeadContact, intercomJsonUser: JsValue): UserContact = {
    UserContact(

    )
    /*
    * userId: Long,
                        token: Option[String],
                        subject: String,
                        message: Option[String],
                        whenToContact: Option[String],
                        businessName: Option[String],
                        location: Option[String]*/
  }*/
}