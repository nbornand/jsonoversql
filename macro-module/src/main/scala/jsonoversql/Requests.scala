package utils.jsonoversql

import scala.reflect.runtime.universe._
import scala.language.experimental.macros

object Request{
  def all[T](implicit t:WeakTypeTag[T]) = {
    val table = t.tpe.toString.toLowerCase+"s"
    Entity.fromType(t.tpe)
    BasicRequest[T]("", table, Nil)
  }
}
trait RequestSeed[T]{
  def all:BasicRequest[T]
}
trait Request[T]{
  def url:String
  def entityName:String
  def dataToSend:List[Any]
}
case class BasicRequest[T](url: String, entityName:String, dataToSend:List[Any]) extends Request[T]{
  def where(p:CriteriaClient[T]) = WhereRequest[T](url+p.id, entityName, dataToSend ::: p.toSend())
}
case class WhereRequest[T](url: String, entityName:String, dataToSend:List[Any]) extends Request[T]{
  def and(p:CriteriaClient[T]) = WhereRequest[T](s"${url}/and/${p.id}", entityName, dataToSend ::: p.toSend())
  def or(p:CriteriaClient[T]) = WhereRequest[T](s"${url}/or/${p.id}", entityName, dataToSend ::: p.toSend())
  def fullUrl: String = s"$entityName/filter/"+url
}