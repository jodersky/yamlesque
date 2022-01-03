import utest._

object TranslationTest extends TestSuite {

  case class Container1(a: String)
  object Container1 {
    implicit def reader: upickle.default.ReadWriter[Container1] = upickle.default.macroRW
  }

  case class Container2(a: Int)
  object Container2 {
    implicit def reader: upickle.default.ReadWriter[Container2] = upickle.default.macroRW
  }

  case class Nested(a: Int, b: List[String])
  object Nested {
    implicit def reader: upickle.default.ReadWriter[Nested] = upickle.default.macroRW
  }

  case class Top(name: String, nested: List[Nested])
  object Top {
    implicit def reader: upickle.default.ReadWriter[Top] = upickle.default.macroRW
  }

  def tests = Tests {
    test("basic") {
      val input = """a: hello world"""
      yamlesque.upickle.default.read[Container1](input) ==> Container1("hello world")
    }
    test("implicit type cast") {
      val input = """a: 42"""
      yamlesque.upickle.default.read[Container2](input) ==> Container2(42)
    }
    test("complex") {
      val input = """|name: hello, world
                     |nested:
                     | - a: 1
                     |   b: - item 1
                     |      - item 2
                     | - b: - "item a"
                     |      - item b
                     |   a: 2
                     |""".stripMargin
      yamlesque.upickle.default.read[Top](input) ==> Top(
        "hello, world",
        List(
          Nested(1, List("item 1", "item 2")),
          Nested(2, List("item a", "item b"))
        )
      )
    }
  }

}
