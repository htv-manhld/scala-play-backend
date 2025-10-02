name := "api-gateway"

version := "1.0-SNAPSHOT"

scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,

      // HTTP Client for service communication
      "org.playframework" %% "play-ws" % "3.0.4",
      "org.playframework" %% "play-ahc-ws" % "3.0.4",

      // JSON
      "org.playframework" %% "play-json" % "3.0.4"
    )
  )