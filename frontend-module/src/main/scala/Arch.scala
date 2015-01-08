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
import Dom._
import js.Dynamic.{ global => g }
import rx._
import scala.collection.mutable.ArrayBuffer

object MainApp extends JSApp {
  def main(): Unit = { 
    println("Hello world !")
  }
  @JSExport 
  def start() = {
    /*new ColorInput().renderComponent(document.getElementById("content"))
    new InputWithValidation("Email validation", "\\w+@\\w+\\.\\w{2,3}").renderComponent(document.getElementById("email"))*/
    new Test().renderComponent(document.getElementById("content"));
  }
}

class Test extends Component("ColorInput"){
    private val color = initStream("yellow")
    private val numbers = RxWrapper(1::2::Nil);
	def render(state:js.Dynamic) = {
	    div.clazz("commentBox row form-group")( 
	        p.clazz("control-label col-md-3").onclick(x => color.emit("green"))(
	            color, p.clazz(color)("test"), label.clazz("control-label col-md-3")("Yet another l")),
	        input.onkeyup(_.map(e => e.target.asInstanceOf[HTMLInputElement].value).assign(color))(),
	        numbers.map({e:Int => p.onclick(s => println(""))(e+"")})
        )
	}
}

class ColorInput extends Component("ColorInput"){
    private val color = initStream("yellow")
	def render(state:js.Dynamic) = {
	    Dom.div.clazz("commentBox row form-group")(
	        Dom.label.clazz("control-label col-md-3")("What is your favorite (css) color ?"),
	        Dom.div.clazz("col-md-5")(
	            Dom.input.clazz("form-control").onkeyup(
	                _.map(_.target.asInstanceOf[HTMLInputElement].value).foreach(assign(color))
	            )()
	        ),
	        Dom.div.style("backgroundColor",bind(color))("css color : "+bind(color))
       )
	}
}

class InputWithValidation(label:String, regexp:String) extends Component("InputWithValidation"){
    val valid = initStream(false);
    val color = initStream("white");
    valid.map(valid => if(valid) "lightgreen" else "orangered").foreach(assign(color));
	def render(state:js.Dynamic) = {
      Dom.div(
    		  Dom.label.clazz("control-label col-md-2")(label),
    		  Dom.div.clazz("col-md-5")(Dom.input.clazz("form-control").style("backgroundColor",bind(color)).onkeyup(
    		      _.map(_.target.asInstanceOf[HTMLInputElement].value).map(_.matches(regexp)).foreach(assign(valid)))()),
    		  Dom.div.clazz("control-label col-md-5")(if(bind(valid)) "correct" else "invalid")
          )
	}
}

abstract class Component(name:String){
  val r:js.Dynamic = js.Dynamic.global.React;
  private val initalState = js.Dynamic.literal("text" -> "tactac")
  var tree:DomNode = null
  val reactClass = r.createClass(js.Dynamic.literal(
        "displayName" -> "LikeButton",
        "render" -> ({
          (self:js.Dynamic) => {
              instance = self;
              if(tree == null) tree = render(self.state)
	          tree.view(self)
          }
        }:js.ThisFunction),
        "getInitialState" -> {() => initalState}
  ))
  private var instance:js.Dynamic = null
  def setState(pairs: (java.lang.String, js.Any)*) ={
    val state = js.Dynamic.literal()
    for((key,value) <- pairs){
      state.updateDynamic(key)(value)
    }
    instance.setState(state)
  }
  implicit def strToDomNode(x: String):DomNode = new TextNode(x)
  implicit def streamToDomNode(x: Stream[String]):DomNode = new StreamNode(x)
  def renderIfNeeded(self:js.Dynamic) = render(self).view(instance)
  def render(self:js.Dynamic) : DomNode
  def renderComponent(elem:HTMLElement) : Unit = {
      r.renderComponent(
    	  reactClass(null),
    	  elem
   	  );
  }
  def state:js.Dynamic = ???
  def initStream[T](startValue:T):Stream[T] = {
    val newStream = new Stream[T]()
    initalState.updateDynamic(newStream.id)(startValue.asInstanceOf[js.Any])
    newStream
  }
  def bind[T](s:Stream[T]) : T = instance.state.selectDynamic(s.id).asInstanceOf[T]
  def assign[T](s:Stream[T]): (T => Unit) =  v => {
    s.emit(v)
    setState(s.id -> v.asInstanceOf[js.Dynamic])
  }
  def keep[T](i:Int, s:Stream[List[T]]): (T => Unit) = v => {
    println(instance.state.selectDynamic(s.id))
  }
}


