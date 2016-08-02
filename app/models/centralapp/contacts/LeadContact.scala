package models.centralapp.contacts

import models.centralapp.BasicUser
import models.centralapp.places.BasicPlace
import models.centralapp.relationships.BasicPlaceUser
import models.intercom.ConversationInit
import models.{JsonReadsConstraints, Message}
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
                        phone: Option[String],
                        language: Option[String],
                        businessName: Option[String],
                        location: Option[String]
                      ) extends ContactRequest

object LeadContact extends JsonReadsConstraints {

  val MSG_LANGUAGE_INVALID = "ERR.LANGUAGE_INVALID"

  implicit val jsonReads: Reads[LeadContact] = (
      (JsPath \ "subject").read[String](nonEmptyString) and
      (JsPath \ "message").readNullable[String](nonEmptyString) and
      (JsPath \ "when_to_contact").readNullable[String] and
      (JsPath \ "name").read[String](nonEmptyString) and
      (JsPath \ "email").read[String](Reads.email) and
      (JsPath \ "phone").readNullable[String](nonEmptyString) and
      (JsPath \ "language").readNullable[String](Reads.minLength[String](2) keepAnd Reads.maxLength[String](2)) and
      (JsPath \ "business_name").readNullable[String] and
      (JsPath \ "location").readNullable[String]
    )(LeadContact.apply _)

  /**
    * Method that processes a contact request coming from http client
    *
    * @param leadContact: The parsed user contact data
    */
   // Note: ConversationInit is an Intercom related object, if the need of new service providers arises, this has to be moved on Intercom related classes to keep the service providers logic on the ForwardActor side
  def process(leadContact: LeadContact) = {
    val system = Akka.system()

    // Leads are always created, never fetched
    if (leadContact.whenToContact.isDefined || leadContact.message.isDefined) {
      system.actorOf(ForwardActor.props) ! Forward(
        Message[ConversationInit](
          "lead-contact",
          Json.obj(),
          Some(ConversationInit(
            leadContact.subject +
              leadContact.whenToContact.map(" - to be contacted: " + _).getOrElse("") +
              leadContact.message.map(" - " + _).getOrElse(""),
            None,
            None,
            Some(leadContact)
          ))
        )
      )
    } else if (leadContact.businessName.isDefined && leadContact.location.isDefined) {
      system.actorOf(ForwardActor.props) ! Forward(
        Message[BasicPlaceUser](
          "lead-creation",
          Json.obj(),
          Some(BasicPlaceUser(
            new BasicPlace {
              override def name: String = leadContact.businessName.get
              override def locality: Option[String] = leadContact.location
              override def lead: Boolean = true
            },
            getBasicUser(leadContact)
          ))
        )
      )
    } else {
      system.actorOf(ForwardActor.props) ! Forward(
        Message[BasicUser](
          "lead-creation",
          Json.obj(),
          Some(getBasicUser(leadContact))
        )
      )
    }
  }

  /**
    * Gets a basic user trait from lead contact data
    * @param leadContact: the data from the contact
    * @return
    */
  def getBasicUser(leadContact: LeadContact, optionalIntercomId: Option[String] = None): BasicUser = {
    new BasicUser {
      override def email: String = leadContact.email
      override def optName: Option[String] = Some(leadContact.name)
      override def optLang: Option[String] = leadContact.language
      override def optPhone: Option[String] = leadContact.phone
      override def optIntercomId: Option[String] = optionalIntercomId
    }
  }
}