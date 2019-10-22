import mill._, scalalib._, scalajslib._, scalanativelib._, scalalib.publish._

val scalaVersions = Map(
  "2.13" -> "2.13.1",
  "2.12" -> "2.12.10",
  "2.11" -> "2.11.12",
  "2.10" -> "2.10.7"
)

trait YamlesqueModule extends ScalaModule with PublishModule {
  def binVersion: String
  def scalaVersion = scalaVersions(binVersion)
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
}

trait YamlesqueTestModule extends TestModule with ScalaModule {
  def millSourcePath = build.millSourcePath / "yamlesque" / "test"
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.1")
  def testFrameworks = Seq("utest.runner.Framework")
}

class YamlesqueJVMModule(val binVersion: String) extends YamlesqueModule {
  object test extends Tests with YamlesqueTestModule
}
class YamlesqueJSModule(val binVersion: String) extends YamlesqueModule with ScalaJSModule {
  def scalaJSVersion = "0.6.29"
  object test extends Tests with YamlesqueTestModule
}
class YamlesqueNativeModule(val binVersion: String) extends YamlesqueModule with ScalaNativeModule {
  def scalaNativeVersion = "0.3.8"
  def releaseMode = scalanativelib.api.ReleaseMode.Release  
  object test extends Tests with YamlesqueTestModule
}
object yamlesque extends Module {
  object jvm extends Cross[YamlesqueJVMModule]("2.13", "2.12", "2.11", "2.10")
  object js extends Cross[YamlesqueJSModule]("2.13", "2.12", "2.11")
  object native extends Cross[YamlesqueNativeModule]("2.11")
}

object site extends ScalaJSModule {
  def scalaVersion = scalaVersions("2.13")
  def scalaJSVersion = "0.6.29" 
  def moduleDeps = Seq(yamlesque.js("2.13"))
  def ivyDeps = Agg(
    ivy"com.lihaoyi::ujson::0.8.0",
    ivy"org.scala-js::scalajs-dom::0.9.7"
  )
}
