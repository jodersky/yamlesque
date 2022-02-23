import mill._, scalalib._, scalajslib._, scalanativelib._, scalalib.publish._

val scala3 = "3.0.2"
val scala31 = "3.1.1" // temporary, until 3.1.2 is out at which point we'll use the -Yrelease flag to target old versions
val scala213 = "2.13.8"
val scalajs = "1.8.0"
val scalanative = "0.4.3"

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.11")
  def testFramework = "utest.runner.Framework"
}

trait Publish extends PublishModule {
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

object yamlesque extends Module {

  trait YamlesquesModule
      extends CrossScalaModule
      with Publish {
    def scalacOptions = Seq("-feature", "-deprecation", "-release", "8")
    def artifactName = "yamlesque"
    def ivyDeps = Agg(ivy"com.lihaoyi::geny::0.7.1")
  }

  class JvmModule(val crossScalaVersion: String) extends YamlesquesModule {
    def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule](scala213, scala3)

  class JsModule(val crossScalaVersion: String, val crossScalaJSVersion: String)
      extends YamlesquesModule
      with ScalaJSModule {
    def scalaJSVersion = crossScalaJSVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-js")))
    object test extends Tests with Utest
  }
  object js extends Cross[JsModule]((scala213, scalajs), (scala3, scalajs))

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends YamlesquesModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest
  }
  object native extends Cross[NativeModule]((scala213, scalanative), (scala31, scalanative))

}

object site extends ScalaJSModule {
  def scalaVersion = scala3
  def scalaJSVersion = scalajs
  def moduleDeps = Seq(yamlesque.js(scala3, scalajs))
  def ivyDeps = Agg(
    ivy"com.lihaoyi::ujson::1.5.0",
    ivy"org.scala-js::scalajs-dom::2.0.0"
  )
}

object `yamlesque-upickle` extends Module { self =>

  trait Base extends CrossScalaModule with Publish {
    def scalacOptions = Seq("-feature", "-deprecation", "-release", "8")
    def artifactName = "yamlesque-upickle"
    def ivyDeps = Agg(ivy"com.lihaoyi::upickle::1.5.0")
    def millSourcePath = self.millSourcePath
  }

  class JvmModule(val crossScalaVersion: String) extends Base {
    def moduleDeps = Seq(yamlesque.jvm(crossScalaVersion))
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule](scala213, scala3)

  class JsModule(val crossScalaVersion: String, val crossScalaJSVersion: String)
      extends Base
      with ScalaJSModule {
    def moduleDeps = Seq(yamlesque.js(crossScalaVersion, crossScalaJSVersion))
    def scalaJSVersion = crossScalaJSVersion
    object test extends Tests with Utest
  }
  object js extends Cross[JsModule]((scala213, scalajs), (scala3, scalajs))

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends Base
      with ScalaNativeModule {
    def moduleDeps = Seq(yamlesque.native(crossScalaVersion, crossScalaNativeVersion))
    def scalaNativeVersion = crossScalaNativeVersion
    object test extends Tests with Utest
  }
  object native extends Cross[NativeModule]((scala213, scalanative), (scala31, scalanative))

}
