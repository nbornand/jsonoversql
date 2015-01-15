package utils.jsonoversql


import scala.reflect.runtime.universe._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * Hold all schema built, and provide them on demand
 */
case class Schemas(entityKeys:List[String], entityValues:List[PreparedSQL], criteriaList:List[CriteriaServer]){
  private val schemas = entityKeys.zip(entityValues).toMap
  private val criterias = criteriaList.map(pair => (pair.id, pair.sql)).toMap

  def forServer[T:TypeTag] = {
    val typeTag = typeOf[T]
    schemas.get(typeTag.toString).getOrElse(throw new IllegalArgumentException(s"No schema for $typeTag"))
}

  def materializeFilter(url:String) : String = {
    url.split("/").toList match {
      case entity :: "filter" :: rest => rest.map { frag =>
        if (frag.forall(_.isDigit)) criterias (frag.toInt)
        else if(frag.toLowerCase == "and" || frag.toLowerCase == "or") frag.toUpperCase
        else throw new IllegalArgumentException("Filter part of the Url malformed, expected integer or or/and keyword")
      }.mkString(" ")
      case _ => throw new IllegalArgumentException("Url provided is not of the form entity/filter/???")
    }
  }

}

object Schemas{

  def forClient[T](schemas:Schemas): RequestSeed[T] = macro implForClient[T]

  def implForClient[T](c: Context)(schemas:c.Expr[Schemas])(implicit weakType: c.WeakTypeTag[T]): c.Expr[RequestSeed[T]] = {
    import c.universe._

    val tpe = weakType.tpe
    c.Expr[RequestSeed[T]](
      q"""
         new RequestSeed[$tpe]{
            def all = BasicRequest[$tpe]("", ${tpe.toString.toLowerCase + "s"}, Nil)
         }
       """)
  }
}