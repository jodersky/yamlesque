import utest._

object NegTest extends TestSuite {
  import yamlesque.core._

  def tests = Tests {
    test("key and string") {
      intercept[ParseException] {
        read("""|b:
                |a
                |""".stripMargin)
      }
    }
    test("list and key") {
      intercept[ParseException] {
        read("""|- b:
                |a:
                |""".stripMargin)
      }
    }
    test("list and string") {
      intercept[ParseException] {
        read("""|-
                |a
                |""".stripMargin)
      }
    }
    test("list and key") {
      intercept[ParseException] {
        read("""|-
                |a:
                |""".stripMargin)
      }
    }
    test("key alignment") {
      intercept[ParseException] {
        read("""|a:
                |  a:
                | b:
                |""".stripMargin)
      }
    }
    test("list alignment") {
      intercept[ParseException] {
        read("""|-
                |  -
                | -
                |""".stripMargin)
      }
    }
    // test("verbatim end") {
    //   intercept[ParseException] {
    //     read("""|a: |
    //             |   foo
    //             |  b # b is parsed as a scalar
    //             |""".stripMargin)
    //   }
    // }
    // test("verbatim before last token") {
    //   intercept[ParseException] {
    //     read("""|a:
    //             |   a: |
    //             |   b
    //             |""".stripMargin)

    //   }
    // }
    // test("verbatim before last token 2") {
    //   intercept[ParseException] {
    //     read("""|a:
    //             |  a:
    //             |    a: |
    //             |   b:
    //             |""".stripMargin)
    //   }
    // }
  }
}
