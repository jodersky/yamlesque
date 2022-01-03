import utest._

object BasicTest extends TestSuite {
  import yamlesque._

  val tests = Tests {
    test("empty doc") {
      read("") ==> Null()
    }
    test("plain string") {
      read("a") ==> Str("a")
      read("a ") ==> Str("a")
      read(" a") ==> Str("a")
      read(" a ") ==> Str("a")
    }
    test("combined plain string") {
      read("""|a
              |b
              |""".stripMargin) ==> Str("a b")
    }
    test("combined plain string with newline") {
      read("""|a
              |
              |b
              |""".stripMargin) ==> Str("a\nb")
      read("""|a
              |
              |
              |b
              |""".stripMargin) ==> Str("a\n\nb")
      read(s"""|a
               |
               | ${" "}
               |b
               |""".stripMargin) ==> Str("a\n\nb")
    }
    test("combined plain string indentation") {
      read("""|a
              |  b
              | c
              |d
              |""".stripMargin) ==> Str("a b c d")
    }
    test("map empty") {
      read("a: ") ==> Obj("a" -> Null())
      read("a:") ==> Obj("a" -> Null())
      read("a:\n") ==> Obj("a" -> Null())
    }
    test("map single") {
      read("a: b") ==> Obj("a" -> Str("b"))
      read("a:\n b") ==> Obj("a" -> Str("b"))
      read("a:\n  b") ==> Obj("a" -> Str("b"))
    }
    test("map space in key") {
      read("a : b") ==> Obj("a" -> Str("b"))
    }
    test("map multiple") {
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
    test("map, nested") {
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
    test("list empty") {
      read("- ") ==> Arr(Null())
      read("-") ==> Arr(Null())
      read("-\n") ==> Arr(Null())
    }
    test("list single") {
      read("- a") ==> Arr(Str("a"))
      read("-\n a") ==> Arr(Str("a"))
      read("-\n  a") ==> Arr(Str("a"))
    }
    test("list multiple") {
      read("""|- a
              |-
              |  b
              |-    c
              |""".stripMargin) ==> Arr(Str("a"), Str("b"), Str("c"))
    }
    test("list nested") {
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
          Arr(Str("c1")),
          Str("c2")
        )
      )
    }
    test("list after map") {
      read("""|a:
              | - b
              | - c
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("b"), Str("c"))
      )
    }
    test("list after map no indent") {
      read("""|a:
              |- b
              |- c
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("b"), Str("c"))
      )
      read("""|a:
              |- b:
              |  - c
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Obj("b" -> Arr(Str("c"))))
      )
    }
  }
}
