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
