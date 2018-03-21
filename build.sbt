//val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

val amfVersion = "1.2.0"

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

val settings = Common.settings ++ Seq(
  name := "AMFMeter",
  version := "0.0.1",

  libraryDependencies ++= Seq(
    "org.mule.amf" %% "amf-client" % amfVersion,
    "com.storm-enroute" %% "scalameter" % "0.8.2"
  ),
  resolvers ++= List(Common.releasesPublic, Common.snapshots, Common.external, Common.scalaMeter, Resolver.mavenLocal),
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