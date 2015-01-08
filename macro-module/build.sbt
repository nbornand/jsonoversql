name := "Macro"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.12",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "javax.validation" % "validation-api" % "1.0.0.GA"
)