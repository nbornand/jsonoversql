package utils.jsonoversql

import java.io.File

import scala.collection.mutable
import scala.reflect.macros._
import scala.reflect.runtime.universe._
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import play.api.libs.json._
import utils.jsonoversql._

case class PreparedSQL(create:String, select:String)

case object Storage{

  case class Storage(select:String)

  private val m = Map[String, Option[Int] => String](
    "String" -> (i => s"varchar(${i.getOrElse(255)})"),
    "scala.Int" -> (i => s"int(${i.getOrElse(11)})"),
    "Int" -> (i => s"int(${i.getOrElse(11)})"),
    "scala.Boolean" -> (i => "boolean")
  )

  def nameOf(t:String):String = t.toLowerCase

  def createTableQueries(entity:Entity): String = {

    val columns = "`id` int(11) NOT NULL auto_increment" :: "PRIMARY KEY (`id`)" ::
      entity.localFields.map( field => {
        val sqlType = m.get(field.tpe.toString).getOrElse(throw new Exception(s"Undefined mapping for type ${field.tpe}"))(None)
        s"`${field.name}` $sqlType ${field.referTo.map(foreign => s" REFERENCES ${nameOf(foreign.toString)}s(id)").mkString}"
    })

    val sql = s"CREATE TABLE IF NOT EXISTS `${nameOf(entity.tpe)}s`(${columns.mkString(",")})ENGINE=INNODB;"
    sql
  }

  def selectQuery(target:Entity, allEntities:Map[String,Entity]):String = {
    val entitiesToJoin = mutable.Set[String]();
    val jsonConcatenation = jsonForEntity(target, allEntities, 1, entitiesToJoin)
    val groupBy = entitiesToJoin.map(
      foreign => s"LEFT JOIN ${nameOf(foreign.toString)}s ON ${nameOf(target.tpe)}s.id=${nameOf(foreign.toString)}s.${nameOf(target.tpe)}_id"
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

  private def jsonForEntity(target:Entity, allEntities:Map[String,Entity], depth:Int, groupBy:mutable.Set[String]):String = {
    val components = target.localFields.map {
      field => "'\"" + field.name + "\":\'," + nameOf(target.tpe) + s"s.${field.name}"
    } ::: {
      if(depth>0) {
        target.joins.map({
          case OneTo(name, from, to) => {
            val otherEntity = allEntities(to.toString)
            groupBy += to.toString
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
      q"Map(..$list)"
  }

  def all[T]: BasicRequest[T] = macro implAll[T]

  def implAll[T](c: Context)(implicit weakType: c.WeakTypeTag[T]): c.Expr[BasicRequest[T]] = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    val tpe = weakType.tpe

    val tableName = tpe.toString.toLowerCase+"s"

    c.Expr[BasicRequest[T]](q"""BasicRequest[$tpe]("", $tableName, Nil)""")
  }

  def prepare(entity:Entity):PreparedSQL = {
    val create = createTableQueries(entity)
    val select = selectQuery(entity, Map(entity.tpe -> entity))
    PreparedSQL(create, select)
  }
}

case class CriteriaClient[+T](sql:String, id:Int, toSend: () => List[Any] = () => List[Any]())
case class CriteriaServer(id:Int, sql:String)

case object Criteria{

  /**
   * Count the number of time the build macro is expanded. Work as a identifier for the SQL code generated that
   * stays consistant when we recompile.
   */
  var counter = 1;

  val queries = scala.collection.mutable.ListBuffer[CriteriaServer]()

  def build[T](f:(T=>Boolean)): CriteriaClient[T] = macro implBuildCriteria[T]

  def implBuildCriteria[T](c: Context)(f:c.Expr[T => Boolean])(implicit weakType: c.WeakTypeTag[T]): c.Expr[CriteriaClient[T]] = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    val tpe = weakType.tpe

    val tableName = tpe.toString.toLowerCase+"s"


    def toSQL(t:Tree):String = t match{
      case Apply(Select(target, func), Nil) => {
        val left = toSQL(target)
        func.toString match{
          case "toString" => s"$left"
          case other => {
            throw new UnsupportedOperationException(s"$other not supported for scala => SQL conversion")
          }
        }
      }
      case Apply(Select(target, func), first :: rest) => {
        val left = toSQL(target)
        val right = toSQL(first)
        func.toString match{
          // => Boolean operators
          case "$eq$eq" => s"""$left = $right"""
          case "$greater" => s"""$left > $right"""
          case "$greater$eq" => s"""$left >= $right"""
          case "$amp$amp" => s"""$left AND $right"""
          case "$bar$bar" => s"""$left OR $right"""
          //string matching
          case "startsWith" => s"""$left LIKE '%$right'"""
          case "matches" => s"""$left REGEXP '$right'"""
          case "$plus" => "#plus"
          case other => {
            throw new UnsupportedOperationException(s"$other not supported for scala => SQL conversion")
          }
        }
      }
      case s @ Select(Ident(obj), field) => {
        println(obj)
        s"$tableName."+field.toString
      }
      case Literal(Constant(value)) => value.toString
      case other => {
        selected += other
        "?"
      }
    }

    val criteria = f match{
      case Expr(Function(params, body)) => {
        val sqlSnippet = toSQL(body)
        println(s"SQL#$counter [$sqlSnippet]")
        appendSqlToFile("name#"+counter, counter, sqlSnippet)
        val uid = java.util.UUID.randomUUID.toString

        val toSend = c.Expr[() => List[Any]](q"() => List(..${selected.toList})")
        val res = c.Expr[CriteriaClient[T]](q""" CriteriaClient[$tpe]($sqlSnippet, $counter, $toSend) """)

        queries.append(CriteriaServer(counter, sqlSnippet))

        counter += 1;
        res
      }
      case _ => sys.exit(1)
    }

    criteria
  }

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
  }*/

  val sharedTempFile = new java.io.PrintWriter(new File("macrores.temp"))
  sharedTempFile.print("")
  
  def appendSqlToFile(name:String, id:Int, sql:String): Unit = {
    val md = java.security.MessageDigest.getInstance("MD5")
    val code = new String(md.digest(sql.getBytes("UTF-8")))

    sharedTempFile.append(s"$name;$id;$sql")
    sharedTempFile.println("")
    sharedTempFile.flush()
  }

}

case object View{

  var counter = 1;

}

case class RemoteQuery(url:String, params:List[Any])