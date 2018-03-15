import sbt.Keys.{scalacOptions, _}
import sbt.{Def, _}

object Common {

  val organizationName = "org.mulesoft"

  private val nexus = "https://repository-master.mulesoft.org/nexus/content/repositories"

  val releasesPublic: MavenRepository = "MuleSoft private releases" at s"$nexus/releases"
  //  val releasesPrivate: MavenRepository  = "MuleSoft private releases" at s"$nexus/ci-releases"
  val snapshots: MavenRepository = "MuleSoft private snapshots" at s"$nexus/ci-snapshots"
  val external: MavenRepository =  "MuleSoft external snapshots" at "https://repository.mulesoft.org/nexus/content/repositories/snapshots"
  val scalaMeter: MavenRepository =  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := organizationName,
    scalaVersion := "2.12.2",
    parallelExecution in Test := false,
    fork in Test := false,
    scalacOptions ++= Seq("-unchecked"/*, "-deprecation", "-Xfatal-warnings" */),
    scalacOptions ++= Seq("-encoding", "utf-8")
  )

  val publish: Def.Setting[_] = publishTo := Some(if (isSnapshot.value) snapshots else releasesPublic)

  def credentials(): Seq[Credentials] = {
    val cs =
      Seq("mule_user"         -> "mule_password",
        "PUBLIC_NEXUS_USER" -> "PUBLIC_NEXUS_PASS",
        "NEXUS_USER"        -> "NEXUS_PASSWORD",
        "NEXUS_USR"         -> "NEXUS_PSW")
        .flatMap({
          case (user, password) =>
            for {
              u <- sys.env.get(user)
              p <- sys.env.get(password)
            } yield u -> p
        })

    if (cs.nonEmpty) {
      println("Using System Custom credentials ")
      cs.flatMap({
        case (user, password) =>
          Seq(
            Credentials("Sonatype Nexus Repository Manager", "repository-master.mulesoft.org", user, password),
            Credentials("Sonatype Nexus Repository Manager", "repository.mulesoft.org", user, password)
          )
      })

    } else {

      val ivyCredentials   = Path.userHome / ".ivy2" / ".credentials"
      val mavenCredentials = Path.userHome / ".m2" / "settings.xml"

      val servers = Map(("mule-ee-releases", "repository-master.mulesoft.org"),
        ("mule-ee-customer-releases", "repository.mulesoft.org"))

      def loadMavenCredentials(file: java.io.File): Seq[Credentials] = {
        xml.XML.loadFile(file) \ "servers" \ "server" flatMap (s => {
          val id = (s \ "id").text
          if (servers.contains(id)) {
            Some(
              Credentials("Sonatype Nexus Repository Manager",
                servers(id),
                (s \ "username").text,
                (s \ "password").text))
          } else {
            None
          }
        })
      }

      val credentials: Seq[Credentials] =
        (ivyCredentials.asFile, mavenCredentials.asFile) match {
          case (ivy, _) if ivy.canRead => Credentials(ivy) :: Nil
          case (_, mvn) if mvn.canRead => loadMavenCredentials(mvn)
          case _                       => Nil
        }

      credentials
    }
  }
}
