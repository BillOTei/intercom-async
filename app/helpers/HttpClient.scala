package helpers

import akka.actor.ActorRef
import akka.actor.Status.Failure
import models.Response
import play.api.libs.ws.{WS, WSAuthScheme, WSRequest, WSResponse}
import play.api.Play.current
import play.api.libs.json.{JsObject, JsValue}

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
    handleIntercomResponse(
      Try(
        getAuthIntercomRequest(endpoint).post(data)
      ),
      Some(sender)
    )
  }

  def getFromIntercomApi(endpoint: String, params: (String, String)*) = {
    handleIntercomResponse(
      Try(
        getAuthIntercomRequest(endpoint).withQueryString(params: _*).get
      )
    )
  }

  /**
    * Gets the auth WS request to send to Intercom
    * @param endpoint: the endpoint uri
    * @return
    */
  private def getAuthIntercomRequest(endpoint: String): WSRequest = {
    WS.url(current.configuration.getString("intercom.apiurl").getOrElse("") + s"/$endpoint").
    withAuth(
      current.configuration.getString("intercom.appid").getOrElse(""),
      current.configuration.getString("intercom.apikey").getOrElse(""),
      WSAuthScheme.BASIC
    ).
    withRequestTimeout(5000).
    withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json")
  }

  /**
    * Handler for intercom WS call
    *
    * @param t: the Try result
    * @param optSender: the optional actor sender ref, used is response not needed mainly
    * @return
    */
  private def handleIntercomResponse(t: Try[Future[WSResponse]], optSender: Option[ActorRef] = None): Future[Try[JsValue]] = {
    t match {
      case Success(f) => f.map(
        response => response.status match {
          case 200 | 202 =>
            optSender.foreach(_ ! Response(status = true, s"Intercom resource ${response.statusText}: ${response.json}"))
            Success(response.json)
          case _ =>
            optSender.foreach(_ ! Failure(new Throwable(response.json.toString)))
            scala.util.Failure(new Throwable(response.json.toString))
        }
      ).recoverWith {
        case e: Exception => Future.failed(new Throwable(e.getMessage))
        case t: Throwable => Future.failed(t)
        case _ => Future.failed(new Throwable("Intercom request failed for unknown reason"))
      }
      case scala.util.Failure(e) =>
        optSender.foreach(_ ! Failure(e))
        Future.failed(new Throwable(e.getMessage))
    }
  }
}