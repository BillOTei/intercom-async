package service.actors

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.centralapp.BasicUser
import models.centralapp.places.Place
import models.centralapp.relationships.BasicPlaceUser
import models.centralapp.users.{User, UserReach}
import models.intercom.{ConversationInit, Tag}
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
            case p: JsSuccess[Place] =>
              Logger.debug("Forwarding placeuser-creation to intercom...")
              (context.actorOf(IntercomActor.props) ? PlaceUserMessage(u.value, p.value)).mapTo[EventResponse].onComplete {
                case Success(response) => Logger.info(response.body)
                case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
              }
            case e: JsError => Logger.error(s"Place invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }
          case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        case "all-placeusers-deletion" =>
          (msg.payload \ "permission").validate[String] match {
            case s: JsSuccess[String] if Place.CAN_DELETE_REL_TYPES.contains(s.value) =>
            case e: JsError => Logger.error(s"PlaceUsers can only be deleted by centralappAdmin or owner ${e.toString}")
          }

        // On place update
        case "place-update" => (msg.payload \ "place").validate(Place.placeReads(msg.payload)) match {
          case p: JsSuccess[Place] =>
            Logger.debug(s"Forwarding ${msg.event} to intercom...")
            (context.actorOf(IntercomActor.props) ? PlaceMessage(p.value)).mapTo[EventResponse].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
            }
          case e: JsError => Logger.error(s"Place invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        // When a user reaches us with place data that we don't already have in CentralApp
        case "basic-placeuser-creation" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(placeUserPayload: BasicPlaceUser) =>
              Logger.debug(s"Forwarding ${msg.event} to intercom...")
              (context.actorOf(IntercomActor.props) ? BasicPlaceUserMessage(placeUserPayload)).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.info(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }
            case _ => Logger.error("BasicPlaceUser payload invalid")
          }

        // On user creation or update
        case "user-creation" | "user-update" => (msg.payload \ "user").validate(User.userReads) match {
          case u: JsSuccess[User] =>
            Logger.debug(s"Forwarding ${msg.event} to intercom...")
            (context.actorOf(IntercomActor.props) ? UserMessage(u.value)).mapTo[EventResponse].onComplete {
              case Success(response) => Logger.info(response.body)
              case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
            }
          case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
        }

        // On user login or when he asks for place verification
        case "user-login" | "verification-request" =>
          // See if we need a paypload for this event or if it is possible to use only one case "event"
          (msg.payload \ "user").validate(User.userReads) match {
            case u: JsSuccess[User] =>
              Logger.debug(s"Forwarding ${msg.event} event to intercom...")
              (context.actorOf(IntercomActor.props) ? EventMessage(
                msg.event,
                (msg.payload \ "created_at").asOpt[Long].getOrElse(System.currentTimeMillis / 1000),
                u.value,
                (msg.payload \ "place").asOpt(Place.placeReads(msg.payload)))).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.debug(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }

            case e: JsError => Logger.error(s"User invalid ${e.toString}", new Throwable(e.errors.mkString(";")))
          }

        // On user or lead contact
        case "user-contact" | "lead-contact" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(contactPayload: ConversationInit) =>
              // So far only intercom conversation contact
              Logger.debug(s"Forwarding ${msg.event} to intercom...")
              (context.actorOf(IntercomActor.props) ? ConversationInitMessage(contactPayload)).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.debug(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }
            case _ => Logger.error(s"${msg.event} payload invalid")
          }

        // On lead creation/contact with or without place data
        case "lead-creation" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(placeUserPayload: BasicPlaceUser) =>
              // So far only intercom leads
              Logger.debug(s"Forwarding lead-creation with place to intercom...")
              (context.actorOf(IntercomActor.props) ? LeadMessage(placeUserPayload.user, Some(placeUserPayload))).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.info(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }
            case Some(userPayload: BasicUser) =>
              // So far only intercom leads
              Logger.debug(s"Forwarding lead-creation to intercom...")
              (context.actorOf(IntercomActor.props) ? LeadMessage(userPayload)).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.info(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }
            case _ => Logger.error("lead-creation payload invalid")
          }

        // When a user tried to reach us (with whatever data and mainly through contact form), we store this
        // with that event, due to intercom lead endpoints design, the lead-reach is done under the hood inside intercom actor atm
        case "user-reach" =>
          // No json payload atm (internal msg)
          msg.optPayloadObj match {
            case Some(userPayload: UserReach) =>
              // So far we only use intercom tagging system for that
              Logger.debug(s"Forwarding user-reach to intercom...")
              (context.actorOf(IntercomActor.props) ? TagMessage(Tag(userPayload.subject, List(userPayload.basicUser)))).
                mapTo[EventResponse].onComplete {
                  case Success(response) => Logger.info(response.body)
                  case Failure(err) => Logger.error(s"ForwardActor did not succeed: ${err.getMessage}", err)
                }
            case _ => Logger.error("user-reach payload invalid")
          }


        case _ => Logger.warn(s"Service ${msg.event} not implemented yet")
      }

    case Answer(result) => result match {
      case Success(resp) => Logger.info(resp.body)
      case Failure(fail) => Logger.error(fail.getMessage)
    }
  }
}