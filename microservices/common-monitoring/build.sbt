name := "common-monitoring"

version := "1.0-SNAPSHOT"

scalaVersion := "3.7.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,

      // Prometheus Metrics
      "io.prometheus" % "simpleclient" % "0.16.0",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_servlet" % "0.16.0",

      // Consul Service Discovery
      "com.orbitz.consul" % "consul-client" % "1.5.3"
    )
  )
