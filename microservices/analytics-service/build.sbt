name := "analytics-service"

version := "1.0-SNAPSHOT"

scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
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
      // Monitoring
      "io.prometheus" % "simpleclient" % "0.16.0",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_servlet" % "0.16.0",
      "com.orbitz.consul" % "consul-client" % "1.5.3"
    )
  )
