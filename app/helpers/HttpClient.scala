package helpers

import akka.actor.ActorRef
import akka.actor.Status.Failure
import models.EventResponse
import models.centralapp.{Country, CountryReaders}
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.{WS, WSAuthScheme, WSRequest, WSResponse}
import service.actors.ForwardActor.Answer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

object HttpClient {

  /**
    * Gets http response from url
    *
    * @param url    : the url to get from
    * @param params : the data to send
    * @return
    */
  def get(url: String, params: (String, String)*): Try[Future[WSResponse]] = {
    Try(WS.url(url).withQueryString(params: _*).withRequestTimeout(5000).get())
  }

  /**
    * Post to the intercom api using WS play service
    *
    * @param endpoint : intercom endpoint users, companies, events...
    * @param data     : the json data formatted as they want
    * @param sender   : the actor ref for error handling
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
    *
    * @param endpoint : intercom endpoint users, companies, events...
    * @param params   : the params to query
    * @param sender   : the actor ref to send response to, mainly useful in case of failure
    * @return
    */
  def getFromIntercomApi(endpoint: String, sender: ActorRef, params: (String, String)*): Future[Try[JsValue]] = {
    handleIntercomResponse(
      Try(
        getAuthIntercomRequest(endpoint).withQueryString(params: _*).get
      ),
      Some(sender)
    )
  }

  def getAllPagedFromIntercom(endpoint: String, dataType: String, sender: ActorRef, params: (String, String)*) = {

    def go(accData: List[JsObject], page: Int): Future[Try[List[JsObject]]] = {

      val fullParams = params ++ Seq("page" -> page.toString, "per_page" -> "50")

      getFromIntercomApi(endpoint, sender, fullParams: _*).flatMap {

        _.toOption map {

          jsonRes => {

            if ((jsonRes \ "pages" \ "next").asOpt[String].isDefined) go(
              accData = accData ++ (jsonRes \ dataType).asOpt[List[JsObject]].getOrElse(Nil),
              page = page + 1
            )
            else Future(Success(accData ++ (jsonRes \ dataType).asOpt[List[JsObject]].getOrElse(Nil)))

          }

        } getOrElse Future(Success(accData))

      }

    }

    go(Nil, 1)
  }

  /**
    * Gets the auth WS request to send to Intercom
    *
    * @param endpoint : the endpoint uri
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
    * @param t         : the Try result
    * @param optSender : the optional actor sender ref, used is response not needed mainly
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
            optSender.foreach(_ ! Failure(new Exception("Intercom resource not found")))
            scala.util.Failure(new Exception("Intercom resource not found"))
          case _ =>
            optSender.foreach(_ ! Failure(new Exception(response.json.toString)))
            scala.util.Failure(new Exception(response.json.toString))
        }
      ).recoverWith {
        case e: Exception => Future.failed(new Exception(e.getMessage))
        case t: Throwable => Future.failed(t)
        case _ => Future.failed(new Exception("Intercom request failed for unknown reason"))
      }
      case scala.util.Failure(e) =>
        optSender.foreach(
          sender => if (!needAnswer) sender ! Failure(e) else sender ! Answer(scala.util.Failure(e))
        )
        Future.failed(new Exception(e.getMessage))
    }
  }

  /**
    * get the list of countries currently supported by Atlas service
    *
    * @return a future of the list of countries
    */
  @deprecated
  def getAtlasCountries: Future[Try[List[Country]]] = {
    WS.url(Play.configuration.getString("atlasservice.static.countries.url").get).get().map {
      res => Try {
        res.json.
          validate[List[JsValue]].
          asOpt.
          map {
            _.flatMap {
              _.validate[Country](CountryReaders.atlas).asOpt
            }
          }.getOrElse(Nil)
      }
    }
  }

}
