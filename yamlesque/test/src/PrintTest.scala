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

      import yamlesque._

      val value: Value = read(text)

      read(value.render()) ==> value
    }
    test("reprint null-like strings") {
      import yamlesque._
      val value = Obj(
        "a" -> Str("null"),
        "b" -> Str(""),
        "c" -> Str("~"),
        "d" -> Null(),
        "e" -> Arr(Str("NULL"), Null())
      )
      read(value.render()) ==> value
    }
    test("utf8") {
      import yamlesque._
      val value = Obj("ключ" -> Str("ü😀日本"))
      val bytes = new java.io.ByteArrayOutputStream
      value.writeBytesTo(bytes)
      new String(bytes.toByteArray, "utf-8") ==> "ключ: ü😀日本"
      value.render() ==> "ключ: ü😀日本"
      read(value.render()) ==> value
    }
  }

}
