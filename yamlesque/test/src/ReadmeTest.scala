import utest._

object ReadmeTest extends TestSuite {

  def tests = Tests{
    test("readme") {
      val text = """|name: yamlesque
                    |description: a YAML library for scala
                    |authors:
                    |  - name: Jakob Odersky
                    |    id: jodersky
                    |  - name: Another
                    |""".stripMargin

      import yamlesque.{core => y}

      val value: y.Value = y.read(text)

      val id = value.obj("authors").arr(0).obj("id").str

      println(id) // == "jodersky"

      id ==> "jodersky"
    }
  }

}
