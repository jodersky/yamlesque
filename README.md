[![Build Status](https://travis-ci.org/jodersky/yamlesque.svg?branch=master)](https://travis-ci.org/jodersky/yamlesque)

# yamlesque

Pure Scala YAML parsing.

As the name suggests, "yam-el-esque" is a Scala implementation of the
most frequently used YAML features. It takes inspiration from
Spray-JSON and aims to provide an idiomatic API that is cross-platform
and has a minimal set of dependencies.

## Getting Started
Include yamlesque into a project. In sbt, this can be done with:

```scala
libraryDependencies += "io.crashbox" %% "yamlesque" % "<latest_tag>"
```

### Parse some YAML
```scala
import yamlesque._

val text = s"""|name: yamlesque
               |description: a YAML library for scala
               |authors:
               |  - name: Jakob Odersky
               |    id: jodersky
               |  - name: Another
               |""".stripMargin

// parse yaml to a type safe representation
val yaml = text.parseYaml
```

### Integration with Spray-JSON
*TODO*

### Integration with Akka-HTTP
*TODO*

## YAML Conformance

Yamlesque does not strictly implement all features as defined in [YAML
1.2](http://yaml.org/spec/1.2/spec.html), however support should be
sufficient for most regular documents. Pull requests with additional
feature implementations are always welcome!

The current feature restrictions are:

- always assumes utf-8 is used
- anchors and references are not supported
- tags are not supported
- flow-styles (aka inline JSON) aren't supported
- only single-line literals are allowed (no > or | blocks)
