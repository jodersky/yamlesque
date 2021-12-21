import utest._

object ReadmeTest extends TestSuite {

  def tests = Tests{
    test("read") {
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
    test("write") {
      import yamlesque.core.{Arr, Obj, Str}
      val config = Obj(
        "auth" -> Obj(
          "username" -> Str("admin"),
          "password" -> Str("guest")
        ),
        "interfaces" -> Arr(
          Obj(
            "address" -> Str("0.0.0.0"),
            "port" -> Str("80")
          ),
          Obj(
            "address" -> Str("0.0.0.0"),
            "port" -> Str("443")
          )
        )
      )
      println(config.render())
    }
  }

}
