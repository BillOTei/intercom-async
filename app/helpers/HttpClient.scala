package helpers

import play.api.libs.ws.{WS, WSResponse}
import play.api.Play.current

import scala.concurrent.Future
import scala.util.Try

object HttpClient {
  def get(url: String, params: (String, String)*): Try[Future[WSResponse]] = {
    Try(WS.url(url).withQueryString(params: _*).withRequestTimeout(5000).get())
  }
}