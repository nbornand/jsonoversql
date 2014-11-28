name := "JSON over SQL"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
	"mysql" % "mysql-connector-java" % "5.1.12",
  //"org.json4s" % "json4s-native" % "3.2.10",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.h2database" % "h2" % "1.3.175",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "javax.validation" % "validation-api" % "1.0.0.GA"
)

lazy val root = project.in( file(".") ).enablePlugins(PlayScala)