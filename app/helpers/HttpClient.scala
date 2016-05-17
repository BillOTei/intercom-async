package helpers

import akka.actor.ActorRef
import akka.actor.Status.Failure
import models.EventResponse
import play.api.libs.ws.{WS, WSAuthScheme, WSRequest, WSResponse}
import play.api.Play.current
import play.api.libs.json._
import service.actors.ForwardActor.Answer

import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object HttpClient {

  /**
    * Gets http response from url
    *
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

  /**
    * Same as above for get method
    * @param endpoint: intercom endpoint users, companies, events...
    * @param params: the params to query
    * @param sender: the actor ref to send response to, mainly useful in case of failure
    * @return
    */
  def getFromIntercomApi(endpoint: String, sender: ActorRef, params: (String, String)*) = {
    handleIntercomResponse(
      Try(
        getAuthIntercomRequest(endpoint).withQueryString(params: _*).get
      )
    )
  }

  def getAllPagedFromIntercom(endpoint: String, dataType: String, sender: ActorRef, params: (String, String)*) = {

    def go(accData: List[JsObject], page: Int): Future[Try[List[JsObject]]] = {
      val fullParams = params ++ Seq("page" -> page.toString, "per_page" -> "2")

      val data =  getFromIntercomApi(endpoint, sender, fullParams: _*)
      val t = data map {
        case Success(pages) => (pages \ "pages" \ "next").asOpt[String] match {

          case Some(nextUrl) => (pages \ dataType).validate[List[JsObject]] match {

            case JsSuccess(list: List[JsObject], _) => go(accData ++ list, page + 1)

            case e: JsError =>
              //sender ! Failure(new Throwable(e.errors.mkString(";")))
              List.empty
          }

          case _ => accData
        }

        case scala.util.Failure(e) =>
          //sender ! Failure(e)
          List.empty
      }

    }

    go(List.empty, 1)
  }

  /**
    * Gets the auth WS request to send to Intercom
    *
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
  private def handleIntercomResponse(
                                      t: Try[Future[WSResponse]],
                                      optSender: Option[ActorRef] = None
                                      )(implicit needAnswer: Boolean = false): Future[Try[JsValue]] = {
    t match {
      case Success(f) => f.map(
        response => response.status match {
          case 200 | 202 =>
            optSender.foreach(
              _ ! EventResponse(
                status = true,
                s"Intercom resource ${response.statusText + Try(": " + response.json).getOrElse("")}"
              )
            )
            Success(response.json)
          case 404 =>
            optSender.foreach(_ ! EventResponse(status = true, s"Intercom resource not found"))
            Success(JsNull)
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
        optSender.foreach(
          sender => if (!needAnswer) sender ! Failure(e) else sender ! Answer(scala.util.Failure(e))
        )
        Future.failed(new Throwable(e.getMessage))
    }
  }
}