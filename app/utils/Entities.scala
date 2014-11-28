package utils.jsonoversql

import scala.reflect.runtime.universe._
import scala.reflect.macros._
import javax.validation.constraints._
import scala.language.experimental.macros

case class Entity(tpe:Type, localFields:List[Field], joins:List[Relation], deps:Int=0)
case class Field(name:String, tpe:Type, referTo:Option[Type] = None, regexp:Option[String] = None)

sealed trait Relation{
  def name:String
  def from:Type
  def to:Type
}

case class OneTo(name:String, from:Type, to:Type) extends Relation
case class ManyTo(name:String, from:Type, to:Type) extends Relation

/*
 * Schema classes
 */

case class Dummy(
  @Pattern(regexp = "\\d{2,4}")e:String,
  c:C
)
case class Dummy2(
  @Pattern(regexp = "\\w+") @NotNull a:String,
  @Pattern(regexp = "\\d{2,4}") b:Int,
  l:List[Dummy]
)
case class C(name:String)

object Entity{

  private val m = Map[String, Option[Int] => String](
    "String" -> (i => s"varchar(${i.getOrElse(255)})"),
    "scala.Int" -> (i => s"int(${i.getOrElse(11)})"),
    "Int" -> (i => s"int(${i.getOrElse(11)})")
  )

  private def nameOf(t:Type):String = t.typeSymbol.name.toString.toLowerCase

  /**
   * Split the fields of an entity into lists of local field and relations
   * @param tpe a case class corresponding to an entity
   * @return (simple fields, relations)
   */
  def fromType(tpe:Type): (Entity, List[Relation]) = {

    val constructor = tpe.members.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }
    val params = constructor.head.paramLists.head

    val (simpleTypes, references) = params.partition( s => {
      m.contains(s.typeSignature.toString)
    })

    val (manyTo, oneTo) = references.partition( s => s.typeSignature <:< typeOf[List[_]] )
    val onePlusMany:List[Relation] = oneTo.map(t => OneTo(t.name.toString, tpe, t.typeSignature)) :::
      manyTo.map(t => ManyTo(t.name.toString, tpe, t.typeSignature.typeArgs.head))

    val simples:List[Field] = simpleTypes.map(t => {

      //val pattern = t.annotations.collectFirst{case a => a.javaArgs.contains(newTermName("regexp"))}
      val pattern = t.annotations.collectFirst{
        case a => a.tree.children.tail.collectFirst{case t => t.children.tail.head.toString}.get
      }

      Field(t.name.toString, t.typeSignature, None, pattern)
    })

    (
      Entity(tpe, simples, onePlusMany),
      onePlusMany
      )
  }

  private def buildEntities(types:Type*): Map[Type, Entity] = {

    def getTypeParams(tpe: Type): List[Symbol] = {
      val constructor = tpe.members.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }
      constructor.head.paramLists.head
    }

    val (entitiesT, relationsT) = types.map(entityType => fromType(entityType)).unzip
    val entitiesWithoutRelations = entitiesT.map(entity => (entity.tpe, entity)).toMap
    val relations = relationsT.flatten

    def addRelations(entities: Map[Type, Entity], relations: List[Relation]): Map[Type, Entity] = relations match {
      case Nil => entities
      case first :: others => {
        val foreign = entities.get(first.to).getOrElse(throw new Exception(""))
        val extraField = Field(nameOf(first.from)+"_id", typeOf[scala.Int], Some(first.from))
        addRelations(entities + (first.to -> Entity(foreign.tpe, extraField :: foreign.localFields,foreign.joins, foreign.deps+1)), others)
      }
    }

    addRelations(entitiesWithoutRelations, relations.toList)
  }


  private val entities =  buildEntities(weakTypeOf[Dummy], weakTypeOf[Dummy2], weakTypeOf[C])

  def getEntities = entities

}