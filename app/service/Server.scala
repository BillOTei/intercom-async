package service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.pattern.ask
import akka.util.Timeout

import models.{Response, Message}

import play.Logger
import play.api.libs.json.Json

import service.actors.ForwardActor
import service.actors.ForwardActor.Forward

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Server {
  def connect(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val forwardActor = system.actorOf(ForwardActor.props)

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      Logger.info("Event server connected to: " + conn.remoteAddress)
      // Get the ByteString flow and reconstruct the msg for handling and then output it back
      // that is how handleWith work apparently
      conn.handleWith(
        Flow[ByteString].fold(ByteString.empty)((acc, b) => acc ++ b).map(
          b => {
            implicit val timeout = Timeout(5 seconds)
            val stringMsg = b.utf8String
            Logger.info("Event server received message: " + stringMsg)
            // Forward the message to the appropriate actor
            Message.asOption(Json.parse(stringMsg)) match {
              case Some(m) =>
                (forwardActor ? Forward(m)).mapTo[Response].onComplete {
                  case Success(response) => Logger.info(response.body)
                  case Failure(err) =>
                }
              case None => Logger.error(s"Message invalid $stringMsg")
            }
            // Output the strmsg for bytestring conversion to respond to the publisher
            stringMsg
          }
        ).map(ByteString(_))
      )
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        Logger.info("Event server started, listening on: " + b.localAddress)
      case Failure(e) =>
        Logger.info(s"Event server could not bind to $address:$port: ${e.getMessage}")
        system.shutdown()
    }
  }
}