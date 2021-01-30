name := "SDProject_HQ"

version := "1.0"

scalaVersion := "2.12.5"

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.12.5",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.6",
     "org.scala-lang.modules" %% "scala-async" % "0.10.0"
    )
  )