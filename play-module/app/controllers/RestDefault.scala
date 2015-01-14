package controllers

import java.sql.{Connection, DriverManager}
import play.api.libs.json._
import play.api.mvc._
import play.api.db._
import play.api.Play.current
import utils.jsonoversql._

object RestDefault extends Controller{

  val entities = Entity.getEntities.map{case (tpe, entity) => (tpe.toString, entity)}

  /*def run2(): Unit = {

  }
    getConnection match {
      case connection:Connection => {

        val statement = connection.prepareStatement("SELECT * in ?")
        statement.setString(1, "dummy2s")

        println(statement.execute())

        connection.close()
      }
  }*/

	def get(path:String) = Action {

    val sb = new StringBuilder()
    sb.append("random")

    DB.withConnection{ con => {

        con.close()
        Ok("connection ok")
      }
    }

	    /*getConnection match {
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
		}*/
	}

  def post(entityName:String, json:JsValue):Result = {
    /*val target = entities.values.collectFirst{ case e if e.tpe.toString.split("\\.").last.toLowerCase() == entityName => e}
    target match {
      case Some(entity) => {
        val query = Storage.insertQuery(entity, json);
        val statement = getConnection.createStatement()
        statement.execute(query)
        Ok("POST "+entityName+System.currentTimeMillis())
      }
      case None => NotFound(s"404 : The entity $entityName doesn't exist")
    }*/
    Ok("as")
  }
}