package yamlesque

import utest._

object VerbatimTest extends TestSuite {
  def tests = Tests {
    "empty verbatim" - {
      read("|") ==> Str("")
      read("|\n") ==> Str("")
    }
    "single verbatim" - {
      read("""||
              | a
              |""".stripMargin) ==> Str("a\n")
      read("""||
              |      a
              |""".stripMargin) ==> Str("a\n")
    }
    "multi-line verbatim" - {
      read("""||
              | foo bar
              | baz
              |""".stripMargin) ==> Str("foo bar\nbaz\n")
    }
    "multi-line indent verbatim" - {
      read("""||
              | foo
              |   bar
              | baz
              |""".stripMargin) ==> Str("foo\n  bar\nbaz\n")
    }
    "verbatim in map" - {
      read("""|a: |
              | foo
              |   bar
              | baz
              |b:    |
              |       extra
              |       cool!
              |""".stripMargin) ==> Obj(
        "a" -> Str("foo\n  bar\nbaz\n"),
        "b" -> Str("extra\ncool!\n")
      )
    }
    "empty verbatim in map" - {
      read("""|a: |
              |b:
              |c: |
              |d: |""".stripMargin) ==> Obj(
        "a" -> Str(""),
        "b" -> Null,
        "c" -> Str(""),
        "d" -> Str("")
      )
    }
    "verbatim in list" - {
      read("""|- |
              | foo
              |-    |
              |        extra
              |        cool!
              |""".stripMargin) ==> Arr(Str("foo\n"), Str("extra\ncool!\n"))
    }
    "empty verbatim in list" - {
      read("""|- |
              |-
              |- |
              |- |
              |""".stripMargin) ==> Arr(Str(""), Null, Str(""), Str(""))
    }
    "new lines in verbatim" - {
      read("""|a: |
              |
              |  a
              |  b
              |
              |
              |  c
              |
              |
              |
              |b:
              |""".stripMargin) ==> Obj(
        "a" -> Str("\na\nb\n\n\nc\n"),
        "b" -> Null
      )
    }
    "minimum starting col" - {
      read("""|a:
              |  b: |
              |  c:
              |""".stripMargin) ==> Obj(
        "a" -> Obj(
          "b" -> Str(""),
          "c" -> Null
        )
      )
    }
    "minimum starting col prev" - {
      read("""|a:
              |  a:
              |    a: |
              |  b: foo bar
              |""".stripMargin) ==> Obj(
        "a" -> Obj(
          "a" -> Obj(
            "a" -> Str("")
          ),
          "b" -> Str("foo bar")
        )
      )
    }
  }
}
