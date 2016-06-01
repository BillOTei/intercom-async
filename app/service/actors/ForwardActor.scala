package service.actors

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.centralapp.BasicUser
import models.centralapp.BasicUser.VeryBasicUser
import models.centralapp.places.Place
import models.centralapp.relationships.{BasicPlaceUser, PlaceUser}
import models.centralapp.users.{User, UserReach}
import models.intercom.{ConversationInit, IntercomMessage, Tag}
import models.{EventResponse, Message}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsSuccess}
import service.actors.IntercomActor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ForwardActor {
  def props = Props[ForwardActor]

  case class Forward[T](msg: Message[T])

  case class Answer(result: Try[EventResponse])
}

class ForwardActor extends Actor {

  import ForwardActor._

  implicit val timeout = Timeout(10 seconds)

  /**
    * Main forward method to the clients, so far only intercom, more to be added...
    *
    * @return
    */
  def receive = {
    case Forward(msg: Message[_]) =>
      msg.event match {
        // When a user creates a place
        case "placeuser-creation" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] => (msg.payload \ "place").validate(Place.placeReads(msg.payload)) match {
            case p: JsSuccess[Place] => forwardAndAskIntercom(PlaceUserMessage(u.value, p.value), msg.event)

            case e: JsError => Logger.error(s"Place invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }
          case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        // When owner or centralAppAdmin deletes a place, that is what happens: all place user relationships deleted
        case "all-placeusers-deletion" => (msg.payload \ "place" \ "id").validate[Long] match {
          case JsSuccess(placeId: Long, _) => forwardAndAskIntercom(DeleteAllPlaceUsersMessage("", placeId), msg.event)
          case e: JsError => Logger.error(s"Place invalid ${e.errors.mkString(";")}")
        }

        case "placeuser-deletion" =>
          implicit val contextPayload = msg.payload
          msg.payload.validate[PlaceUser] match {
            case placeUser: JsSuccess[PlaceUser] => forwardAndAskIntercom(
              PlaceUserMessage(placeUser.value.user, placeUser.value.place, removeRelationship = true),
              msg.event
            )

            case e: JsError => Logger.error(s"PlaceUser invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }

        // On place update
        case "place-update" => (msg.payload \ "place").validate(Place.placeReads(msg.payload)) match {
          case p: JsSuccess[Place] => forwardAndAskIntercom(PlaceMessage(p.value), msg.event)

          case e: JsError => Logger.error(s"Place invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        // When a user reaches us with place data that we don't already have in CentralApp
        case "basic-placeuser-creation" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(placeUserPayload: BasicPlaceUser) => 
              forwardAndAskIntercom(BasicPlaceUserMessage(placeUserPayload), msg.event)
              
            case _ => Logger.error("BasicPlaceUser payload invalid")
          }

        // On user creation or update
        case "user-creation" | "user-update" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] => forwardAndAskIntercom(
            UserMessage(u.value, msg.event == "user-update"),
            msg.event
          )
            
          case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        // On user login or when he asks for place verification
        case "user-login" | "verification-request" =>
          // See if we need a paypload for this event or if it is possible to use only one case "event"
          (msg.payload \ "user").validate(User.userReads) match {
            case u: JsSuccess[User] =>
              forwardAndAskIntercom(
                EventMessage(
                  msg.event,
                  (msg.payload \ "created_at").asOpt[Long].getOrElse(System.currentTimeMillis / 1000),
                  u.value,
                  (msg.payload \ "place").asOpt(Place.placeReads(msg.payload))
                ),
                msg.event
              )

            case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }

        // On user or lead contact
        case "user-contact" | "lead-contact" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(contactPayload: ConversationInit) =>
              // So far only intercom conversation contact
              forwardAndAskIntercom(ConversationInitMessage(contactPayload), msg.event)
            case _ => Logger.error(s"${msg.event} payload invalid")
          }

        // On lead creation/contact with or without place data
        case "lead-creation" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(placeUserPayload: BasicPlaceUser) =>
              // So far only intercom leads
              forwardAndAskIntercom(LeadMessage(placeUserPayload.user, Some(placeUserPayload)), msg.event)

            case Some(userPayload: BasicUser) => forwardAndAskIntercom(LeadMessage(userPayload), msg.event)

            case _ => Logger.error("lead-creation payload invalid")
          }

        // When a user tried to reach us (with whatever data and mainly through contact form), we store this
        // with that event, due to intercom lead endpoints design, the lead-reach is done under the hood inside intercom actor atm
        case "user-reach" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(userPayload: UserReach) =>
              // So far we only use intercom tagging system for that
              forwardAndAskIntercom(TagMessage(Tag(userPayload.subject, List(userPayload.basicUser))), msg.event)

            case _ => Logger.error("user-reach payload invalid")
          }

        // Intercom dedicated event here that's not the best place for that but it's a single shot event
        // implemented here to avoid messing the ctrl upper layers with forwarding events directly to intercom actor
        case "intercom-users-update" =>
          msg.payload.validate[List[VeryBasicUser]] match {
            case userList: JsSuccess[List[VeryBasicUser]] =>
              Logger.info("Processing users page " + (msg.payload \ "page").asOpt[Int].map(_.toString).getOrElse("unknown"))
              context.actorOf(IntercomActor.props) ! BulkUserIdUpdate(userList.value)

            case e: JsError => Logger.error(s"User list invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }

        // Update multiple place-users at a time
        case "placeusers-update" =>
          implicit val contextPayload = msg.payload
          (msg.payload \ "placeusers").validate[List[PlaceUser]] match {
            case placeUserList: JsSuccess[List[PlaceUser]] =>
              Logger.info("Processing placeusers page " + (msg.payload \ "page").asOpt[Int].map(_.toString).getOrElse("unknown"))
              forwardAndAskIntercom(BulkPlaceUserUpdate(placeUserList.value), msg.event)

            case e: JsError => Logger.error(s"PlaceUser list invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }

        case _ => Logger.warn(s"Service ${msg.event} not implemented yet")
      }

    case Answer(result) => result match {
      case Success(resp) => Logger.info(resp.body)
      case Failure(fail) => Logger.error(fail.getMessage)
    }
  }

  /**
    * Forwards and ask response to intercom in a non blocking way
    * @param message: the intercom message to forward
    * @param eventName: the event name to forward
    * @tparam T: subtype of intercom message
    */
  def forwardAndAskIntercom[T <: IntercomMessage](message: T, eventName: String) = {
    Logger.debug(s"Forwarding $eventName to intercom...")
    (context.actorOf(IntercomActor.props) ? message).mapTo[EventResponse].onComplete {
      case Success(response) => Logger.info(response.body)
      case Failure(err) => Logger.error(
        s"ForwardActor to Intercom did not succeed: ${err.getMessage} for message: ${message.toString}",
        err
      )
    }
  }
}