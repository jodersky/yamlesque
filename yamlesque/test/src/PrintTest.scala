import utest._

object PrintTest extends TestSuite {

  def tests = Tests{
    test("reprint") {
      val text = """|name: yamlesque
                    |description: a YAML library for scala
                    |authors:
                    |  - name: Jakob Odersky
                    |    id: jodersky
                    |    empty:
                    |  - - a
                    |    - b
                    |  - - c
                    |
                    |""".stripMargin

      import yamlesque.{core => y}

      val value: y.Value = y.read(text)

      y.read(value.render()) ==> value
    }
  }

}
