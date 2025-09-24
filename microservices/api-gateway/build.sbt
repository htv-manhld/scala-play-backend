name := "api-gateway"

version := "1.0-SNAPSHOT"

scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,

      // HTTP Client for service communication
      "org.playframework" %% "play-ws" % "3.0.4",

      // JWT for authentication
      "com.github.jwt-scala" %% "jwt-play-json" % "10.0.1",

      // Rate limiting
      "com.github.blemale" %% "scaffeine" % "5.2.1",

      // Circuit breaker
      "com.lightbend.akka" %% "akka-stream-alpakka-simple-codecs" % "8.0.0",

      // Service discovery
      "com.typesafe.akka" %% "akka-discovery" % "2.8.8",

      // JSON
      "org.playframework" %% "play-json" % "3.0.4",

      // Metrics
      "io.micrometer" % "micrometer-core" % "1.11.5",
      "io.micrometer" % "micrometer-registry-prometheus" % "1.11.5"
    )
  )

// Docker configuration
enablePlugins(DockerPlugin)
dockerBaseImage := "eclipse-temurin:21-jre-alpine"
dockerExposedPorts := Seq(9000)