object Stream{
  private var counter = 0;
  def nextId = {
    counter += 1;
    "stream##"+counter
  }
}
class Stream[V](val f:V=>Boolean = (x:V) => true){
  println("newStream")
  var observers = new js.Array[Observer[V,Any]]
  val id = Stream.nextId
  def map[R](f: V => R):Stream[R] = {
    val dest = new Stream[R]();
    observers.push(new Observer(f, dest))
    dest
  }
  def foreach(f: V => Unit): Unit = {
    observers.push(new Observer(f))
  }
  def assign(other:Stream[V]){
    observers.push(new Observer(x => x, other))
  }
  def emit(v:V): Unit = {
    observers.foreach(observer => if(f(v)) observer.newValue(v))
  }
  def fork[R](g: V => Unit, f: V => R):Stream[R] = {
    foreach(g)
    map(f)
  }
  def filter(f: V => Boolean) : Stream[V] = {
    val dest = new Stream[V](f);
    observers.push(new Observer(x => x, dest))
    dest
  }
  def onChange(f: V => Unit) = {
    observers.push(new Observer(f))
  }
  def init : Unit = {}
}
class StreamWithMemory[T](size:Int) extends Stream[List[T]]{
  private var buffer = new js.Array[T]

  override def emit(v:List[T]): Unit = {
    buffer.push(v.head);
    var temp = buffer.toList
    observers.foreach(observer => if(f(buffer.toList)) observer.newValue(buffer.toList))
  }
}
class Observer[A,+B](f:A => B, s:Stream[B] = null){
  def newValue(v:A): Unit = {
    val next = f(v);
    if(s != null) s.emit(next)
  }
}


object Dom {
  def input = new DomBuilder("input");
  def div = new DomBuilder("div");
  def p = new DomBuilder("p");
  def label = new DomBuilder("label");
}

class DomBuilder (tag:String, static: Map[String, js.Any] = Map(), reactive : Map[String, Stream[String]] = Map()){
  val r = js.Dynamic.global.React.DOM;
  
  def onclick(f: dom.MouseEvent => Unit): DomBuilder = {
    new DomBuilder(tag, static + ("onClick" -> f))
  }
  def onkeyup(handles: (Stream[dom.KeyboardEvent] => Unit)*): DomBuilder = {
    val start = new Stream[dom.KeyboardEvent]();
    handles.foreach( _(start))
    val t: dom.KeyboardEvent => Unit = { e:dom.KeyboardEvent => {start.emit(e)} }
    new DomBuilder(tag, static + ("onKeyUp" -> t), reactive)
  }
  def attr(attr: String, value: String): DomBuilder = {
     new DomBuilder(tag, static + (attr -> value), reactive)
  }
  def attr(attr: String, value: Stream[String]) = {
     new DomBuilder(tag, static, reactive + (attr -> value))
  }
  def clazz(name:String) = {
    attr("className", name)
  }
  def clazz(name:Stream[String]) = {
    attr("className", name)
  }
  def apply(implicit children: DomNode*): DomNode = {
    new SimpleDomNode(tag, children, static, reactive)
  }
  def style(prop: String, value: String) = {
    // static.updateDynamic("style")(js.Dynamic.literal(prop -> value))
    this
  }
}
abstract class DomNode{
  def view(self:js.Dynamic): js.Any
}
class SimpleDomNode (tag:String, childs:Seq[DomNode], initState:Map[String, js.Any], reactive: Map[String, Stream[String]]) extends DomNode{
  var upToDate = true;
  val r = js.Dynamic.global.React.DOM;
  val s = js.Dynamic.literal()
  initState.foreach( { case (k, v) => s.updateDynamic(k)(v) })
  var firstRender = true;
  def view(self : js.Dynamic): js.Any = {
    if(firstRender){
    	reactive.foreach( { case (k, stream) => stream.onChange(newVal => {println("newVal");self.applyDynamic("setState")(js.Dynamic.literal(k->newVal))}) })
    	firstRender = false;
    }
    r.selectDynamic(tag)(s +: childs.map(_.view(self)):_*)
  }
}
class TextNode(s: String) extends DomNode{
  override def view(self : js.Dynamic) = s
}
class StreamNode(s: Stream[String]) extends DomNode{
  private var currentVal = ""
  private var firstRender = true;
  override def view(self : js.Dynamic) = {
    if(firstRender){
      s.onChange(newVal => {
        println("changed")
	    if(currentVal != newVal) self.applyDynamic("setState")(js.Dynamic.literal())
	    currentVal = newVal
	    firstRender = false;
      })
    }
    currentVal
  }
}

class RxNode[T](init:Seq[T], f:T => DomNode) extends DomNode{
  private var seq = ArrayBuffer[T](init:_*);
  override def view(self : js.Dynamic) = {
    seq.map(f).map(_.view(self)).toArray.asInstanceOf[js.Array[js.Any]]
  }
}

case class RxWrapper[T](init:Seq[T]){
  def map(f:T => DomNode) = new RxNode[T](init, f)
}

/**
 * Not used anymore
 */
package object react extends js.GlobalScope {
   val React: React = ???
}

trait React extends js.Object {
    @JSBracketAccess
	def apply(s:String): js.Dynamic = ???
}
trait DomReact{
	def div(url: js.Object, target: js.String): DomReact = ???
}	