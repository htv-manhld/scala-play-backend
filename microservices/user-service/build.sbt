name := "user-service"

version := "1.0-SNAPSHOT"

scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,

      // Database
      "org.playframework" %% "play-slick" % "6.1.1",
      "org.playframework" %% "play-slick-evolutions" % "6.1.1",
      "org.postgresql" % "postgresql" % "42.7.4",

      // Message Queue
      "org.apache.kafka" % "kafka-clients" % "3.5.1",

      // JSON
      "org.playframework" %% "play-json" % "3.0.4",

      // HTTP Client
      "org.playframework" %% "play-ws" % "3.0.4",

      // Metrics & Monitoring
      "io.micrometer" % "micrometer-core" % "1.11.5",
      "io.micrometer" % "micrometer-registry-prometheus" % "1.11.5"
    )
  )

// Docker configuration
enablePlugins(DockerPlugin)
dockerBaseImage := "eclipse-temurin:21-jre-alpine"
dockerExposedPorts := Seq(9001)