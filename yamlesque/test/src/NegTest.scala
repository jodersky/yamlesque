package yamlesque

import utest._

object NegTest extends TestSuite {
  def tests = Tests {
    "key and string" - {
      val e = intercept[Parser.ParseException] {
        read("""|b:
                |a
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
    "list and key" - {
      val e = intercept[Parser.ParseException] {
        read("""|- b:
                |a:
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
    "list and string" - {
      val e = intercept[Parser.ParseException] {
        read("""|-
                |a
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
    "list and key" - {
      val e = intercept[Parser.ParseException] {
        read("""|-
                |a:
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
    "key alignment" - {
      val e = intercept[Parser.ParseException] {
        read("""|a:
                |  a:
                | b:
                |""".stripMargin)
      }
      assert(e.message.contains("aligned"))
    }
    "list alignment" - {
      val e = intercept[Parser.ParseException] {
        read("""|-
                |  -
                | -
                |""".stripMargin)
      }
      assert(e.message.contains("aligned"))
    }
    "verbatim end" - {
      val e = intercept[Parser.ParseException] {
        read("""|a: |
                |   foo
                |  b # b is parsed as a scalar
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
    "verbatim before last token" - {
      val e = intercept[Parser.ParseException] {
        read("""|a:
                |   a: |
                |   b
                |""".stripMargin)

      }
      assert(e.message.contains("expected"))
    }
    "verbatim before last token 2" - {
      val e = intercept[Parser.ParseException] {
        read("""|a:
                |  a: 
                |    a: |
                |   b:
                |""".stripMargin)
      }
      assert(e.message.contains("aligned"))
    }
    "verbatim followed by scalar" - {
      val e = intercept[Parser.ParseException] {
        read("""||
                |  a
                |a
                |""".stripMargin)
      }
      assert(e.message.contains("expected"))
    }
  }
}
