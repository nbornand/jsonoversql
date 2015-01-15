import utils.jsonoversql._
import reflect.runtime.universe._
import scalatags.Text.all._

case class Person(first:String, surname:String, age:Int, other:List[Course])
case class Course(name:String)

object RequestBuilder {
  def main(args: Array[String]) {

    val isAdult = Criteria.build[Person](p => p.age >= 18)
    val nameMatches2 = (name: String) => Criteria.build[Person](p => p.first == "jason" || p.surname.startsWith("a"))

    val nameMatches = (name: String) => {
      val short = name.take(3) + "\\w+"
      Criteria.build[Person](s => s.first == name || s.surname.matches(short))
    }

    val req = Storage.all[Person].where(nameMatches("nicolas")).and(isAdult)
    println(req.fullUrl)
    println(req.dataToSend)

    Entity.register[Person]
    Entity.register[Course]
    val schemas = Entity.buildSchemas

    val person = schemas.forServer[Person]
    println(person.create)
    println(person.select)

    val personSchema = Schemas.forClient[Person](schemas)

    println(schemas.materializeFilter(req.fullUrl))

  }
}
