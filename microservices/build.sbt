name := "microservices-root"
version := "1.0-SNAPSHOT"
scalaVersion := "3.7.3"

// Common monitoring module
lazy val `common-monitoring` = (project in file("common-monitoring"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "io.prometheus" % "simpleclient" % "0.16.0",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_servlet" % "0.16.0",
      "com.orbitz.consul" % "consul-client" % "1.5.3"
    )
  )

// User Service
lazy val `user-service` = (project in file("user-service"))
  .enablePlugins(PlayScala)
  .dependsOn(`common-monitoring`)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.playframework" %% "play-slick" % "6.1.1",
      "org.playframework" %% "play-slick-evolutions" % "6.1.1",
      "org.postgresql" % "postgresql" % "42.7.4",
      "org.playframework" %% "play-json" % "3.0.4",
      "org.playframework" %% "play-ws" % "3.0.4",
      "org.apache.kafka" % "kafka-clients" % "3.6.1",
      "org.mindrot" % "jbcrypt" % "0.4"
    )
  )

// Notification Service
lazy val `notification-service` = (project in file("notification-service"))
  .enablePlugins(PlayScala)
  .dependsOn(`common-monitoring`)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.playframework" %% "play-slick" % "6.1.1",
      "org.playframework" %% "play-slick-evolutions" % "6.1.1",
      "org.postgresql" % "postgresql" % "42.7.4",
      "org.playframework" %% "play-json" % "3.0.4",
      "org.playframework" %% "play-ws" % "3.0.4",
      "org.apache.kafka" % "kafka-clients" % "3.6.1"
    )
  )

// Analytics Service
lazy val `analytics-service` = (project in file("analytics-service"))
  .enablePlugins(PlayScala)
  .dependsOn(`common-monitoring`)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.playframework" %% "play-slick" % "6.1.1",
      "org.playframework" %% "play-slick-evolutions" % "6.1.1",
      "org.postgresql" % "postgresql" % "42.7.4",
      "org.playframework" %% "play-json" % "3.0.4",
      "org.playframework" %% "play-ws" % "3.0.4",
      "org.apache.kafka" % "kafka-clients" % "3.6.1"
    )
  )

// API Gateway
lazy val `api-gateway` = (project in file("api-gateway"))
  .enablePlugins(PlayScala)
  .dependsOn(`common-monitoring`)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.playframework" %% "play-json" % "3.0.4",
      "org.playframework" %% "play-ws" % "3.0.4"
    )
  )

// Root aggregates all projects
lazy val root = (project in file("."))
  .aggregate(`common-monitoring`, `user-service`, `notification-service`, `analytics-service`, `api-gateway`)
  .settings(
    publish / skip := true
  )
