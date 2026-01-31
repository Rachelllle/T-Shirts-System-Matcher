ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.19"

lazy val root = (project in file("."))
  .settings(
    name := "scala",
    mainClass in (Compile, run) := Some("CBIRTShirtScala"),

    libraryDependencies ++= Seq(
      // Spark 3.5.1 (Scala 2.12)
      "org.apache.spark" %% "spark-core" % "3.5.1",
      "org.apache.spark" %% "spark-sql"  % "3.5.1",
      "org.apache.spark" %% "spark-mllib"     % "3.5.1"
    ),
    fork := true,
    javaOptions += "-Djava.security.manager=allow"
  )
