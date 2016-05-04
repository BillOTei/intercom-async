import play.api.{GlobalSettings, Logger}

/**
 * Created by BillOTei on 17/02/16
 */
object Global extends GlobalSettings {
  // No stream anymore, http prefered
  /*override def onStart(app: play.api.Application): Unit = {
    Logger.info("Centralapp Events Service starting")

    Server.connectStream(ActorSystem("CentralappEvents"), "localhost", 2554)
  }*/
}
