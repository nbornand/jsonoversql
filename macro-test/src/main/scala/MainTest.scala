import utils.jsonoversql._

object MainTest{
  def main(args:Array[String]) {

    //val sql = Remote.where[String](s => (s + "other" == "test" && s.startsWith("t")))
    //println("##############"+sql("test"))


    case class Person(name:String, age:Int)
    var full = Remote.where[Person]( (s,i) => s.age == 21 && s.name.startsWith("t") && i == 2)
    println("query : "+full)

  }
}
