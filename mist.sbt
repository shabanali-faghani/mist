import sbt.Keys._
import StageDist._
import complete.DefaultParsers._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.AssemblyOption

ThisBuild / scalaVersion := "2.13.16"

// Fix insecure resolvers
ThisBuild / useCoursier := true // Optional - helps with some resolver issues

// Replace with safe resolvers
ThisBuild / resolvers := Seq(
  "Maven Central" at "https://repo1.maven.org/maven2/",
  "Typesafe Releases" at "https://repo.typesafe.org/typesafe/releases/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % "early-semver"

dependencyOverrides +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0"

resolvers ++= Resolver.sonatypeOssRepos("releases")
//resolvers += "Typesafe Releases" at "https://repo.typesafe.org/typesafe/releases"

lazy val sparkVersion: SettingKey[String] = settingKey[String]("Spark version")
lazy val scalaPostfix: SettingKey[String] = settingKey[String]("Scala version postfix")
lazy val sparkLocal: TaskKey[File] = taskKey[File]("Download spark distr")
lazy val mistRun: InputKey[Unit] = inputKey[Unit]("Run mist locally")

lazy val versionRegex = "(\\d+)\\.(\\d+).*".r

lazy val commonSettings = Seq(
  organization := "io.hydrosphere",

//  sparkVersion := sys.props.getOrElse("sparkVersion", "2.4.0"),
//  scalaVersion :=  sys.props.getOrElse("scalaVersion", "2.11.12"),

//  sparkVersion := sys.props.getOrElse("sparkVersion", "2.4.0"),
//  scalaVersion :=  sys.props.getOrElse("scalaVersion", "2.12.18"),

  sparkVersion := sys.props.getOrElse("sparkVersion", "4.0.1"),
  scalaVersion :=  sys.props.getOrElse("scalaVersion", "2.13.16"),
  scalaPostfix := { if (scalaBinaryVersion.value == "2.12") "-scala-2.12" else "" },
  crossScalaVersions := Seq("2.13.16"),
//  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  javacOptions ++= Seq("--release", "17"),
  Test / parallelExecution := false,
  version := "1.1.3"
)

lazy val mistLib = project.in(file("mist-lib"))
  .settings(commonSettings: _*)
  .settings(PublishSettings.settings: _*)
  .settings(PyProject.settings: _*)
  .settings(
    scalacOptions ++= commonScalacOptions,
    name := "mist-lib",
    Compile / sourceGenerators += (Compile / sourceManaged).map(dir => Boilerplate.gen(dir)).taskValue,
    Compile / unmanagedSourceDirectories += {
      val sparkV = sparkVersion.value
      val sparkSpecific =  if (sparkV == "2.4.0") "spark-2.4.0" else "spark"
      baseDirectory.value / "src" / "main" / sparkSpecific
    },
    libraryDependencies ++= Library.spark(sparkVersion.value).map(_ % "provided"),
    libraryDependencies ++= Seq(
//      "io.hydrosphere" %% "shadedshapeless" % "2.3.3",
      "com.chuusai" %% "shapeless" % "2.3.12",
      Library.slf4j % "test",
//      Library.slf4jLog4j % "test",
      Library.scalaTest % "test"
    ),
    PyProject.pyName := "mistpy",
    Test / parallelExecution := false,
    Test / test := Def.sequential(Test / test, Test / PyProject.pyTest).value
  )

lazy val core = project.in(file("mist/core"))
  .dependsOn(mistLib)
  .settings(commonSettings: _*)
  .settings(
    name := "mist-core",
    scalacOptions ++= commonScalacOptions,
    libraryDependencies ++= Library.spark(sparkVersion.value).map(_ % "runtime"),
    libraryDependencies ++= Seq(
      Library.Akka.actor,
      Library.slf4j,
      Library.Akka.testKit % "test",
      Library.mockito % "test", Library.scalaTest % "test"
    )
  )

lazy val master = project.in(file("mist/master"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(commonAssemblySettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "mist-master",
    scalacOptions ++= commonScalacOptions,
//    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
//    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2"),  // or 0.10.3 for older compatibility
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
      libraryDependencies ++= Library.Akka.base,
    libraryDependencies ++= Seq(
//      Library.slf4jLog4j, Library.typesafeConfig, Library.scopt,
      Library.h2, Library.flyway,
      Library.chill,
      Library.kafka, Library.pahoMqtt,

      Library.doobieCore, Library.doobieH2, Library.doobieHikari,
      Library.doobiePostgres, Library.doobieSpecs2,

      Library.Akka.testKit % "test",
      Library.Akka.http, Library.Akka.httpSprayJson, Library.Akka.httpTestKit % "test",
      //Library.cats,

      Library.dockerJava,

//      "io.hydrosphere" %% "shadedshapeless" % "2.3.3",
      "com.chuusai" %% "shapeless" % "2.3.12",
      Library.commonsCodec, Library.scalajHttp,
      Library.jsr305 % "provided",

      Library.scalaTest % "test",
      Library.mockito % "test"
    )
  ).settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sparkVersion),
    buildInfoPackage := "io.hydrosphere.mist",
    libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.14"
  )

lazy val worker = project.in(file("mist/worker"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(commonAssemblySettings: _*)
  .settings(
    name := "mist-worker",
    scalacOptions ++= commonScalacOptions,
    Compile / resourceGenerators += {
      Def.task {
        val resourceDir = (Compile / resourceManaged).value
        val f = (mistLib / PyProject.pySources).value
        val baseOut = resourceDir / "mistpy"
        f.listFiles().toSeq.map(r => {
          val target = baseOut / r.name
          IO.write(target, IO.read(r))
          target
        })
      }
    },
    libraryDependencies ++= Library.Akka.base :+ Library.Akka.http,
    libraryDependencies += Library.Akka.testKit,
    libraryDependencies ++= Library.spark(sparkVersion.value).map(_ % "provided"),
    libraryDependencies ++= Seq(
      Library.scopt,
      Library.scalaTest % "test"
    ),
//    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false),
    assembly / assemblyShadeRules := Seq(
       ShadeRule.rename("scopt.**" -> "shaded.@0").inAll
    )
  )

lazy val root = project.in(file("."))
  .aggregate(mistLib, core, master, worker, examples)
  .dependsOn(master)
  .enablePlugins(DockerPlugin)
  .settings(commonSettings: _*)
  .settings(Ui.settings: _*)
  .settings(StageDist.settings: _*)
  .settings(
    name := "mist",
    stageDirectory := target.value / (s"mist-${version.value}${scalaPostfix.value}"),
    stageActions := {
      Seq(
        CpFile("bin"),
        MkDir("configs"),
        CpFile("configs/default.conf").to("configs"),
        CpFile("configs/logging").to("configs"),
        CpFile(assembly.in(master, assembly).value).as("mist-master.jar"),
        CpFile(assembly.in(worker, assembly).value).as("mist-worker.jar"),
        CpFile(Ui.ui.value).as("ui")
      )
    },
    basicStage / stageActions += {
      val name = imageNames.in(docker).value.head.toString()
      val configData =
        IO.read(file("configs/default.conf"))
          .replaceAll("\\$\\{imageName\\}", name)
      Write("configs/default.conf", configData)
    },
    dockerStage / stageDirectory := target.value / s"mist-docker-${version.value}",
    dockerStage / stageActions += {
      val name = imageNames.in(docker).value.head.toString()
      val configData =
        IO.read(file("configs/docker.conf"))
          .replaceAll("\\$\\{imageName\\}", name)
      Write("configs/default.conf", configData)
    },

    runStage / stageDirectory := target.value / s"mist-run-${version.value}${scalaPostfix.value}",
    runStage / stageActions ++= {
      val mkJfunctions = Seq(
        ("spark-ctx-example", "SparkContextExample$"),
        ("jspark-ctx-example", "JavaSparkContextExample"),
        ("streaming-ctx-example", "StreamingExample$"),
        ("jstreaming-ctx-example", "JavaStreamingContextExample"),
        ("hive-ctx-example", "HiveContextExample$"),
        ("sql-ctx-example", "SQLContextExample$"),
        ("text-search-example", "TextSearchExample$"),
        ("less-verbose-example", "LessVerboseExample$"),
        ("pi-example", "PiExample$"),
        ("jpi-example", "JavaPiExample")
      ).map({case (name, clazz) => {
        Write(
          s"data/functions/$name.conf",
          s"""path = mist-examples.jar
             |className = "$clazz"
             |namespace = foo""".stripMargin
        )
      }}) :+ CpFile(sbt.Keys.`package`.in(examples, Compile).value)
        .as("mist-examples.jar")
        .to("data/artifacts")

      val mkPyfunctions = Seq(
        "session_example",
        "sparkctx_example",
        "sqlctx_example",
        "streamingctx_example"
      ).map(fn => {
        Write(
          s"data/functions/${fn}_py.conf",
          s"""path = mist_pyexamples.egg
             |className = "mist_examples.${fn}"
             |namespace = foo""".stripMargin
        )
      }) :+ CpFile(PyProject.pyBdistEgg.in(examples).value)
          .as("mist_pyexamples.egg")
          .to("data/artifacts")

      Seq(MkDir("data/artifacts"), MkDir("data/functions")) ++ mkJfunctions ++ mkPyfunctions
    }
  ).settings(
    sparkLocal := {
      val log = streams.value.log
      val sparkV= sparkVersion.value
      val scalaBin = scalaBinaryVersion.value

      val local = file("spark_local")
      if (!local.exists())
        IO.createDirectory(local)

      val sparkDir = local / SparkLocal.distrName(sparkV, scalaBin)
      if (!sparkDir.exists()) {
        log.info(s"Downloading spark $version to $sparkDir")
        SparkLocal.downloadSpark(sparkV, scalaBin, local)
      }
      sparkDir
    },

    mistRun := {
      val log = streams.value.log
      val taskArgs = spaceDelimited("<arg>").parsed.grouped(2).toSeq
        .flatMap(l => {if (l.size == 2) Some(l.head -> l.last) else None})
        .toMap

      val uiEnvs = taskArgs.get("--ui-dir").fold(Seq.empty[(String, String)])(p => Seq("MIST_UI_DIR" -> p))
      val sparkEnvs = {
        val spark = taskArgs.getOrElse("--spark", sparkLocal.value.getAbsolutePath)
        Seq("SPARK_HOME" -> spark)
      }

      val extraEnv = sparkEnvs ++ uiEnvs
      val home = runStage.value

      val args = Seq("bin/mist-master", "start", "--debug", "true")

      import scala.sys.process._

      val ps = Process(args, Some(home), extraEnv: _*)
      log.info(s"Running mist $ps with env $extraEnv")

      ps.!<(StdOutLogger)
    }
  ).settings(
    docker / imageNames := {
      Seq(ImageName(s"hydrosphere/mist:${version.value}-${sparkVersion.value}${scalaPostfix.value}"))
    },
    docker / dockerfile := {
      val localSpark = sparkLocal.value
      val mistHome = "/usr/share/mist"
      val distr = dockerStage.value

      new Dockerfile {
        from("anapsix/alpine-java:8")

        expose(2004)

        workDir(mistHome)

        env("SPARK_VERSION", sparkVersion.value)
        env("SPARK_HOME", "/usr/share/spark")
        env("MIST_HOME", mistHome)

        entryPoint("/docker-entrypoint.sh")

        run("apk", "update")
        run("apk", "add", "python", "curl", "jq", "coreutils", "subversion-dev", "fts-dev")

        copy(localSpark, "/usr/share/spark")

        copy(distr, mistHome)

        copy(file("docker-entrypoint.sh"), "/")
        run("chmod", "+x", "/docker-entrypoint.sh")
      }
    })
  .settings(
    libraryDependencies ++= Library.spark(sparkVersion.value).map(_ % "provided"),
  )

addCommandAlias("testAll", ";test;it:test")

lazy val examples = project.in(file("examples/examples"))
  .dependsOn(mistLib)
  .settings(commonSettings: _*)
  .settings(PyProject.settings:_*)
  .settings(
    name := "mist-examples",
    libraryDependencies ++= Library.spark(sparkVersion.value).map(_ % "provided"),
    libraryDependencies ++= Seq(
      Library.scalaTest % "test",
      Library.junit % "test",
      "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep")
    ),
    PyProject.pyName := "mist_examples"
  )

libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"

lazy val commonAssemblySettings = Seq(
  assembly / assemblyMergeStrategy := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case PathList("META-INF", xs @ _*) =>
      (xs map {_.toLowerCase}) match {
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.discard
      }
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case PathList("org", "apache", xs@_*) => MergeStrategy.first
    case PathList("org", "jboss", xs@_*) => MergeStrategy.first
    case "about.html" => MergeStrategy.rename
    case "reference.conf" => MergeStrategy.concat
    case PathList("org", "datanucleus", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  assembly / logLevel := Level.Error,
  assembly / test := {}
)

lazy val commonScalacOptions = Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
//  "-Ypartial-unification",
  "-Wconf:msg=legacy-binding:s",
  "-deprecation"
)


import sbtassembly.AssemblyPlugin.autoImport._

assembly / assemblyMergeStrategy := {
  case PathList("javax", "inject", _*) =>
    MergeStrategy.first

  case PathList("org", "apache", "commons", "logging", _*) =>
    MergeStrategy.first

  case PathList("org", "apache", "http", _*) =>
    MergeStrategy.first

  case PathList("org", "newsclub", "net", "unix", _*) =>
    MergeStrategy.first

  case PathList("META-INF", xs @ _*) =>
    xs match {
      case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
      case _                      => MergeStrategy.first
    }

  case _ =>
    MergeStrategy.first
}

assembly / assemblyMergeStrategy := {
  case PathList("javax", "jdo", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
  case PathList("com", "google", "thirdparty", xs @ _*) => MergeStrategy.first
  case PathList("META-INF", "versions", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList("plugin.xml") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}