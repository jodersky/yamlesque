import utest._

object FlowTest extends TestSuite {
  import yamlesque._

  def error(source: String): ParseException = assertThrows[ParseException](read(source))

  def tests = Tests {
    test("empty") {
      read("[]") ==> Arr()
      read("{}") ==> Obj()
      read("[ ]") ==> Arr()
      read("{\n}") ==> Obj()
      read("a: []\nb: {}") ==> Obj("a" -> Arr(), "b" -> Obj())
    }
    test("sequence") {
      read("[a, b, c]") ==> Arr(Str("a"), Str("b"), Str("c"))
      read("[a,b,c]") ==> Arr(Str("a"), Str("b"), Str("c"))
      read("[a b, c-d, -1, .5]") ==> Arr(Str("a b"), Str("c-d"), Num(-1), Num(0.5))
      read("[a, b,]") ==> Arr(Str("a"), Str("b"))
    }
    test("mapping") {
      read("{a: 1, b: two}") ==> Obj("a" -> Num(1), "b" -> Str("two"))
      read("{a: 1,}") ==> Obj("a" -> Num(1))
      read("{a: , b}") ==> Obj("a" -> Null(), "b" -> Null())
      read("{a:}") ==> Obj("a" -> Null())
      read("{a:1}") ==> Obj("a:1" -> Null()) // like YAML, `:` needs a space after an unquoted key
      read("{1: x, true: y}") ==> Obj("1" -> Str("x"), "true" -> Str("y")) // keys are strings
    }
    test("typed values") {
      read("[1, -2.5, 0x1F, .inf, true, False, null, ~, x]") ==> Arr(
        Num(1), Num(-2.5), Num(31), Num(Double.PositiveInfinity),
        Bool(true), Bool(false), Null(), Null(), Str("x")
      )
      read("['1', \"true\", 'null']") ==> Arr(Str("1"), Str("true"), Str("null"))
    }
    test("quoted") {
      read("[\"a, b\", '[c]', \"{d: e}\", 'it''s', \"#x\"]") ==> Arr(
        Str("a, b"), Str("[c]"), Str("{d: e}"), Str("it's"), Str("#x")
      )
      read("{'a b': \"c\"}") ==> Obj("a b" -> Str("c"))
    }
    test("json") {
      read("""{"a": 1, "b": [true, false, null], "c": {"d": "e"}, "f": -1.5e3}""") ==> Obj(
        "a" -> Num(1),
        "b" -> Arr(Bool(true), Bool(false), Null()),
        "c" -> Obj("d" -> Str("e")),
        "f" -> Num(-1500)
      )
      read("""{"a":1,"b":[1,2],"c":{}}""") ==> Obj(
        "a" -> Num(1),
        "b" -> Arr(Num(1), Num(2)),
        "c" -> Obj()
      )
      read("""[{"x":"y"}]""") ==> Arr(Obj("x" -> Str("y")))
    }
    test("plain scalars with special characters") {
      read("[http://example.com, a:b, a#b, 'x']") ==> Arr(
        Str("http://example.com"), Str("a:b"), Str("a#b"), Str("x")
      )
      read("{url: http://example.com}") ==> Obj("url" -> Str("http://example.com"))
      read("[ü, 😀, 日本]") ==> Arr(Str("ü"), Str("😀"), Str("日本"))
    }
    test("nested") {
      read("[[1, 2], [3], []]") ==> Arr(Arr(Num(1), Num(2)), Arr(Num(3)), Arr())
      read("{a: {b: [c, {d: e}]}}") ==> Obj("a" -> Obj("b" -> Arr(Str("c"), Obj("d" -> Str("e")))))
    }
    test("single pair in sequence") {
      read("[a: 1, b]") ==> Arr(Obj("a" -> Num(1)), Str("b"))
      read("[\"a\": 1]") ==> Arr(Obj("a" -> Num(1)))
      read("[a: ]") ==> Arr(Obj("a" -> Null()))
      read("[a: [1, 2]]") ==> Arr(Obj("a" -> Arr(Num(1), Num(2))))
    }
    test("in block context") {
      read("branches: [main]") ==> Obj("branches" -> Arr(Str("main")))
      read("a: [1, 2]\nb: {c: d}\ne: f") ==> Obj(
        "a" -> Arr(Num(1), Num(2)),
        "b" -> Obj("c" -> Str("d")),
        "e" -> Str("f")
      )
      read("- [1]\n- {a: b}\n- c") ==> Arr(Arr(Num(1)), Obj("a" -> Str("b")), Str("c"))
      read("a:\n  [1, 2]\nb: 3") ==> Obj("a" -> Arr(Num(1), Num(2)), "b" -> Num(3))
      read("a:\n  b: [x]\n  c: y") ==> Obj("a" -> Obj("b" -> Arr(Str("x")), "c" -> Str("y")))
      read("command: [sh, -c, echo hi]") ==> Obj("command" -> Arr(Str("sh"), Str("-c"), Str("echo hi")))
    }
    test("multi-line") {
      read("a: [\n  1,\n  2\n]\nb: 3") ==> Obj("a" -> Arr(Num(1), Num(2)), "b" -> Num(3))
      read("a: {\nx: 1,\n      y: 2}\nb: 3") ==> Obj("a" -> Obj("x" -> Num(1), "y" -> Num(2)), "b" -> Num(3))
      read("[a\n  b, c]") ==> Arr(Str("a b"), Str("c"))
      read("[a\n\n  b]") ==> Arr(Str("a\nb"))
      read("{\n  \"a\": [\n    1,\n    2\n  ]\n}") ==> Obj("a" -> Arr(Num(1), Num(2)))
    }
    test("comments") {
      read("[a, # comment\n b] # trailing") ==> Arr(Str("a"), Str("b"))
      read("[a # comment\n, b]") ==> Arr(Str("a"), Str("b"))
      read("a: {b: c} # comment\nd: e") ==> Obj("a" -> Obj("b" -> Str("c")), "d" -> Str("e"))
    }
    test("multiple documents") {
      readDocuments("--- [1]\n--- {a: b}\n---\n[x,\n y]") ==> Seq(
        Arr(Num(1)), Obj("a" -> Str("b")), Arr(Str("x"), Str("y"))
      )
    }
    test("positions") {
      val recorded = PositionTest.record("a: [x, {k: 😀}]")
      recorded.map { case (text, p) => (text, p.line, p.col, p.index) } ==> List(
        ("a", 1, 1, 0),
        ("x", 1, 5, 4),
        ("k", 1, 9, 8),
        ("😀", 1, 12, 11)
      )
    }
    test("errors") {
      def check(source: String, message: String, line: Int, col: Int) = {
        val ex = error(source)
        ex.message ==> message
        (ex.position.line, ex.position.col) ==> (line, col)
      }
      check("[a, b", "Expected ']' to close this flow collection, but reached EOF", 1, 1)
      check("a: {b: 1", "Expected '}' to close this flow collection, but reached EOF", 1, 4)
      check("[a, [b]", "Expected ']' to close this flow collection, but reached EOF", 1, 1)
      check("[a,,b]", "Expected a value, found ','", 1, 4)
      check("[,]", "Expected a value, found ','", 1, 2)
      check("{,}", "Expected a key, found ','", 1, 2)
      check("[: a]", "Expected a value, found ':'", 1, 2)
      check("[a}", "Expected ',' or ']'", 1, 3)
      check("{a: b: c}", "Expected ',' or '}'", 1, 6)
      check("[a] b", "Unexpected content after flow collection", 1, 5)
      check("[a]: b", "Flow collections cannot be used as keys", 1, 4)
      check("{[a]: b}", "Flow collections cannot be used as keys", 1, 2)
      check("[[a]: b]", "Flow collections cannot be used as keys", 1, 5)
      check("[\"abc]", "Expected closing \" but reached EOF", 1, 2)
      check("[a,\n---\n]", "Document markers are not allowed within flow collections", 2, 1)
      check("[a]\n[b]", "Expected end of document. Found: flow collection", 2, 1)
    }
    test("render empty collections") {
      Arr().render() ==> "[]"
      Obj().render() ==> "{}"
      Obj("a" -> Arr(), "b" -> Obj()).render() ==> "a: []\nb: {}"
      val value = Obj(
        "a" -> Arr(),
        "b" -> Obj(),
        "c" -> Arr(Arr(), Obj(), Arr(Str("x"))),
        "d" -> Obj("e" -> Obj())
      )
      read(value.render()) ==> value
      readDocuments(writeDocuments(Seq(Arr(), Obj()))) ==> Seq(Arr(), Obj())
    }
    test("render bracket-like strings") {
      val value = Obj("a" -> Str("[x"), "b" -> Str("{y}"), "c" -> Str("[]"), "d" -> Str("x[y]"))
      read(value.render()) ==> value
      Obj("a" -> Str("[]")).render() ==> "a: '[]'"
    }
  }
}
