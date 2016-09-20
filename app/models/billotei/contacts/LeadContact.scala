package models.billotei.contacts

import models.billotei.BasicUser
import models.billotei.places.BasicPlace
import models.billotei.relationships.BasicPlaceUser
import models.intercom.ConversationInit
import models.{JsonReadsConstraints, Message}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

case class LeadContact(
                        subject: Option[String],
                        message: Option[String],
                        whenToContact: Option[String],
                        name: Option[String],
                        email: Option[String],
                        phone: Option[String],
                        language: Option[String],
                        businessName: Option[String],
                        location: Option[String]
                      )

object LeadContact extends JsonReadsConstraints {

  val MSG_LANGUAGE_INVALID = "ERR.LANGUAGE_INVALID"

  implicit val jsonReads: Reads[LeadContact] = (
      (JsPath \ "subject").readNullable[String](nonEmptyString) and
      (JsPath \ "message").readNullable[String](nonEmptyString) and
      (JsPath \ "when_to_contact").readNullable[String] and
      (JsPath \ "name").readNullable[String](nonEmptyString) and
      (JsPath \ "email").readNullable[String](Reads.email) and
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
            leadContact.subject.map(_ + " - ").getOrElse("") +
              leadContact.whenToContact.map("to be contacted: " + _ + " - ").getOrElse("") +
              leadContact.message.getOrElse(""),
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
      override def email: String = leadContact.email.orNull
      override def optName: Option[String] = leadContact.name
      override def optLang: Option[String] = leadContact.language
      override def optPhone: Option[String] = leadContact.phone
      override def optIntercomId: Option[String] = optionalIntercomId
    }
  }
}
