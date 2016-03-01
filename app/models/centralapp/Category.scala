package models.centralapp

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.annotation.tailrec

case class Category(
                    `object`: String,
                    path: String,
                    name: String,
                    level: Int,
                    id: Long,
                    parent: Option[Category]
                    )

object Category {
  implicit val categoryOrdering = Ordering.by((c: JsObject) => (c \ "level").asOpt[Int].getOrElse(0))

  implicit val categoryReads: Reads[Category] = (
    (JsPath \ "object").read[String] and
      (JsPath \ "path").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "level").read[Int] and
      (JsPath \ "id").read[Long] and
      (JsPath \ "parent").readNullable[Category]
    )(Category.apply _)

  /**
    * Get the primary category, for some reason, implicit reads[Category] gives a null exception
    * @param categories: the categories js array
    * @return
    */
  def getPrimaryOpt(categories: JsArray): Option[JsObject] = categories.asOpt[List[JsObject]] match {
    case Some(cats) => Some(cats.min)
    case _ => None
  }

  /**
    * Get the last parent of the primary category by recursion
    * @param categories: the categories js array
    * @return
    */
  def getLastPrimaryOpt(categories: JsArray) = getPrimaryOpt(categories).map(recurseLastParent)

  @tailrec
  def recurseLastParent(category: JsObject): JsObject = (category \ "parent").asOpt[JsObject] match {
    case None => category
    case Some(p) => recurseLastParent(p)
  }
}