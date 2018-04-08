# yamlesque

Pure Scala YAML parsing.

As the name suggests, "yam-el-esque" is a Scala implementation of the
most frequently used YAML features. It takes inspiration from
Spray-JSON and aims to provide an idiomatic, to-the-point API that is
cross-platform and has a minimal set of dependencies.

It does not strictly implement all features as defined in [YAML
1.2](http://yaml.org/spec/1.2/spec.html), however support should be
sufficient for regular use. Pull requests with additional feature
implementations are always welcome!

## Getting Started
Include yamlesque into a project. In sbt, this can be done with:

```
libraryDependencies += "io.crashbox" %% "yamlesque" % "<latest_tag>"
```

### Parse some YAML
```
import yamlesque._

val text = s"""|name: yamlesque
               |description: a YAML library for scala
               |authors:
               |  - name: Jakob Odersky
               |    id: jodersky
               |  - name: Another
               |""".stripMargin

// parse yaml to a typesafe representation
val yaml = text.parseYaml
```
