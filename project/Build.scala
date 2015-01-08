import sbt._
import sbt.Keys._
import play._
import play.PlayImport.PlayKeys._
import com.typesafe.sbt.web.Import.Assets
import scala.scalajs.sbtplugin.ScalaJSPlugin
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._


object ApplicationBuild extends Build {

  //settings common to all projects
  val common = Seq(
    scalaVersion := "2.11.2",
    scalacOptions ++= Seq("-feature", "-deprecation")
  )

  object Deps {
    val upickle = "com.lihaoyi" %%%! "upickle" % "0.2.5"
    val scalaJsReact = "com.github.japgolly.scalajs-react" %%%! "core" % "0.6.0"
    val autowireJVM = "com.lihaoyi" %% "autowire" % "0.2.3"
    val autowireJS = "com.lihaoyi" %%%! "autowire" % "0.2.3"
    val jquery = "org.scala-lang.modules.scalajs" %%%! "scalajs-jquery" % "0.6"
  }

  lazy val root = Project("root", file(".")).aggregate(
    play,
    macro,
    frontend
  )

  lazy val test = (
    Project("macro-test", file("macro-test"))
      settings(common: _*)
      dependsOn(macro)
    )

  lazy val macro = (
    Project("macro-module", file("macro-module"))
    settings(common: _*)
    settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "com.typesafe.play" %% "play-json" % "2.3.4"
      )
    )
  )

  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  val generateSql = taskKey[String]("A sample string task.")

  lazy val frontend = (
    Project("frontend-module", file("frontend-module"))
      settings(ScalaJSPlugin.scalaJSSettings: _*)
      settings(common: _*)
      settings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %%%! "scalajs-dom" % "0.6",
        "com.scalarx" %%%! "scalarx" % "0.2.5",
        "com.scalatags" %%%! "scalatags" % "0.4.2",
        Deps.upickle,
        Deps.scalaJsReact,
        Deps.autowireJS,
        Deps.jquery
      )
      )
      dependsOn(macro)
  )

  lazy val play = (
    Project("play-module", file("play-module"))
	  enablePlugins(PlayScala)
    settings(common: _*)
    settings(
      generateSql := {
        println("###################taskCompleted")

        val generated = baseDirectory.value / "app" / "controllers" / "Test.scala"
        IO.write(generated, """object Test extends App { println("Hi") }""")
        "Task done"
      },
      //unmanagedResourceDirectories in Assets += (crossTarget in frontend).value,
      //compile in Compile <<= (compile in Compile) dependsOn generateSql,
      compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in (frontend, Compile)),
      scalajsOutputDir := baseDirectory.value / "app" / "assets" / "js",
      //playMonitoredFiles += (scalaSource in (frontend, Compile)).value.getCanonicalPath,
      libraryDependencies ++= Seq(
        "mysql" % "mysql-connector-java" % "5.1.12",
        "org.slf4j" % "slf4j-nop" % "1.6.4",
        "com.h2database" % "h2" % "1.3.175",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "javax.validation" % "validation-api" % "1.0.0.GA",
        Deps.autowireJVM
      )
    )
    settings(
      Seq(packageLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
        crossTarget in (frontend, Compile, packageJSKey) := scalajsOutputDir.value
      }: _*
    )
	  dependsOn(macro)
    dependsOn(frontend)
  )
}