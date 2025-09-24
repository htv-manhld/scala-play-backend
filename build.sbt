name := """scala-play-backend"""
organization := "com.demo"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, FlywayPlugin)
  .settings(
    name := "scala-play-backend",
    scalaVersion := "3.7.3",
    javacOptions ++= Seq("-source", "21", "-target", "21"),
    // Flyway configuration
    flywayUrl := "jdbc:postgresql://localhost:5432/playdb",
    flywayUser := "postgres",
    flywayPassword := "password",
    flywayLocations := Seq("filesystem:src/main/resources/db/migration"),
    flywayBaselineOnMigrate := true,

    libraryDependencies ++= Seq(
      guice,
      // jdbc, // conflicts with slick evolutions
      evolutions,
      "org.postgresql" % "postgresql" % "42.7.4",
      "org.playframework" %% "play-slick" % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.flywaydb" % "flyway-core" % "10.10.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
    ),
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3"
    )
  )
