package yamlesque

import utest._

object BasicTest extends TestSuite {
  def tests = Tests {
    "empty doc" - {
      read("") ==> Null
    }
    "empty, terminated doc" - {
      read("---") ==> Null
    }
    "null doc" - {
      read("null") ==> Null
    }
    "plain string" - {
      read("a") ==> Str("a")
      read("a ") ==> Str("a")
    }
    "plain int" - {
      read("1") ==> Num(1)
    }
    "plain double" - {
      read("1.1") ==> Num(1.1)
    }
    "combined plain string" - {
      read("""|a
              |b
              |""".stripMargin) ==> Str("a b")
    }
    "combined plain string, indentation" - {
      read("""|a
              |  b
              | c
              |d
              |""".stripMargin) ==> Str("a b c d")
    }
    "plain bool" - {
      read("true") ==> Bool(true)
      read("false") ==> Bool(false)
    }
    "map, empty" - {
      read("a: ") ==> Obj("a" -> Null)
      read("a:") ==> Obj("a" -> Null)
      read("a:\n") ==> Obj("a" -> Null)
    }
    "map, single" - {
      read("a: b") ==> Obj("a" -> Str("b"))
      read("a:\n b") ==> Obj("a" -> Str("b"))
      read("a:\n  b") ==> Obj("a" -> Str("b"))
    }
    "map, space in key" - {
      read("a : b") ==> Obj("a" -> Str("b"))
      read("hello world : b") ==> Obj("hello world" -> Str("b"))
    }
    "map, multiple" - {
      read("""|a: x
              |b:
              | y
              |c: 
              |  foo
              |""".stripMargin) ==> Obj(
        "a" -> Str("x"),
        "b" -> Str("y"),
        "c" -> Str("foo")
      )
    }
    "map, nested" - {
      read("""|a:
              | b: x
              |b: a: foo
              |   b: bar
              |c: y
              |""".stripMargin) ==> Obj(
        "a" -> Obj("b" -> Str("x")),
        "b" -> Obj(
          "a" -> Str("foo"),
          "b" -> Str("bar")
        ),
        "c" -> Str("y")
      )
    }
    "list, empty" - {
      read("- ") ==> Arr(Null)
      read("-") ==> Arr(Null)
      read("-\n") ==> Arr(Null)
    }
    "list, single" - {
      read("- a") ==> Arr(Str("a"))
      read("-\n a") ==> Arr(Str("a"))
      read("-\n  a") ==> Arr(Str("a"))
    }
    "list, multiple" - {
      read("""|- a
              |-
              |  b
              |-    c
              |""".stripMargin) ==> Arr(Str("a"), Str("b"), Str("c"))
    }
    "list, nested" - {
      read("""|- a
              |- - b1
              |  - b2
              |- 
              |    - - c1
              |    - c2
              |""".stripMargin) ==> Arr(
        Str("a"),
        Arr(Str("b1"), Str("b2")),
        Arr(
          Arr("c1"),
          Str("c2")
        )
      )
    }
    "list after map" - {
      read("""|a:
              | - b
              | - c
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("b"), Str("c"))
      )
    }
    "list after map, no indent" - {
      read("""|a:
              |- b
              |- c
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("b"), Str("c"))
      )
    }
    "comment" - {
      read("#nothing to see here") ==> Null
      read("# nothing to see here") ==> Null
    }
    "comment, after string" - {
      read("a #nothing to see here") ==> Str("a")
    }
    "comment, after key" - {
      read("a: #nothing to see here") ==> Obj("a" -> Null)
    }
    "comment, after item" - {
      read("- #nothing to see here") ==> Arr(Null)
    }
    "not a comment" - {
      read("a#nothing to see here") ==> Str("a#nothing to see here")
      read("a:#nothing to see here") ==> Str("a:#nothing to see here")
      read("a-#nothing to see here") ==> Str("a-#nothing to see here")
    }
    "mixed" - {
      read("""|# Authentication config
              |auth:
              |  username: john doe
              |  password:
              |    guest
              |  2fa:
              |    - otp: a1234
              |    - 
              |      code: abc
              |    - other: backdoor! # super secret back door
              |
              |# Interface to listen on
              |# 
              |# Multiple are allowed
              |#
              |interfaces:
              |  - addr: 0.0.0.0
              |    port: 1234
              |  - addr: 0.0.0.0
              |    port: 80
              |extra: null
              |""".stripMargin) ==> Obj(
        "auth" -> Obj(
          "username" -> Str("john doe"),
          "password" -> Str("guest"),
          "2fa" -> Arr(
            Obj("otp" -> Str("a1234")),
            Obj("code" -> Str("abc")),
            Obj("other" -> Str("backdoor!"))
          )
        ),
        "interfaces" -> Arr(
          Obj(
            "addr" -> Str("0.0.0.0"),
            "port" -> Num(1234)
          ),
          Obj(
            "addr" -> Str("0.0.0.0"),
            "port" -> Num(80)
          )
        ),
        "extra" -> Null
      )
    }
  }
}
