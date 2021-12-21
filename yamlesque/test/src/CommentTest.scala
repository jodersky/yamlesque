package yamlesque

import utest._

object CommentTest extends TestSuite {
  import yamlesque.core._

  val tests = Tests {
    test("comment") {
      read("#foo") ==> Null()
      read("# foo") ==> Null()
      read("#   foo") ==> Null()
      read("#   foo   ") ==> Null()
      read(" #   foo   ") ==> Null()
      read("   #   foo   ") ==> Null()
      read("   #   foo   bar ") ==> Null()
    }
    test("multiple lines") {
      read("""|# hello
              |# world
              |""".stripMargin) ==> Null()
    }
    test("text") {
      read("""|hello # world
              |""".stripMargin) ==> Str("hello")
      read("""|# world
              |hello
              |""".stripMargin) ==> Str("hello")
      read("""|hello
              |# world
              |""".stripMargin) ==> Str("hello")
    }
    test("map") {
      read("""|hello: # world
              |""".stripMargin) ==> Obj("hello" -> Null())
      read("""|hello:
              |# world
              |""".stripMargin) ==> Obj("hello" -> Null())
      read("""|# world
              |hello:
              |""".stripMargin) ==> Obj("hello" -> Null())
    }
    test("list") {
      read("""|- # yo
              |""".stripMargin) ==> Arr(Null())
      read("""|-
              |# world
              |""".stripMargin) ==> Arr(Null())
      read("""|# world
              |-
              |""".stripMargin) ==> Arr(Null())
    }
    test("block text") {
      read("""|> # yo
              |hello
              |""".stripMargin) ==> Str("hello")
      read("""|# test
              |>
              |# yo
              |hello
              |# more
              |""".stripMargin) ==> Str("# yo hello # more")
    }
  }
}
