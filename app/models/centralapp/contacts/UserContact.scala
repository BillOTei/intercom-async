package models.centralapp.contacts

import models.Message
import models.centralapp.{Attribution, SimplePlace}
import models.intercom.ConversationInit
import org.joda.time.DateTime
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
                        whenToContact: Option[Long],
                        businessName: Option[String],
                        city: Option[String]
                      ) extends ContactRequest

object UserContact {
  val MSG_UNAUTHORIZED = "ERR.USER.UNAUTHORIZED"
  val MSG_USER_INVALID = "ERR.USER.INVALID"

  implicit val jsonReads: Reads[UserContact] = (
    (JsPath \ "user_id").read[Long] and
      (JsPath \ "token").readNullable[String] and
      (JsPath \ "subject").read[String] and
      (JsPath \ "message").readNullable[String] and
      (JsPath \ "when_to_contact").readNullable[Long] and
      (JsPath \ "business_name").readNullable[String] and
      (JsPath \ "city").readNullable[String]
    )(UserContact.apply _)

  /**
    * Method that processes a contact request coming from http client
    * @param userContact: The parsed user contact data
    * @param userEmail: the user email retrieved after token verification
    */
  def process(userContact: UserContact, userEmail: String) = {
    val system = Akka.system()
    //userContact.businessName.map(

    //)
    if (userContact.whenToContact.isDefined || userContact.message.isDefined) {
      system.actorOf(ForwardActor.props) ! Forward(
        Message[ConversationInit](
          "user-contact",
          Json.obj(),
          Some(ConversationInit(
            userContact.subject +
              userContact.whenToContact.map(t => " - to be contacted: " + new DateTime(t * 1000).toString).getOrElse("") +
              userContact.message.map(" - " + _).getOrElse(""),
            Some(userEmail)
          ))
        )
      )
    }
  }
}