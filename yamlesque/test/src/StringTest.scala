import utest._

object StringTest extends TestSuite {
  import yamlesque._

  def tests = Tests {
    test("literal") {
      read("""||
              |hello
              |world
              |""".stripMargin) ==> Str("hello\nworld\n")
      read(s"""||
               |hello ${" "}
               |world
               |""".stripMargin) ==> Str("hello\nworld\n")
      read("""||
              |hello
              | world
              |""".stripMargin) ==> Str("hello\n world\n")
      read("""||
              |hello
              |  world
              |""".stripMargin) ==> Str("hello\n  world\n")
      read("""||
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("hello\n\n  world\n")
      read("""||
              |
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("\nhello\n\n  world\n")
      read(s"""||
               |
               |hello
               |
               |  world
               | ${" "}
               |foo
               |""".stripMargin) ==> Str("\nhello\n\n  world\n\nfoo\n")
    }
    test("fold") {
      read("""|>
              |hello
              |world
              |""".stripMargin) ==> Str("hello world\n")
      read(s"""|>
               |hello ${" "}
               |world
               |""".stripMargin) ==> Str("hello world\n")
      read("""|>
              |hello
              | world
              |""".stripMargin) ==> Str("hello  world\n")
      read("""|>
              |hello
              |  world
              |""".stripMargin) ==> Str("hello   world\n")
      read("""|>
              |hello
              |
              |  world
              |""".stripMargin) ==> Str("hello\n  world\n")
      read(s"""|>
               |hello
               |
               |  world
               |  ${"  "}
               |foo
               |""".stripMargin) ==> Str("hello\n  world\nfoo\n")
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
              |""".stripMargin) ==> Obj("a" -> Str("hello\n"))
      read("""|a: >
              |
              | hello
              |""".stripMargin) ==> Obj("a" -> Str("hello\n"))
      read("""|a: >
              |
              |  hello
              |  world
              |   foo
              |  bar
              |""".stripMargin) ==> Obj("a" -> Str("hello world  foo bar\n"))
      read("""|a: |
              | hello
              |""".stripMargin) ==> Obj("a" -> Str("hello\n"))
      read("""|a: |
              |
              |  hello
              |  world
              |   foo
              |  bar
              |""".stripMargin) ==> Obj("a" -> Str("\nhello\nworld\n foo\nbar\n"))
    }
    test("invalid map") {
      val e = assertThrows[ParseException] {
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
              |""".stripMargin) ==> Arr(Str("hello\n"))
      read("""|- |
              | hello
              |""".stripMargin) ==> Arr(Str("hello\n"))
    }
    test("invalid list") {
      val e = assertThrows[ParseException] {
        read("""|- |
                |hello
                |""".stripMargin)
      }
      assert(e.message.contains("Expected list item. Found: text"))
    }
    test("chomping clip") {
      read("|\n  a\n") ==> Str("a\n")
      read("|\n  a\n\n\n") ==> Str("a\n")
      read("|\n  a") ==> Str("a") // no final line break to keep
      read(">\n  a\n  b\n\n") ==> Str("a b\n")
      read("|\n\n\n") ==> Str("")
      read("a: |\n  x\n\nb: y") ==> Obj("a" -> Str("x\n"), "b" -> Str("y"))
    }
    test("chomping strip") {
      read("|-\n  a\n") ==> Str("a")
      read("|-\n  a\n  b\n\n\n") ==> Str("a\nb")
      read(">-\n  a\n  b\n\n") ==> Str("a b")
      read("|-\n  a\n\n  b\n") ==> Str("a\n\nb") // only trailing line breaks are stripped
      read("|-\n") ==> Str("")
      read("|-") ==> Str("")
      read("a: |-\n  x\n\nb: y") ==> Obj("a" -> Str("x"), "b" -> Str("y"))
      read("- >-\n  x\n- |-\n  y\n") ==> Arr(Str("x"), Str("y"))
    }
    test("chomping keep") {
      read("|+\n  a\n") ==> Str("a\n")
      read("|+\n  a\n\n\n") ==> Str("a\n\n\n")
      read("|+\n  a") ==> Str("a")
      read(">+\n  a\n  b\n\n") ==> Str("a b\n\n")
      read("|+\n  a\n  \n  \n") ==> Str("a\n\n\n") // whitespace-only lines count too
      read("|+\n\n\n") ==> Str("\n\n")
      read("a: |+\n  x\n\nb: y") ==> Obj("a" -> Str("x\n\n"), "b" -> Str("y"))
      read("a: |+\n  x\n\n# comment\n\nb: y") ==> Obj("a" -> Str("x\n\n"), "b" -> Str("y"))
    }
    test("chomping crlf") {
      read("|-\r\n  a\r\n\r\n") ==> Str("a")
      read("|+\r\n  a\r\n\r\n") ==> Str("a\n\n")
      read("|\r\n  a\r\n\r\n") ==> Str("a\n")
    }
    test("chomping with comment") {
      read("|- # comment\n  a\n") ==> Str("a")
      read(">+ # comment\n  a\n\n") ==> Str("a\n\n")
    }
    test("chomping across documents") {
      readDocuments("--- |+\n  a\n\n---\nb") ==> Seq(Str("a\n\n"), Str("b"))
      readDocuments("--- |+\na\n\n---\nb") ==> Seq(Str("a\n\n"), Str("b"))
      readDocuments("--- |-\na\n...\n") ==> Seq(Str("a"))
    }
    test("chomping-like text") {
      read("|-x") ==> Str("|-x")
      read("|+x") ==> Str("|+x")
      read(">-- a") ==> Str(">-- a")
      read("a: |-x") ==> Obj("a" -> Str("|-x"))
      read("|++") ==> Str("|++")
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
              |""".stripMargin) ==> Str("\"# not a comment\"\n")
      read(""""> not a block"""") ==> Str("""> not a block""")
    }
    test("single quoted") {
      read("''") ==> Str("")
      read("'a'") ==> Str("a")
      read("'a b' 'a c'") ==> Str("a b a c")
      read("'it''s'") ==> Str("it's")
      read("''''") ==> Str("'")
      read("'a\\nb'") ==> Str("a\\nb") // no backslash escapes
      read("'a\\'") ==> Str("a\\")
      read("'a\"b'") ==> Str("a\"b")
      read("'# not a comment'") ==> Str("# not a comment")
      read("'a: b'") ==> Str("a: b")
      read("'- not a list'") ==> Str("- not a list")
      read("'> not a block'") ==> Str("> not a block")
      read("'ü😀'") ==> Str("ü😀")
      read("'a' # comment") ==> Str("a")
      read("it's") ==> Str("it's") // quote not at start is plain text
    }
    test("single quoted in map and list") {
      read("a: 'null'\nb: '~'\nc: ''") ==> Obj(
        "a" -> Str("null"),
        "b" -> Str("~"),
        "c" -> Str("")
      )
      read("- 'x'\n- 'y''z'") ==> Arr(Str("x"), Str("y'z"))
    }
    test("single quoted unterminated") {
      val ex = assertThrows[ParseException](read("a: 'b"))
      ex.message ==> "Expected closing ' but reached EOF"
      (ex.position.line, ex.position.col) ==> (1, 4)
    }
  }
}
