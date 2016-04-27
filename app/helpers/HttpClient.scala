package helpers

import akka.actor.ActorRef
import akka.actor.Status.Failure
import models.Response
import play.api.libs.ws.{WS, WSAuthScheme, WSResponse}
import play.api.Play.current
import play.api.libs.json.JsObject

import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object HttpClient {

  /**
    * Gets http response from url
    * @param url: the url to get from
    * @param params: the data to send
    * @return
    */
  def get(url: String, params: (String, String)*): Try[Future[WSResponse]] = {
    Try(WS.url(url).withQueryString(params: _*).withRequestTimeout(5000).get())
  }

  /**
    * Post to the intercom api using WS play service
    *
    * @param endpoint: intercom endpoint users, companies, events...
    * @param data: the json data formatted as they want
    * @param sender: the actor ref for error handling
    * @return
    */
  def postDataToIntercomApi(endpoint: String, data: JsObject, sender: ActorRef) = {
    handleAsyncIntercomResponse(
      Try(
        WS.url(current.configuration.getString("intercom.apiurl").getOrElse("") + s"/$endpoint").
          withAuth(
            current.configuration.getString("intercom.appid").getOrElse(""),
            current.configuration.getString("intercom.apikey").getOrElse(""),
            WSAuthScheme.BASIC
          ).
          withRequestTimeout(5000).
          withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json").
          post(data)
      ),
      sender
    )
  }

  /**
    * Handler for intercom WS call
    *
    * @param t: the Try result
    * @param sender: the actor sender ref
    * @return
    */
  private def handleAsyncIntercomResponse(t: Try[Future[WSResponse]], sender: ActorRef) = t match {
    case Success(f) => f.map(
      response => {
        if (response.status == 200) sender ! Response(status = true, s"Intercom resource upserted: ${response.json}")
        else if (response.status == 202) sender ! Response(status = true, s"Intercom resource accepted")
        else sender ! Failure(new Throwable(response.json.toString))
      }
    ).recoverWith {
      case e: Exception => Future.failed(new Throwable(e.getMessage))
      case t: Throwable => Future.failed(t)
      case _ => Future.failed(new Throwable("Intercom request failed for unknown reason"))
    }
    case scala.util.Failure(e) => sender ! Failure(e)
  }
}