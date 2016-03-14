import akka.actor.ActorSystem
import play.api.{GlobalSettings, Logger}
import service.Server

/**
 * Created by BillOTei on 17/02/16
 */
object Global extends GlobalSettings {
  override def onStart(app: play.api.Application): Unit = {
    Logger.info("Centralapp Events Service starting")

    Server.connect(ActorSystem("CentralappEvents"), "localhost", 2554)
  }
}
