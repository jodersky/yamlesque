# yamlesque

Pure Scala YAML parsing.

As the name suggests, "yam-el-esque" is a Scala implementation of the most
frequently used YAML features. It takes inspiration from [Haoyi Li's
ujson](http://www.lihaoyi.com/post/uJsonfastflexibleandintuitiveJSONforScala.html)
and aims to provide an idiomatic API that is cross-platform and has no
dependencies.

## Getting Started

Include yamlesque into a project.

- mill:

  ```scala
  def ivyDeps = Agg(ivy"io.crashbox::yamlesque::<latest_tag>")
  ```

- sbt:

  ```scala
  libraryDependencies += "io.crashbox" %%% "yamlesque" % "<latest_tag>"
  ```

**Yamlesque is available for Scala 2.13, 2.12 and 2.11, including ScalaJS and
Native.**

It should also work with Scala 2.10 and 2.9, although no pre-compiled libraries
are published for these versions.

### Read Some YAML

```scala
val text = s"""|name: yamlesque
               |description: a YAML library for scala
               |authors:
               |  - name: Jakob Odersky
               |    id: jodersky
               |  - name: Another
               |""".stripMargin

val yaml: yamlesque.Node = yamlesque.read(text)

val id = yaml.obj("authors").arr(0).obj("id").str

println(id) // == "jodersky"
```

### Write Some YAML

```scala
import yamlesque.{Arr, Num, Obj, Str}
val config = Obj(
  "auth" -> Obj(
    "username" -> Str("admin"),
    "password" -> Str("guest")
  ),
  "interfaces" -> Arr(
    Obj(
      "address" -> Str("0.0.0.0"),
      "port" -> Num(80)
    ),
    Obj(
      "address" -> Str("0.0.0.0"),
      "port" -> Num(443)
    )
  )
)

val stringly = yamlesque.write(config)

println(stringly)
```

will result in

```yaml
auth:
  username: admin
  password: guest
interfaces:
  - address: 0.0.0.0
    port: 80.0
  - address: 0.0.0.0
    port: 443.0
```

## Official YAML Conformance

Yamlesque does not strictly implement all features as defined in [YAML
1.2](http://yaml.org/spec/1.2/spec.html), however support should be
sufficient for most regular documents.

Available features:

- plain strings (i.e. scalars), including specialization to numbers, booleans
  and null
- lists and maps
- quoted strings
- comments
- multiple documents (i.e. ---)

Features which are currently not supported but for which support is planned:

- verbatim blocks (i.e. | and >) (support is limited currently)
- flow-styles (aka inline JSON)

Unsupported features with no planned implementation:

- anchors and references
- tags

Pull requests with additional feature implementations are always welcome!
