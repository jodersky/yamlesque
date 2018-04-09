organization in ThisBuild := "io.crashbox"
licenses in ThisBuild := Seq(
  ("Apache 2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
homepage in ThisBuild := Some(url("https://github.com/jodersky/yamlesque"))
publishMavenStyle in ThisBuild := true
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/jodersky/yamlesque"),
    "scm:git@github.com:jodersky/yamlesque.git"
  )
)
developers := List(
  Developer(
    id = "jodersky",
    name = "Jakob Odersky",
    email = "jakob@odersky.com",
    url = url("https://crashbox.io")
  )
)
