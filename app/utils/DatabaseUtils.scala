package utils.jsonoversql

import scala.collection.mutable
import scala.reflect.macros._
import scala.reflect.runtime.universe._
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import play.api.libs.json._

object DatabaseUtils{

  private val m = Map[String, Option[Int] => String](
    "String" -> (i => s"varchar(${i.getOrElse(255)})"),
    "scala.Int" -> (i => s"int(${i.getOrElse(11)})"),
    "Int" -> (i => s"int(${i.getOrElse(11)})")
  )

  /*//val temp = s.map{ case ((name:String, f:(String, Option[Int] => String))) => q"$name -> $f"}
  val a = q""""""
  //val m = s.map{ case ((name:String, f:(String, Option[Int] => String))) => q"$name -> $f".asInstanceOf[(String, Option[Int] => String)] }.toMap
  def wrapper(x: Int) = { x }
  def apply(x: Int): Int = macro impl
  def impl(c: Context)(x: Tree) = {
    q"wrapper($x)"
  }*/


  def nameOf(t:Type):String = t.typeSymbol.name.toString.toLowerCase

  def createTableQueries(entity:Entity): String = {

    val columns = "`id` int(11) NOT NULL auto_increment" :: "PRIMARY KEY (`id`)" ::
      entity.localFields.map( field => {
        val sqlType = m.get(field.tpe.toString).getOrElse(throw new Exception(s"Undefined mapping for type ${field.tpe}"))(None)
        s"`${field.name}` $sqlType ${field.referTo.map(foreign => s" REFERENCES ${nameOf(foreign)}s(id)").mkString}"
    })

    val sql = s"CREATE TABLE IF NOT EXISTS `${nameOf(entity.tpe)}s`(${columns.mkString(",")})ENGINE=INNODB;"
    sql
  }

  def selectQuery(target:Entity, allEntities:Map[Type,Entity]):String = {
    val entitiesToJoin = mutable.Set[Type]();
    val jsonConcatenation = jsonForEntity(target, allEntities, 1, entitiesToJoin)
    val groupBy = entitiesToJoin.map(
      foreign => s"LEFT JOIN ${nameOf(foreign)}s ON ${nameOf(target.tpe)}s.id=${nameOf(foreign)}s.${nameOf(target.tpe)}_id"
    ).mkString(" ")
    s"SELECT $jsonConcatenation as json FROM ${nameOf(target.tpe)}s $groupBy"
  }

  def updateQuery(target:Entity, json:JsValue) : String = {

    val sets = target.localFields.map(f => s"${f.name}='${(json \ f.name).toString}'")
    s"UPDATE ${nameOf(target.tpe)}s SET $sets WHERE id=2";
  }

  def insertQuery(target:Entity, json:JsValue) : String = {
    val fields = target.localFields.map(f => f.name)
    val values = target.localFields.map(f => (json \ f.name).toString)
    s"INSERT INTO ${nameOf(target.tpe)}s (${fields.mkString(",")}) VALUES (${values.mkString(",")})"
  }

  private def jsonForEntity(target:Entity, allEntities:Map[Type,Entity], depth:Int, groupBy:mutable.Set[Type]):String = {
    val components = target.localFields.map {
      field => "'\"" + field.name + "\":\'," + nameOf(target.tpe) + s"s.${field.name}"
    } ::: {
      if(depth>0) {
        target.joins.map({
          case OneTo(name, from, to) => {
            val otherEntity = allEntities(to)
            groupBy += to
            "'\""+name+"\":\'," + jsonForEntity(otherEntity, allEntities, depth-1, groupBy)
          }
          case ManyTo(tpe, from, to) => "''"
        })
      } else Nil
    }

    "CONCAT_WS('','{\"id\":',"+nameOf(target.tpe)+"s.id,',',"+components.mkString(",',',")+",'}')"
  }


}


object Macro {
  //implicit val liftEntity = Liftable[Entity]{ e => q"_root_.Entity(${e.tpe}, ${e.localFields}], ${e.joins}, ${e.deps})"}
  def apply(x: Int): Map[String,String] = macro impl
  def impl(c: Context)(x: c.Tree) = { import c.universe._
    //implicit val liftField = Liftable[Field]{ f => q"_root_.Field(${f.name}, ${f.tpe}, ${f.referTo}, ${f.regexp})"}
    val all = Entity.getEntities
    val list = all.values.toList.map(e => (e.tpe.toString, DatabaseUtils.selectQuery(e,all)))

    q"Map(..$list)"
  }
}