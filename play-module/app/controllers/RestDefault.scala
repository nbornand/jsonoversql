package controllers

import java.sql.{Connection, DriverManager}
import play.api.libs.json._
import play.api.mvc._
import scala.reflect.runtime.universe._
import utils.jsonoversql._

object RestDefault extends Controller{

  private def getConnection: Connection = {

    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost/jsonoversql"
    val username = "itbeautyadmin"
    val password = "1234"

    Class.forName(driver)
    DriverManager.getConnection(url, username, password)
  }

  val entities = Entity.getEntities

  val s = Entity.build((x:Entity) => "a")
  Entity.build(e => "")
  Storage(2)

  def run2(): Unit = {

  }
    getConnection match {
      case connection:Connection => {

        val statement = connection.prepareStatement("SELECT * in ?")
        statement.setString(1, "dummy2s")

        println(statement.execute())

        connection.close()
      }
  }

	def get() {

	    getConnection match {
        case connection:Connection => {

          val create = entities.values.toList.sortWith(_.deps > _.deps).map(Storage.createTableQueries(_))

          val statement = connection.createStatement()

         create.foreach(
           createTable => statement.execute(createTable)
         )

          entities.values.toList.sortWith(_.deps > _.deps).foreach(e => {
            val query = Storage.selectQuery(e, entities)
            println(query)
            val rs = statement.executeQuery(query)
            val sb = new StringBuilder();
            sb.append("[")
            while(rs.next){
              if(!rs.isFirst) sb.append(",")
              sb.append(rs.getString("json"))
            }
            sb.append("]")
            println(sb.toString())
          }

          )
         connection.close()
        }
		}
	}

  def post(entityName:String, json:JsValue):Result = {
    val target = entities.values.collectFirst{ case e if e.tpe.toString.split("\\.").last.toLowerCase() == entityName => e}
    target match {
      case Some(entity) => {
        val query = Storage.insertQuery(entity, json);
        val statement = getConnection.createStatement()
        statement.execute(query)
        Ok("POST "+entityName+System.currentTimeMillis())
      }
      case None => NotFound(s"404 : The entity $entityName doesn't exist")
    }
  }
}