name := "user-service"

version := "1.0-SNAPSHOT"

scalaVersion := "3.7.3"

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

      // JSON
      "org.playframework" %% "play-json" % "3.0.4",

      // HTTP Client
      "org.playframework" %% "play-ws" % "3.0.4",

      // Kafka
      "org.apache.kafka" % "kafka-clients" % "3.6.1",

      // Password Hashing
      "org.mindrot" % "jbcrypt" % "0.4"
    )
  )