package models.billotei

import helpers.HttpClient
import play.api.{Application, Play}
import play.api.cache.Cache
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


/**
  * country address component
  *
  * @param name long name of the country
  * @param shortName short name of the country, must also be the ISO code of the country
  */
case class Country(name: String,
                   shortName: String)

object Country {

  val objectName = "country"
  val MSG_UNSUPPORTED = "ERR.COUNTRY.UNSUPPORTED"

  /**
    * create a cache key from the provider and a unique identifier of the country
    * @param provider provider name
    * @param id unique ID of the country
    * @return the generated Cache key
    */
  def cacheKeyFromId(provider: String, id: String) =
    s"centralapp-events:$objectName:$provider:${id.toUpperCase}"

  /**
    * generate a cache key from a country instance
    *
    * @param provider provider from which the country comes from
    * @param country the country instance
    * @return the cache key of the country
    */
  def cacheKey(provider: String, country: Country) =
    cacheKeyFromId(provider, country.shortName.toUpperCase)

  /**
    * cache a country instance
    *
    * @param provider the provider of the country
    * @param country the instance to cache
    * @param app implicit application in context
    */
  def cache(provider: String, country: Country)(implicit app: Application) = {
    Cache.set(cacheKey(provider, country), country)
  }

  /**
    * get a country from the cache
    * @param provider provider of the country
    * @param code the country code
    * @param app implicit app
    * @return optional country, if found
    */
  def getFromCacheByCode(provider: String, code: String)(implicit app: Application): Option[Country] = {
    Cache.getAs[Country](cacheKeyFromId(provider, code))
  }

  val MSG_NO_LATLON = "ERR.LOCATION.LATLON_NOT_FOUND"
  val MSG_NO_RESOLVE = "ERR.LOCATION.NO_RESOLUTION"

  /**
    * get atlas countries
    * @param ec implicit execution context
    * @return an optional list of countries, depending on the success
    */
  @deprecated("no business check for available countries", "09/16")
  def atlasCountries()(implicit ec: ExecutionContext, app: Application): Future[Option[List[Country]]] = {
    HttpClient.getAtlasCountries map {
      case Success(countries) =>
        countries.foreach(cache(Play.configuration.getString("atlasservice.name").get, _))
        Some(countries)

      case _ => None
    }
  }

  /**
    * get country from the atlas service by code
    * @param code the country code
    * @param ec the implicit execution context
    * @return the future of an optional country.
    */
  @deprecated("no business check for available countries", "09/16")
  def getAtlasCountry(code: String)(implicit ec: ExecutionContext, app: Application): Future[Option[Country]] = {
    // attempt to fetch from cache first
    getFromCacheByCode(Play.configuration.getString("atlasservice.name").get, code) match {
      case Some(found) => Future(Some(found))
      case None =>
        atlasCountries() map {
          case Some(countries) => countries.find(_.shortName.contains(code.toUpperCase))
          case _ => None
        }

    }
  }

}

object CountryReaders {

  import play.api.libs.functional.syntax._

  val atlas: Reads[Country] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "code").read[String]
    )((n, c) => Country(n, c))

}
