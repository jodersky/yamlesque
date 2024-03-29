# yamlesque

[![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://crashbox.zulipchat.com/#narrow/stream/343723-yamlesque)
[![yamlesque Scala version support](https://index.scala-lang.org/jodersky/yamlesque/yamlesque/latest.svg)](https://index.scala-lang.org/jodersky/yamlesque/yamlesque)
[![stability: soft](https://img.shields.io/badge/stability-soft-white)](https://www.crashbox.io/stability.html)

Pure Scala YAML parsing.

As the name suggests, "yaml-esque" is a Scala implementation of the most
frequently used YAML features. It takes inspiration from [Li Haoyi's ujson](http://www.lihaoyi.com/post/uJsonfastflexibleandintuitiveJSONforScala.html)
library and aims to provide an idiomatic API that is cross-platform and has no
dependencies.

## Getting Started

Include yamlesque into a project.

- mill:

  ```scala
  ivy"io.crashbox::yamlesque::<latest_tag>"
  ```

- sbt:

  ```scala
  "io.crashbox" %%% "yamlesque" % "<latest_tag>"
  ```

**Yamlesque is available for Scala 3 and 2.13, including ScalaJS and
Native.**

It should also work with Scala 2.12, 2.11, 2.10 and 2.9, although no
pre-compiled libraries are published for these versions.

### :point_right: [Online Converter](https://jodersky.github.io/yamlesque/) :point_left:

Built with ScalaJS, this online converter allows you to transform
YAML to JSON as you type.

### Read Some YAML

```scala
val text = s"""|name: yamlesque
               |description: a YAML library for scala
               |authors:
               |  - name: Jakob Odersky
               |    id: jodersky
               |""".stripMargin

val yaml: yamlesque.Value = yamlesque.read(text)

val id = yaml.obj("authors").arr(0).obj("id").str

println(id) // == "jodersky"
```

### Write Some YAML

```scala
import yamlesque as y
val config = y.Obj(
  "auth" -> y.Obj(
    "username" -> y.Str("admin"),
    "password" -> y.Str("guest")
  ),
  "interfaces" -> y.Arr(
    y.Obj(
      "address" -> y.Str("0.0.0.0"),
      "port" -> y.Str("80")
    ),
    y.Obj(
      "address" -> y.Str("0.0.0.0"),
      "port" -> y.Str("443")
    )
  )
)

val stringly = config.render()

println(stringly)
```

will result in

```yaml
auth:
  username: admin
  password: guest
interfaces:
  - address: 0.0.0.0
    port: 80
  - address: 0.0.0.0
    port: 443
```

## Official YAML Conformance

**Yamlesque does not strictly implement all features as defined in [YAML
1.2](http://yaml.org/spec/1.2/spec.html), however support should be
sufficient for most regular documents.**

A major point of divergence between official YAML and this library is the way
in which typing of strings is done. Whereas official YAML implicitly casts
strings to narrower types when possible (for example the string "2" is treated
as the number 2), this library always treats strings as text. This approach
leads to a more uniform parsing system which avoids many subtle bugs, including
the infamous [Norway
Problem](https://hitchdev.com/strictyaml/why/implicit-typing-removed/). In your
application of course, you are still free to attempt to read strings as
diffferent types. Just the parser won't do this for you.

Available features:

- strings: plain (i.e. scalars) and double quoted
- block-style strings (| and >)
- lists and maps
- comments

Features which are currently not supported but for which support is planned:

- multiple documents (i.e. ---)
- single quoted strings

Unsupported features with no planned implementation:

- anchors and references
- flow-styles (aka inline JSON)
- chomping modifiers (e.g. the '-' in '>-')
- tags

Pull requests with additional feature implementations are always welcome!

## Geny-Compatible

The core type `yamlesque.Value` is a `geny.Writable`. This means that it will
work "out-of-the-box" with many other libraries from the ["Singaporean
Stack"](https://github.com/com-lihaoyi). Some examples:

Read YAML from a file, using the os-lib library:

```scala
yamlesque.read(os.read.stream(os.pwd / "config.yaml"))
```

Send it as part of a HTTP request, using the scala-requests library:

```scala
val yaml: yamlesque.Value = ...

requests.post(
  "https://....",
  body = yaml
)
```

Send it as part of a HTTP response, using the cask framework.
