import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "akka-stream-complex-components",
    libraryDependencies ++=
      Seq(
        // circe
        "io.circe" %% "circe-core" % Versions.circe,
        "io.circe" %% "circe-generic" % Versions.circe,
        "io.circe" %% "circe-parser" % Versions.circe,
        "org.scalatest" %% "scalatest" % Versions.scalaTest % Test
      ) ++
        Seq(
          // akka streams
          "com.typesafe.akka" %% "akka-stream" % Versions.akka,
          "com.lightbend.akka" %% "akka-stream-alpakka-xml" % Versions.alpakka,
          "com.lightbend.akka" %% "akka-stream-alpakka-file" % Versions.alpakka,
          "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test,
          "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test
        ).map(_.withCrossVersion(CrossVersion.for3Use2_13))
  )