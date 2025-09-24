name := "notification-service"

version := "1.0-SNAPSHOT"

scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,

      // Message Queue
      "org.apache.kafka" % "kafka-clients" % "3.5.1",

      // Email
      "com.sun.mail" % "javax.mail" % "1.6.2",

      // JSON
      "org.playframework" %% "play-json" % "3.0.4",

      // HTTP Client
      "org.playframework" %% "play-ws" % "3.0.4",

      // Redis for caching
      "com.github.rediscala" %% "rediscala" % "1.9.0",

      // Metrics & Monitoring
      "io.micrometer" % "micrometer-core" % "1.11.5",
      "io.micrometer" % "micrometer-registry-prometheus" % "1.11.5"
    )
  )

// Docker configuration
enablePlugins(DockerPlugin)
dockerBaseImage := "eclipse-temurin:21-jre-alpine"
dockerExposedPorts := Seq(9002)