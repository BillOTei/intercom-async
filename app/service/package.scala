import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by alexandreteilhet on 12/08/16.
  */
package object service {

  implicit final val akkaAskTimeout = Timeout(10 seconds)

}
