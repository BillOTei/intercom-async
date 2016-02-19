import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import play.Logger

import scala.util.{Failure, Success}

object Server {
  def connect(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      Logger.info("Event server connected to: " + conn.remoteAddress)
      // Get the ByteString flow and reconstruct the msg for handling and then output it back
      // that is how handleWith work apparently
      conn.handleWith(
        Flow[ByteString].fold(ByteString.empty)((acc, b) => acc ++ b).map(
          b => {
            Logger.info("Event server received message: " + b.utf8String)
            b.utf8String
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