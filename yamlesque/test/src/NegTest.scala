import utest._

object NegTest extends TestSuite {
  import yamlesque._

  def tests = Tests {
    test("key and string") {
      assertThrows[ParseException] {
        read("""|b:
                |a
                |""".stripMargin)
      }
    }
    test("list and key") {
      assertThrows[ParseException] {
        read("""|- b:
                |a:
                |""".stripMargin)
      }
    }
    test("list and string") {
      assertThrows[ParseException] {
        read("""|-
                |a
                |""".stripMargin)
      }
    }
    test("list and key") {
      assertThrows[ParseException] {
        read("""|-
                |a:
                |""".stripMargin)
      }
    }
    test("key alignment") {
      assertThrows[ParseException] {
        read("""|a:
                |  a:
                | b:
                |""".stripMargin)
      }
    }
    test("list alignment") {
      assertThrows[ParseException] {
        read("""|-
                |  -
                | -
                |""".stripMargin)
      }
    }
    test("list after map no indent, followed by text") {
      assertThrows[ParseException] {
        read("""|a:
                |- x
                |b
                |""".stripMargin)
      }
    }
    test("continuation line not indented") {
      assertThrows[ParseException] {
        read("""|a: x
                |y
                |""".stripMargin)
      }
      assertThrows[ParseException] {
        read("""|- a
                |b
                |""".stripMargin)
      }
    }
    test("continuation line is a key") {
      assertThrows[ParseException] {
        read("""|a: x
                |  b: y
                |""".stripMargin)
      }
    }
    def tabError(source: String, line: Int, col: Int) = {
      val ex = assertThrows[ParseException](read(source))
      ex.message ==> "Tabs are not allowed for indentation"
      (ex.position.line, ex.position.col) ==> (line, col)
    }
    test("tab indentation") {
      tabError("\ta: b", 1, 1)
      tabError("a:\n\tb: c", 2, 1)
      tabError("a:\n  \tb: c", 2, 3)
      tabError("a:\n\t  b: c", 2, 1)
      tabError("- a\n\t- b", 2, 1)
      tabError("a: x\n\ty", 2, 1)
      tabError("a: |\n  x\n\tb: c", 3, 1)
    }
    // test("verbatim end") {
    //   assertThrows[ParseException] {
    //     read("""|a: |
    //             |   foo
    //             |  b # b is parsed as a scalar
    //             |""".stripMargin)
    //   }
    // }
    // test("verbatim before last token") {
    //   assertThrows[ParseException] {
    //     read("""|a:
    //             |   a: |
    //             |   b
    //             |""".stripMargin)

    //   }
    // }
    // test("verbatim before last token 2") {
    //   assertThrows[ParseException] {
    //     read("""|a:
    //             |  a:
    //             |    a: |
    //             |   b:
    //             |""".stripMargin)
    //   }
    // }
  }
}
