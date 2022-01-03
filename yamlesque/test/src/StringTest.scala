import utest._

object StringTest extends TestSuite {
  import yamlesque._

  def tests = Tests {
    test("literal") {
      read("""||
              |hello
              |world
              |""".stripMargin) ==> Str("hello\nworld")
      read(s"""||
               |hello ${" "}
               |world
               |""".stripMargin) ==> Str("hello\nworld")
      read("""||
              |hello
              | world
              |""".stripMargin) ==> Str("hello\n world")
      read("""||
              |hello
              |  world
              |""".stripMargin) ==> Str("hello\n  world")
      read("""||
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("hello\n\n  world")
      read("""||
              |
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("\nhello\n\n  world")
      read(s"""||
               |
               |hello
               |
               |  world
               | ${" "}
               |foo
               |""".stripMargin) ==> Str("\nhello\n\n  world\n\nfoo")
    }
    test("fold") {
      read("""|>
              |hello
              |world
              |""".stripMargin) ==> Str("hello world")
      read(s"""|>
               |hello ${" "}
               |world
               |""".stripMargin) ==> Str("hello world")
      read("""|>
              |hello
              | world
              |""".stripMargin) ==> Str("hello  world")
      read("""|>
              |hello
              |  world
              |""".stripMargin) ==> Str("hello   world")
      read("""|>
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("hello\n  world")
      read(s"""|>
               |hello
               |
               |  world
               |  ${"  "}
               |foo
               |""".stripMargin) ==> Str("hello\n  world\nfoo")
    }
    test("empty") {
      read("""|""") ==> Str("")
      read(""">""") ==> Str("")
    }
    test("in text") {
      read("""a|""") ==> Str("a|")
      read("""a>""") ==> Str("a>")
      read("""a| a""") ==> Str("a| a")
      read("""a> a""") ==> Str("a> a")
      read("""a | a""") ==> Str("a | a")
      read("""a > a""") ==> Str("a > a")
    }
    test("in map") {
      read("""a: >""") ==> Obj("a" -> Str(""))
      read("a: >\nb: >") ==> Obj("a" -> Str(""), "b" -> Str(""))
      read("""a: |""") ==> Obj("a" -> Str(""))
      read("a: |\nb: |") ==> Obj("a" -> Str(""), "b" -> Str(""))

      read("""|a: >
              | hello
              |""".stripMargin) ==> Obj("a" -> Str("hello"))
      read("""|a: >
              |
              | hello
              |""".stripMargin) ==> Obj("a" -> Str("hello"))
      read("""|a: >
              |
              |  hello
              |  world
              |   foo
              |  bar
              |""".stripMargin) ==> Obj("a" -> Str("hello world  foo bar"))
      read("""|a: |
              | hello
              |""".stripMargin) ==> Obj("a" -> Str("hello"))
      read("""|a: |
              |
              |  hello
              |  world
              |   foo
              |  bar
              |""".stripMargin) ==> Obj("a" -> Str("\nhello\nworld\n foo\nbar"))
    }
    test("invalid map") {
      val e = intercept[ParseException] {
        read("""|a: |
                |hello
                |""".stripMargin)
      }
      assert(e.message.contains("Expected key. Found: text"))
    }
    test("in list") {
      read("""- >""") ==> Arr(Str(""))
      read("""- |""") ==> Arr(Str(""))
      read("- >\n- >") ==> Arr(Str(""), Str(""))
      read("- |\n- |") ==> Arr(Str(""), Str(""))
      read("""|- >
              | hello
              |""".stripMargin) ==> Arr(Str("hello"))
      read("""|- |
              | hello
              |""".stripMargin) ==> Arr(Str("hello"))
    }
    test("invalid list") {
      val e = intercept[ParseException] {
        read("""|- |
                |hello
                |""".stripMargin)
      }
      assert(e.message.contains("Expected list item. Found: text"))
    }
    test("nested") {
      read("|\n|") ==> Str("|")
      read("|\n||") ==> Str("||")
      read(">\n|") ==> Str("|")
      read(">\n|>") ==> Str("|>")
    }
    test("quoted") {
      read("""""""") ==> Str("")
      read(""""a"""") ==> Str("a")
      read(""""a b" "a c"""") ==> Str("a b a c")
      read(""""a\"b"""") ==> Str("""a"b""")
      read(""""a\"b # a b"""") ==> Str("""a"b # a b""")
      read(""""# not a comment"""") ==> Str("""# not a comment""")
      read("""|>
              |"# not a comment"
              |""".stripMargin) ==> Str(""""# not a comment"""")
      read(""""> not a block"""") ==> Str("""> not a block""")
    }
  }
}
