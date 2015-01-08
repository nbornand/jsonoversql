package utils.jsonoversql

import java.io.File

import scala.collection.mutable
import scala.reflect.macros._
import scala.reflect.runtime.universe._
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import play.api.libs.json._
import utils.jsonoversql._

case object Storage{

  case class Storage(select:String)

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

  def apply(e:Int): Map[String,Storage] = macro impl

  def impl(c: Context)(e:c.Expr[Int]) = {
      import c.universe._
      val entities = Entity.getEntities
      implicit val lift = Liftable[Storage]{ s => q"Storage.Storage(${s.select})"}
      val list =  entities.values.map(e => (e.tpe.toString, Storage(selectQuery(e, entities))) )
      println(list)
      q"Map(..$list)"
  }

}

case object Remote{

  /*def implWhere[T](c: Context)(f:c.Expr[T => Boolean]): (Int,c.Expr[T=>RemoteQuery]) = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    def toSQL(t:Tree):Tree = t match{
      case Apply(Select(target, func), args) => {
        val left = toSQL(target)
        val right = toSQL(args.head)
        func.toString match{
          case "$eq$eq" => q"""$left + " EQUALS " + $right"""
          case "$amp$amp" => q"""$left + " AND " + $right"""
          case "startsWith" => q"""$left + " LIKE %" + $right"""
          case "matches" => q"""$left + " REGEXP '" + $right + "'" """
          case "$plus" => t
          case other => throw new Exception(s"method $other not supported")
        }
      }
      case s @ Select(Ident(obj), field) => {
        selected += s
        s
      }
      case _ => {
        t
      }
    }
    val sql = f match{
      case Expr(Function(params, body)) => {
        toSQL(body)
        val uid = java.util.UUID.randomUUID.toString
        c.Expr(Function(params, q""" RemoteQuery($uid, List(..${selected.toList})) """))
      }
      case _ => sys.exit(1)
    }

    println(selected.toList)

    (1,sql)
  }*/

  def where[T](f:((T,Int)=>Boolean)): ((T,Int)=>RemoteQuery) = macro implWhere[T]

  def implWhere[T](c: Context)(f:c.Expr[(T,Int) => Boolean]): c.Expr[(T,Int) => RemoteQuery] = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    def toSQL(t:Tree):Tree = t match{
      case Apply(Select(target, func), args) => {
        val left = toSQL(target)
        val right = toSQL(args.head)
        func.toString match{
          case "$eq$eq" => q"""$left + " EQUALS " + $right"""
          case "$amp$amp" => q"""$left + " AND " + $right"""
          case "startsWith" => q"""$left + " LIKE %" + $right"""
          case "matches" => q"""$left + " REGEXP '" + $right + "'" """
          case "$plus" => t
          case other => throw new Exception(s"method $other not supported")
        }
      }
      case s @ Select(Ident(obj), field) => {
        selected += s
        s
      }
      case _ => {
        t
      }
    }
    val sql = f match{
      case Expr(Function(params, body)) => {
        toSQL(body)
        val uid = java.util.UUID.randomUUID.toString
        c.Expr(Function(params, q""" RemoteQuery($uid, List(..${selected.toList})) """))
      }
      case _ => sys.exit(1)
    }

    println(selected.toList)
    println(showRaw(sql))

    sql
  }

  private val queries = scala.collection.mutable.Map[String,String]()

  /*def select[T](f:(T=>Product))(where:(T=>Boolean)): T=>RemoteQuery = macro implSelect[T]

  def implSelect[T](c: Context)(f:c.Expr[T => Product])(where:c.Expr[(T=>Boolean)]): c.Expr[T=>RemoteQuery] = {
    import c.universe._

    //implicit val lift = Liftable[RemoteQuery]{ s => q"RemoteQuery(${s.select})"}

    def collectRows(l:List[Any]) = l.map( _ match {
      case Select(Ident(TermName(root)), TermName(columnName)) => columnName
      case Literal(Constant(value:String)) => value
      case _ => throw new Exception("Parameters fed to select should be either a String or an identifier")
    })

    val s = implWhere[T](c)(where)
    println(showRaw(s))

    val sql = f match{
      case Expr(Function(params, body)) => body match{
        case Apply(typeApply, l:List[Any]) => collectRows(l)
      }
      case _ => sys.exit(1)
    }
    queries += (("test" -> sql.mkString))

    //Print to file

    appendSqlToFile("test", sql.mkString)

    val temp = "(fake)"
    s
  }

  def appendSqlToFile(name:String, sql:String): Unit = {
    println("=>>> append to file")
    val md = java.security.MessageDigest.getInstance("MD5")
    val code = new String(md.digest(sql.getBytes("UTF-8")))
    println(code)

    val p = new java.io.PrintWriter(new File("macrores.temp"))
    p.println(s"$name;$sql;$code")
    p.println("Test"+System.currentTimeMillis()+" ")
    p.close()
  }*/

}

case class RemoteQuery(uid:String, params:List[Any])