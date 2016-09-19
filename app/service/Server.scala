package service

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.spingo.op_rabbit.{RabbitControl, _}
import models.Message
import play.Logger
import play.api.Play._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.libs.Akka
import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server {

  val system = Akka.system()

  /**
    * Create the listener client for the reactive stream system
    *
    * @param address server string attributed address
    * @param port the address port
    */
  def connectStream(address: String, port: Int): Unit = {
    implicit val sys = system
    implicit val materializer = ActorMaterializer()

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      Logger.debug("Event server connected to: " + conn.remoteAddress)
      // Get the ByteString flow and reconstruct the msg for handling and then output it back
      // that is how handleWith work apparently
      conn.handleWith(
        Flow[ByteString].fold(ByteString.empty)((acc, b) => acc ++ b).
          map(b => handleIncomingMessages(system, b.utf8String)).
          map(ByteString(_))
      )
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        Logger.debug("Event server started, listening on: " + b.localAddress)
      case Failure(e) =>
        Logger.debug(s"Event server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }
  }

  /**
    * Handler for the incoming concatened message pieces from a publisher in the stream
    * converts it to an Option[Message] and forwards to the main actor for dispatch
    *
    * @param system the akka actor system reference
    * @param stringMsg the message
    * @return
    */
  def handleIncomingMessages(system: ActorSystem, stringMsg: String): String = {
    Logger.debug("Event server received message: " + stringMsg)
    // Forward the message to the appropriate actor, ask for the response
    Json.parse(stringMsg).validate(Message.messageReads) match {
      case m: JsSuccess[Message[Nothing]] => system.actorOf(ForwardActor.props) ! Forward(m.value)
      case e: JsError => Logger.error(s"Message invalid $stringMsg", new Exception(e.errors.mkString(";")))
    }
    // Output the strmsg for bytestring conversion to respond to the publisher
    // same message sent back means transmission went ok on the publisher side
    stringMsg
  }

  /**
    * RabbitMQ subscription to the events queue
    *
    * @return
    */
  def subscribe: SubscriptionRef = {
    val queueName = current.configuration.getString("op-rabbit.centralapp.events-queue").get

    Logger.debug(s"Starting to listen to queue: $queueName")

    Subscription.run(system.actorOf(Props[RabbitControl])) {
      import Directives._

      // A qos of 3 will cause up to 3 concurrent messages to be processed at any given time.
      channel(qos = 3) {
        consume(queue(queueName)) {

          (body(as[Message[Nothing]]) & routingKey) { (msg, key) =>
            Logger.debug(s"Event server sending to forward actor message: ${msg.event}")

            // 3 more attemps in 1mn in case of failure, then dropped
            implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver(1 minute)

            ack(system.actorOf(Props[ForwardActor]) ? Forward(msg))
          }

        }
      }

    }
  }

}