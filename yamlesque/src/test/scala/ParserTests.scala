package yamlesque

import utest._

object ParserTests extends TestSuite {

  val yaml = YamlMapping(
    "key1" -> YamlScalar("value1"),
    "key2" -> YamlMapping(
      "key1" -> YamlScalar("value1"),
      "key2" -> YamlScalar("value1"),
      "key3" -> YamlSequence(
        YamlScalar("a1"),
        YamlSequence(
          YamlScalar("a1"),
          YamlScalar("a2"),
          YamlScalar("a3")
        ),
        YamlScalar("a3"),
        YamlMapping(
          "a1" -> YamlScalar("b"),
          "a2" -> YamlScalar("b"),
          "a3" -> YamlScalar("b"),
          "a4" -> YamlScalar("b")
        ),
        YamlScalar("a4"),
        YamlScalar("a4")
      ),
      "key4" -> YamlScalar("value1"),
      "key5" -> YamlScalar("value1"),
      "key6" -> YamlScalar("value1")
    ),
    "key3" -> YamlScalar("value3")
  )

  val string =
    s"""|
        |key1: value1
        |key2:
        |  key4: value1
        |  key5: value1
        |  key1: value1
        |  key2: value1
        |  key6: value1
        |  key3:
        |    - a1
        |    -
        |      - a1
        |      - a2
        |      - a3
        |    - a3
        |    -
        |      a1: b
        |      a2: b
        |      a3: b
        |      a4: b
        |    - a4
        |    - a4
        |key3: value3
        |""".stripMargin

  val tests = Tests{
    "parse" - {
      //      assert(Parser.parse(string) == yaml)
    }
    "printandparse" - {
      //assert(Parser.parse(yaml.print) == yaml)
    }
  }

}
