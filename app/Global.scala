import play.api.{GlobalSettings, Logger}
import service.Server

/**
 * Created by BillOTei on 17/02/16
 */
object Global extends GlobalSettings {
  // No stream anymore, rabbitmq prefered
  override def onStart(app: play.api.Application): Unit = {
    Logger.info("Events Service starting")

    //Server.connectStream(ActorSystem("CentralappEvents"), "localhost", 2554)
    Server.subscribe
  }
}
