import sbt._

object Library {

  val slf4j = "org.slf4j" % "slf4j-api" % "2.0.13"
  val logbackClassic =  "ch.qos.logback" % "logback-classic" % "1.4.14"
  val scopt = "com.github.scopt" %% "scopt" % "4.1.0"
  val typesafeConfig = "com.typesafe" % "config" % "1.3.1"

  val chill = "com.twitter" %% "chill" % "0.10.0"

  val h2 = "com.h2database" % "h2" % "2.2.224"
  val flyway = "org.flywaydb" % "flyway-core" % "9.22.3"

  val doobieVersion = "1.0.0-RC1"
  val doobieCore     = "org.tpolecat" %% "doobie-core"     % doobieVersion
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  val doobieH2       = "org.tpolecat" %% "doobie-h2"       % doobieVersion
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari"   % doobieVersion
  val doobieSpecs2   = "org.tpolecat" %% "doobie-specs2"   % doobieVersion

  val pahoMqtt = "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0"
  val kafka = "org.apache.kafka" %% "kafka" % "3.4.0" exclude("log4j", "log4j") exclude("org.slf4j","slf4j-log4j12")

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  val junit = "junit" % "junit" % "4.12"
  val mockito = "org.mockito" %% "mockito-scala" % "1.17.31"

  val dockerJava = "com.github.docker-java" % "docker-java" % "3.0.12"

  object Akka {
    val akkaVersion = "2.8.5"
    val httpVersion = "10.5.3"

    val stream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
    val http = "com.typesafe.akka" %% "akka-http" % "10.2.10"
    val httpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.10"
    val httpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % "10.2.10"

    val testKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
    val actor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
    val typed = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    def base = {
      Seq(
        actor,
        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
      )
    }
  }

  val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  val scalajHttp = "org.scalaj" %% "scalaj-http" % "2.4.2"

  def spark(v: String) = Seq(
    "org.apache.spark" %% "spark-core" % v,
    "org.apache.spark" %% "spark-sql" % v,
    "org.apache.spark" %% "spark-streaming" % v,
  )

  val jsr305 = "com.google.code.findbugs" % "jsr305" % "1.3.9"

}
