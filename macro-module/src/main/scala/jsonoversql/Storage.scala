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

  def all[T]: BasicRequest[T] = macro implAll[T]

  def implAll[T](c: Context)(implicit weakType: c.WeakTypeTag[T]): c.Expr[BasicRequest[T]] = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    val tpe = weakType.tpe
    tpe.members.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => println(m.paramLists)
    }
    //Entity.fromType(tpe)

    val tableName = tpe.toString.toLowerCase+"s"

    val ret = ""
    c.Expr[BasicRequest[T]](q"""BasicRequest[$tpe]("", $tableName, Nil)""")
  }

  def prepare(entity:Entity):PreparedSQL = {
    val create = createTableQueries(entity)
    val select = selectQuery(entity, Map(entity.tpe -> entity))
    PreparedSQL(create, select)
  }
}

object Request{
  def all[T](implicit t:WeakTypeTag[T]) = {
    val table = t.tpe.toString.toLowerCase+"s"
    BasicRequest[T]("", table, Nil)
  }
}
trait Request[T]{
  //def preds:Seq[Criteria[T]]
  def url:String
  def entityName:String
  def dataToSend:List[Any]
}
case class BasicRequest[T](url: String, entityName:String, dataToSend:List[Any]) extends Request[T]{
  def where(p:Criteria[T]) = WhereRequest[T](url+p.id, entityName, dataToSend ::: p.toSend())
}
case class WhereRequest[T](url: String, entityName:String, dataToSend:List[Any]) extends Request[T]{
  def and(p:Criteria[T]) = WhereRequest[T](s"${url}/and/${p.id}", entityName, dataToSend ::: p.toSend())
  def or(p:Criteria[T]) = WhereRequest[T](s"${url}/or/${p.id}", entityName, dataToSend ::: p.toSend())
  def fullUrl: String = s"$entityName/filter/"+url
}

case class Criteria[T](sql:String, id:Int, toSend: () => List[Any] = () => List[Any]())

case object Criteria{

  /**
   * Count the number of time the build macro is expanded. Work as a identifier for the SQL code generated that
   * stays consistant when we recompile.
   */
  var counter = 1;

  private val queries = scala.collection.mutable.Map[String,String]()

  def build[T](f:(T=>Boolean)): Criteria[T] = macro implBuildCriteria[T]

  def implBuildCriteria[T](c: Context)(f:c.Expr[T => Boolean])(implicit weakType: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._

    val selected = scala.collection.mutable.ListBuffer[Tree]()

    val tpe = weakType.tpe

    val tableName = tpe.toString.toLowerCase+"s"


    def toSQL(t:Tree):String = t match{
      case Apply(Select(target, func), args) => {
        val left = toSQL(target)
        //TODO not only head
        val right = toSQL(args.head)
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

        val toSend = q"() => List(..${selected.toList})"
        val res= c.Expr(q""" Criteria[$tpe]($sqlSnippet, $counter, $toSend) """)
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