package utils.jsonoversql

import scala.collection.mutable.ListBuffer
import javax.validation.constraints._
import scala.reflect.runtime.universe._
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

case class Entity(tpe:String, localFields:List[Field], joins:List[Relation], deps:Int=0)
case class Field(name:String, tpe:String, referTo:Option[String] = None, regexp:Option[String] = None)
case class Void(s:String)

sealed trait Relation{
  def name:String
  def from:String
  def to:String
}

case class OneTo(name:String, from:String, to:String) extends Relation
case class ManyTo(name:String, from:String, to:String) extends Relation

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
    "Int" -> (i => s"int(${i.getOrElse(11)})"),
    "scala.Boolean" -> (i => "boolean")
  )

  private def nameOf(t:String):String = t.toLowerCase

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
    val onePlusMany:List[Relation] = oneTo.map(t => OneTo(t.name.toString, tpe.toString, t.typeSignature.toString)) :::
      manyTo.map(t => ManyTo(t.name.toString, tpe.toString, t.typeSignature.typeArgs.head.toString))

    val simples:List[Field] = simpleTypes.map(t => {

      //val pattern = t.annotations.collectFirst{case a => a.javaArgs.contains(newTermName("regexp"))}
      val pattern = t.annotations.collectFirst{
        case a => a.tree.children.tail.collectFirst{case t => t.children.tail.head.toString}.get
      }

      Field(t.name.toString, t.typeSignature.toString, None, pattern)
    })

    (
      Entity(tpe.toString, simples, onePlusMany),
      onePlusMany
      )
  }

  private def buildEntities(types:Type*): Map[String, Entity] = {

    def getTypeParams(tpe: Type): List[Symbol] = {
      val constructor = tpe.members.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }
      constructor.head.paramLists.head
    }

    val (entitiesT, relationsT) = types.map(entityType => fromType(entityType)).unzip
    val entitiesWithoutRelations = entitiesT.map(entity => (entity.tpe, entity)).toMap
    val relations = relationsT.flatten

    def addRelations(entities: Map[String, Entity], relations: List[Relation]): Map[String, Entity] = relations match {
      case Nil => entities
      case first :: others => {
        val foreign = entities.get(first.to.toString).getOrElse(throw new Exception(""))
        val extraField = Field(nameOf(first.from.toString)+"_id", typeOf[scala.Int].toString, Some(first.from.toString))
        addRelations(entities + (first.to.toString -> Entity(foreign.tpe, extraField :: foreign.localFields,foreign.joins, foreign.deps+1)), others)
      }
    }

    addRelations(entitiesWithoutRelations, relations.toList)
  }


  private val entities =  buildEntities(weakTypeOf[Dummy], weakTypeOf[Dummy2], weakTypeOf[C])

  def getEntities = entities

  /**
   * Register types => entities
   */
  private val registeredEntities = ListBuffer[Entity]()
  private val relationsBuffer = ListBuffer[Relation]()

  def register[T]: Unit = macro implRegisterType[T]

  def implRegisterType[T](c: Context)(implicit weakType: c.WeakTypeTag[T]): c.Expr[Unit] = {
    import c.universe._

    val tpe = weakTypeTag[T].tpe

    val constructor = tpe.members.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }
    val params = constructor.head.paramLists.head

    val (simpleTypes, references) = params.partition( s => {
      m.contains(s.typeSignature.toString)
    })

    val (manyTo, oneTo) = references.partition( s => s.typeSignature <:< typeOf[List[_]] )
    val onePlusMany:List[Relation] = oneTo.map(t => OneTo(t.name.toString, tpe.toString, t.typeSignature.toString)) :::
      manyTo.map(t => ManyTo(t.name.toString, tpe.toString, t.typeSignature.typeArgs.head.toString))

    val simples:List[Field] = simpleTypes.map(t => {

      //val pattern = t.annotations.collectFirst{case a => a.javaArgs.contains(newTermName("regexp"))}
      val pattern = t.annotations.collectFirst{
        case a => a.tree.children.tail.collectFirst{case t => t.children.tail.head.toString}.get
      }

      Field(t.name.toString, t.typeSignature.toString, None, pattern)
    })

    registeredEntities.append(Entity(tpe.toString, simples, onePlusMany))
    relationsBuffer.append(onePlusMany:_*)

    c.Expr[Unit](q"()")
  }


  def buildSchemas: Schemas = macro implBuild

  def implBuild(c: Context): c.Expr[Schemas] = {
    import c.universe._

    implicit val liftPrepared = Liftable[PreparedSQL]{ s => q"PreparedSQL(${s.create}, ${s.select})"}
    implicit val liftCriteria = Liftable[CriteriaServer]{ c => q"CriteriaServer(${c.id}, ${c.sql})"}

    val entitiesWithoutRelations = registeredEntities.map(entity => (entity.tpe, entity)).toMap
    val relations = relationsBuffer.toList

    def addRelations(entities: Map[String, Entity], relations: List[Relation]): Map[String, Entity] = relations match {
      case Nil => entities
      case first :: others => {
        val foreign = entities.get(first.to.toString).getOrElse{
          throw new Exception(s"Entity ${first.from} refer to ${first.to} but it couldn't be found. Maybe you have forgotten to use Entity.register[${first.to}]")
        }
        val extraField = Field(nameOf(first.from.toString)+"_id", typeOf[scala.Int].toString, Some(first.from.toString))
        addRelations(entities + (first.to.toString -> Entity(foreign.tpe, extraField :: foreign.localFields,foreign.joins, foreign.deps+1)), others)
      }
    }

    val res = addRelations(entitiesWithoutRelations, relations.toList)
    val preparedQueries = res.map{ case (tpe, entity) => (tpe, Storage.prepare(entity)) }
    val (tpes, preparedSqls) = preparedQueries.unzip

    c.Expr[Schemas](q"Schemas(${tpes.toList}, ${preparedSqls.toList}, ${Criteria.queries.toList})")
  }

}