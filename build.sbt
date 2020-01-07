
//val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

val amfVersion = "4.1.0-SNAPSHOT"

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

val settings = Common.settings ++ Seq(
  name := "AMFMeter",
  version := "0.0.1",

  libraryDependencies ++= Seq(
    "com.github.amlorg" %% "amf-client" % amfVersion,
    "com.storm-enroute" %% "scalameter" % "0.19",
    "org.raml" % "raml-parser-2" % "1.0.30",
    "org.raml" % "raml-parser" % "0.9-SNAPSHOT"
  ),
  resolvers ++= List(Common.releases, Common.snapshots, Common.scalaMeter, Resolver.mavenLocal),
  credentials ++= Common.credentials()
)

// scala-meter
testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")
parallelExecution in Test := false

lazy val root = project
  .in(file("."))
  .settings(settings: _*)
  .enablePlugins(BuildInfoPlugin).settings(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, allDependencies),
  buildInfoPackage := "AMFMeter"
)