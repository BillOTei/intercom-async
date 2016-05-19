package models.centralapp.contacts

import models.{JsonReadsConstraints, Message}
import models.centralapp.BasicUser
import models.centralapp.places.BasicPlace
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.UserReach
import models.intercom.ConversationInit
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

case class UserContact(
                        userId: Long,
                        token: Option[String],
                        subject: String,
                        message: Option[String],
                        whenToContact: Option[String],
                        businessName: Option[String],
                        location: Option[String]
                      ) extends ContactRequest

object UserContact extends JsonReadsConstraints {
  val MSG_UNAUTHORIZED = "ERR.USER.UNAUTHORIZED"
  val MSG_USER_INVALID = "ERR.USER.INVALID"

  implicit val jsonReads: Reads[UserContact] = (
    (JsPath \ "user_id").read[Long] and
      (JsPath \ "token").readNullable[String] and
      (JsPath \ "subject").read[String](nonEmptyString) and
      (JsPath \ "message").readNullable[String](nonEmptyString) and
      (JsPath \ "when_to_contact").readNullable[String](nonEmptyString) and
      (JsPath \ "business_name").readNullable[String](nonEmptyString) and
      (JsPath \ "location").readNullable[String](nonEmptyString)
    )(UserContact.apply _)

  /**
    * Method that processes a contact request coming from http client
    * @param userContact: The parsed user contact data
    * @param userEmail: the user email retrieved after token verification
    */
  def process(userContact: UserContact, userEmail: String) = {
    val system = Akka.system()
    for {
      placeName <- userContact.businessName
      location <- userContact.location
    } yield {
      // We store the basic place info if available
      system.actorOf(ForwardActor.props) ! Forward(
        Message[BasicPlaceUser](
          "basic-placeuser-creation",
          Json.obj(),
          Some(BasicPlaceUser(
            new BasicPlace {
              override def name: String = placeName
              override def locality: String = location
              override def lead: Boolean = true
            },
            new BasicUser {
              override def email: String = userEmail
            }
          ))
        )
      )
    }

    // If there are contact info we initiate conversation
    if (userContact.whenToContact.isDefined || userContact.message.isDefined) {
      system.actorOf(ForwardActor.props) ! Forward(
        Message[ConversationInit](
          "user-contact",
          Json.obj(),
          Some(ConversationInit(
            userContact.subject +
              userContact.whenToContact.map(" - to be contacted: " + _).getOrElse("") +
              userContact.message.map(" - " + _).getOrElse(""),
            Some(userEmail)
          ))
        )
      )
    }

    // We persist the user attempt to reach us
    system.actorOf(ForwardActor.props) ! Forward(
      Message[UserReach](
        "user-reach",
        Json.obj(),
        Some(
          UserReach(
            userContact.subject,
            new BasicUser {
              override def email: String = userEmail
            }
          )
        )
      )
    )
  }
}