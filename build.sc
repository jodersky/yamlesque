import mill._, scalalib._, scalajslib._, scalanativelib._, scalalib.publish._

val scala3 = "3.3.3"
val scala213 = "2.13.14"
val scalajs = "1.16.0"
val scalanative = "0.5.1"

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.8.3")
  def testFramework = "utest.runner.Framework"
  def docJar = PathRef(os.pwd / "README.md") // TODO: regular docJar fails under scala 3
}

trait Publish extends PublishModule {
  def publishVersion = T.input{os.proc("git", "describe", "--dirty").call().out.trim()}
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

  trait YamlesquesModule extends ScalaModule with Publish {
    def scalacOptions = Seq("-feature", "-deprecation", "-release", "8")
    def artifactName = "yamlesque"
    def ivyDeps = Agg(ivy"com.lihaoyi::geny::1.1.0")
    def millSourcePath = super.millSourcePath / os.up
  }

  trait JvmModule extends Cross.Module[String] with YamlesquesModule {
    val scalaVersion = crossValue
    object test extends ScalaTests with Utest
  }
  object jvm extends Cross[JvmModule](Seq(scala213, scala3))

  trait JsModule extends Cross.Module2[String, String] with YamlesquesModule with ScalaJSModule {
    val (scalaVersion, scalaJSVersion) = (crossValue, crossValue2)
    object test extends ScalaJSTests with Utest
  }
  object js extends Cross[JsModule]((scala213, scalajs), (scala3, scalajs))

  trait NativeModule extends Cross.Module2[String, String] with YamlesquesModule with ScalaNativeModule {
    val (scalaVersion, scalaNativeVersion) = (crossValue, crossValue2)
    object test extends ScalaNativeTests with Utest
  }
  object native extends Cross[NativeModule](Seq((scala213, scalanative), (scala3, scalanative)))

}

object site extends ScalaJSModule {
  def scalaVersion = scala3
  def scalaJSVersion = scalajs
  def moduleDeps = Seq(yamlesque.js(scala3, scalajs))
  def ivyDeps = Agg(
    ivy"com.lihaoyi::ujson::3.3.0",
    ivy"org.scala-js::scalajs-dom::2.8.0"
  )
}

// object `yamlesque-upickle` extends Module { self =>

//   trait Base extends ScalaModule with Publish {
//     def scalacOptions = Seq("-feature", "-deprecation", "-release", "8")
//     def artifactName = "yamlesque-upickle"
//     def ivyDeps = Agg(ivy"com.lihaoyi::upickle::3.3.0")
//     def millSourcePath = self.millSourcePath
//   }

//   trait JvmModule extends Cross.Module[String] with Base {
//     val scalaVersion = crossValue
//     def moduleDeps = Seq(yamlesque.jvm(crossValue))
//     object test extends ScalaTests with Utest
//   }
//   object jvm extends Cross[JvmModule](Seq(scala213, scala3))

//   trait JsModule extends Cross.Module2[String, String] with Base with ScalaJSModule {
//     val (scalaVersion, scalaJSVersion) = (crossValue, crossValue2)
//     def moduleDeps = Seq(yamlesque.js(crossValue, crossValue2))
//     object test extends ScalaJSTests with Utest
//   }
//   object js extends Cross[JsModule](Seq((scala213, scalajs), (scala3, scalajs)))

//   trait NativeModule extends Cross.Module2[String, String] with Base with ScalaNativeModule {
//     val (scalaVersion, scalaNativeVersion) = (crossValue, crossValue2)
//     def moduleDeps = Seq(yamlesque.native(crossValue, crossValue2))
//     object test extends ScalaNativeTests with Utest
//     // def docJar = PathRef(os.pwd / "README.md") // TODO: regular docJar fails under scala 3.1.1
//   }
//   object native extends Cross[NativeModule](Seq((scala213, scalanative), (scala3, scalanative)))

// }
