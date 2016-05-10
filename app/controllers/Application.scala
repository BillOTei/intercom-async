package controllers

import play.api.mvc._
import play.libs.Akka

class Application extends Controller {
  val system = Akka.system()

  /**
    * The service index probe for testing responsiveness
    * @return
    */
  def index = Action {
    Ok("Index Centralapp Events Service")
  }

}
