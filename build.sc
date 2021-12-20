import mill._, scalalib._, scalajslib._, scalanativelib._, scalalib.publish._


class YamlesquesModule(crossVersion: String) extends ScalaModule with PublishModule {
  def scalaVersion = crossVersion
  def millSourcePath = build.millSourcePath / "yamlesque"
  def scalacOptions = Seq("-feature", "-deprecation")

  def publishVersion = T.input{os.proc("git", "describe", "--dirty", "--match=v*").call().out.trim.tail}
  def pomSettings = PomSettings(
    description = "Simple YAML parsing.",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/yamlesque",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("jodersky", "yamlesque"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky","https://github.com/jodersky")
    )
  )
  def artifactName = "yamlesque"

  object test extends Tests with ScalaModule {
    def millSourcePath = build.millSourcePath / "yamlesque" / "test"
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.10")
    def testFramework = "utest.runner.Framework"
  }
}
object yamlesque extends Cross[YamlesquesModule]("3.0.2", "2.13.7")

// object site extends ScalaJSModule {
//   def scalaVersion = scalaVersions("2.13")
//   def scalaJSVersion = "0.6.29"
//   def moduleDeps = Seq(yamlesque.js("2.13"))
//   def ivyDeps = Agg(
//     ivy"com.lihaoyi::ujson::0.8.0",
//     ivy"org.scala-js::scalajs-dom::0.9.7"
//   )
// }
