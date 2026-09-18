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
    test("list after map no indent, followed by key") {
      read("""|a:
              |- x
              |b: y
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("x")),
        "b" -> Str("y")
      )
      read("""|a:
              |- x
              |- y
              |b:
              |- z
              |c:
              |""".stripMargin) ==> Obj(
        "a" -> Arr(Str("x"), Str("y")),
        "b" -> Arr(Str("z")),
        "c" -> Null()
      )
      read("""|a:
              |  b:
              |  - x
              |  c: y
              |d: z
              |""".stripMargin) ==> Obj(
        "a" -> Obj("b" -> Arr(Str("x")), "c" -> Str("y")),
        "d" -> Str("z")
      )
    }
    test("continuation lines") {
      read("a: x\n  y") ==> Obj("a" -> Str("x y"))
      read("a: x\n y") ==> Obj("a" -> Str("x y"))
      read("a:   x\n  y\n    z") ==> Obj("a" -> Str("x y z"))
      read("a: x\n  y\nb: z") ==> Obj("a" -> Str("x y"), "b" -> Str("z"))
      read("a:\n  b: x\n   y") ==> Obj("a" -> Obj("b" -> Str("x y")))
      read("- a\n b") ==> Arr(Str("a b"))
      read("- a\n b\n- c") ==> Arr(Str("a b"), Str("c"))
      read("a: \"x\"\n  y") ==> Obj("a" -> Str("x y"))
    }
    test("tabs") {
      read("a: b\n\t\nc: d") ==> Obj("a" -> Str("b"), "c" -> Str("d"))
      read("a: b\n\t# comment\nc: d") ==> Obj("a" -> Str("b"), "c" -> Str("d"))
      read("a: b\n\t") ==> Obj("a" -> Str("b"))
      read("- \tb") ==> Arr(Str("b"))
      read("a: \tb") ==> Obj("a" -> Str("b"))
      read("a: b\tc") ==> Obj("a" -> Str("b\tc"))
      read("a: |\n  \tx\n  y\n") ==> Obj("a" -> Str("\tx\ny\n"))
    }
    test("utf8") {
      read("€") ==> Str("€")
      read("ключ: значение") ==> Obj("ключ" -> Str("значение"))
      read("""|日本: 語
              |😀: 😀x
              |""".stripMargin) ==> Obj(
        "日本" -> Str("語"),
        "😀" -> Str("😀x")
      )
      read("- é\n- \"ü😀\"\n- |\n  ö\n  😀\n") ==> Arr(Str("é"), Str("ü😀"), Str("ö\n😀\n"))
      read("a: é\r\nb: ü\r\n") ==> Obj("a" -> Str("é"), "b" -> Str("ü"))
    }
    test("utf8 byte order mark") {
      read("\uFEFFa: b") ==> Obj("a" -> Str("b"))
    }
    test("utf8 invalid") {
      read(Array[Byte]('a', 0xff.toByte, 'b')) ==> Str("a\uFFFDb")
      read(Array[Byte]('a', 0xc3.toByte)) ==> Str("a\uFFFD")
      read(Array[Byte]('a', 0xe2.toByte, 'b')) ==> Str("a\uFFFDb")
    }
    test("null") {
      read("null") ==> Null()
      read("~") ==> Null()
      read("a: null") ==> Obj("a" -> Null())
      read("a: Null") ==> Obj("a" -> Null())
      read("a: NULL") ==> Obj("a" -> Null())
      read("a: ~") ==> Obj("a" -> Null())
      read("a: null # comment") ==> Obj("a" -> Null())
      read("- null\n- ~\n-\n") ==> Arr(Null(), Null(), Null())
    }
    test("null-like strings") {
      read("a: \"null\"") ==> Obj("a" -> Str("null"))
      read("a: \"~\"") ==> Obj("a" -> Str("~"))
      read("a: \"\"") ==> Obj("a" -> Str(""))
      read("a: nullx") ==> Obj("a" -> Str("nullx"))
      read("a: null foo") ==> Obj("a" -> Str("null foo"))
      read("a: ~~") ==> Obj("a" -> Str("~~"))
      read("a: nUll") ==> Obj("a" -> Str("nUll"))
      read("a: null\n   x") ==> Obj("a" -> Str("null x"))
      read("a: |\n  null\n") ==> Obj("a" -> Str("null\n"))
      read("null: x") ==> Obj("null" -> Str("x"))
    }
    test("text starting with block indicator") {
      read("a: |x") ==> Obj("a" -> Str("|x"))
      read("a: >x y") ==> Obj("a" -> Str(">x y"))
    }
  }
}
