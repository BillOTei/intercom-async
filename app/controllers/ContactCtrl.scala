package controllers

import helpers.{HttpClient, JsonError}
import models.centralapp.contacts.UserContact
import play.api.Play._
import play.api.mvc._
import play.libs.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ContactCtrl extends Controller {
  val system = Akka.system()

  /**
    * The contact endpoint for authenticated users
    *
    * @return
    */
  // Todo add checks about leads/users that are already
  /*
  Donc scénario 1 :
  Quand un mec s’inscrit —> Si jamais c’était déjà un lead ave qui tu as conversé , etc. Et il arrive en new user out of no-where baaaaahhh tu vas lui envoyer des message discordant —> Donc il faut matcher le lead correspondant

  alext [12:57 PM]
  cad dire des messages discordants ?

  [12:57]
  je capte pas

  vanderlindenjc [12:57 PM]
  Scénario 2 (moins important)
  Quand un lead nous contacte et qu’enfet c’est déjà un utilisateur existant —> Il faudrait ajouter sa demande à son historique pour qu’on puisse bien comprendre que c’est pas un mec nouveau out of no-where

  [12:57]
  Dans intercom tu créer des centaines de messages automatiques, etc.

  [12:58]
  et puis le support regarde les profils et appel ou envoie des emails sur base du profil

  [12:58]
  Donc imagine je suis un lead et je demande des infos sur un programme spécifique —> Je discute à travers le chat intercom avec le stagiaires business dev.

  [12:58]
  Je reçois un deal de sa part….

  [12:58]
  2 jours plustard je décide de m'inscrire

  [12:59]
  et la j’arrive dans un intercom comme un nouveau signup. Le mec va recevoir des notif et emails correspondant à un nouvea user + Max qui va l’apeller.

  [12:59]
  Donc en bref si Max ne sait pas qu’on a déjà été en contact (car il ne va pas chercher pour tout les nouveau signup)

  [1:00]
  il aura l’air con… puis il va devoir retrouver les convers, etc.
  */
  def userContact = Action.async(parse.json) {
    implicit request =>
      request.body.validate[UserContact].map {
        case uc: UserContact =>
          HttpClient.get(
            current.configuration.getString("coreservice.user.url").getOrElse(""),
            "token" -> uc.token.orElse(request.getQueryString("token")).getOrElse("")
          ) match {
            case Success(f) => f.map(
              response => {
                for {
                  userId <- (response.json \ "id").asOpt[Long]
                  id <- Option(userId == uc.userId).filter(identity)
                  userEmail <- (response.json \ "email").asOpt[String]
                } yield userEmail
              } match {
                case Some(userEmail) =>
                  UserContact.process(uc, userEmail)
                  Accepted
                case _ => BadRequest(JsonError.stringError(UserContact.MSG_USER_INVALID))
              }
            ).recoverWith {
              case _ => Future(InternalServerError) // Content useless here as this case recovers to Failure case below
            }

            case Failure(e) => Future(InternalServerError(JsonError.stringError(UserContact.MSG_UNAUTHORIZED)))
          }
      }.recoverTotal {
        e => Future(BadRequest(JsonError.jsErrors(e)))
      }
  }
}
