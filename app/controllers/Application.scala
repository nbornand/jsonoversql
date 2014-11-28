package controllers

import play.api.mvc._
import utils.jsonoversql._
import play.api.libs.json._

object Application extends Controller{

  def get(path:String) = Action {
    RestDefault.run();
    val tree = Json.parse("""{"a":"test", "b":12}""")

    RestDefault.post(path, tree)
    /*val regex:String = "[ab]{4,6}c";
    val generator = new Xeger(regex);
    val result = generator.generate();
    Ok(result)*/
  }
}