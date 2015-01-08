package controllers

import play.api.mvc._
import utils.jsonoversql._
import nl.flotsam.xeger._

trait MockSettings {
  /**
   * Number to mock of this type to be generated
   */
  def quantity : Int
  def schemas:Map[String, String]

  def targetEntity:Entity
}

/**
 * Primarily add the support for  \w, \d, \s
 */
object XegerMapper {
  def randomString(regex:String):String = {
    val simpleRegex = regex
      .substring(1, regex.length-1)
      .replace("\\\\d","[0-9]")
      .replace("\\\\w","[0-9a-zA-Z_]")
      .replace("\\\\s","[ \t\n]")
    val generator = new Xeger(simpleRegex);
    generator.generate();
  }
}

object MockGenerator extends Controller{


  def fillWithMock = Action {
    val schemas = Entity.getEntities
    Ok(schemas.values.map(randomJsonFor(_)).mkString("\n"))
  }

  private def randomJsonFor(e:Entity) = {
    def randomVal(f:Field) = {
      f.regexp match {
        case Some(regex) => {
          '"' + XegerMapper.randomString(regex) + '"';
        }
        case None => {
          if(f.name.matches("^.*_id$")){
            Math.round(Math.random()*9)+1
          } else {
            f.tpe.toString match {
              case "String" => "great"
              case _ => "not string"
            }
          }
        }
      }
    }
    val pairs = e.localFields.map( f => s""" "${f.name}":${randomVal(f)} """)
    s"{${pairs.mkString(",")}}"
  }
}
