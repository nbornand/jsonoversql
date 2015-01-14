package react.prototype

import scala.scalajs._
import scala.scalajs.js.JSApp
import org.scalajs._
import dom.document
import js.annotation._
import org.scalajs.dom.Window
import scala.scalajs.js.ThisFunction
import org.scalajs.dom.HTMLInputElement
import scala.scalajs.js.RegExp
import org.scalajs.dom.HTMLElement
import js.Dynamic.{ global => g }
import scala.collection.mutable.ArrayBuffer
import upickle._
import autowire._
import scala.language.implicitConversions
import scalatags.Text.all._
import utils.jsonoversql._
/*import japgolly.scalajs.react._
import vdom.ReactVDom._
import vdom.ReactVDom.all._*/


case class PickleTest(name:String, age:Int)
case class A(i:Int)

object MainApp extends JSApp {
  def main(): Unit = { 
    println("Hello world 2!")
    println(write(Seq(1, 2, 3)))
    upickle.write(A(21))
    println(read[PickleTest](s"""${write(PickleTest("nico",19))}"""))

    case class Person(name:String, age:Int)
    //println(write(Person("nico",21)))
    //var full = Criteria.select[Person](s => (s.name,"baba"))(s => s.age == 21 && s.name.startsWith("t"))

    //println("query : "+full(Person("test",20)))

    val frag = div(
        h1("This is my title"),
        div(
          p("This is my first paragraph"),
          p("This is my second paragraph")
        )
    )
    println(frag)
    val xhr = new dom.XMLHttpRequest
    xhr.open("GET", "http://localhost:9000/mocks")
    xhr.onload = (e:dom.Event) => {
      println(xhr.responseText)
    }
    xhr.send()

    val test = document.createElement("div")
    test.innerHTML = frag.toString()
    document.body.appendChild(test)
}
  @JSExport 
  def start() = {
    /*new ColorInput().renderComponent(document.getElementById("content"))
    new InputWithValidation("Email validation", "\\w+@\\w+\\.\\w{2,3}").renderComponent(document.getElementById("email"))*/
    //new Test().renderComponent(document.getElementById("content"));
  }
}