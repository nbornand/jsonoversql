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

    val (entity, relations) = Entity.fromType(typeOf[Person])
    val sql = Storage.prepare(entity)
    println(sql.create)

    val req = Storage.all[Person].where(nameMatches("nicolas")).and(isAdult)
    println(req.fullUrl)
    println(req.dataToSend)

    val s = div(
        h1(id:="title", "Title"),
        p("random text")
      )
    println(s.render)

    Entity.register[Course]
    Entity.register[Person]

    val schemas = Entity.buildSchemas
    val personSchema = schemas.getFor[Person].get
    println(personSchema.create)
    println(personSchema.select)
  }
}
