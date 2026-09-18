import utest._

object PositionTest extends TestSuite {
  import yamlesque._

  // records the position of every key and scalar
  class Recorder(out: collection.mutable.ArrayBuffer[(String, Position)])
      extends Visitor[Unit] with ObjectVisitor[Unit] with ArrayVisitor[Unit] {
    def visitObject(ctx: Ctx) = this
    def visitArray(ctx: Ctx) = this
    def visitEmpty(ctx: Ctx) = out += "" -> ctx.pos
    def visitString(ctx: Ctx, text: CharSequence) = out += text.toString -> ctx.pos
    def visitBool(ctx: Ctx, value: Boolean) = out += value.toString -> ctx.pos
    def visitNumber(ctx: Ctx, text: CharSequence) = out += text.toString -> ctx.pos
    def visitQuotedString(ctx: Ctx, text: CharSequence) = out += s""""$text"""" -> ctx.pos
    def visitBlockStringLiteral(ctx: Ctx, text: CharSequence) = ()
    def visitBlockStringFolded(ctx: Ctx, text: CharSequence) = ()
    def visitKey(ctx: Ctx, key: String) = out += key -> ctx.pos
    def visitIndex(ctx: Ctx, idx: Int) = ()
    def subVisitor() = this
    def visitValue(ctx: Ctx, value: Any) = ()
    def visitEnd() = ()
  }

  def record(source: String) = {
    val out = collection.mutable.ArrayBuffer.empty[(String, Position)]
    val in = new java.io.ByteArrayInputStream(source.getBytes("utf-8"))
    new Parser(in, "virtual").parseValue(0, new Recorder(out))
    out.toList
  }

  def error(source: String): ParseException =
    assertThrows[ParseException](read(source))

  def tests = Tests {
    test("index points to text") {
      val source = """|ключ: значение
                      |😀: "日本"
                      |list:
                      |  - é
                      |  - x😀y
                      |""".stripMargin
      val bytes = source.getBytes("utf-8")
      val recorded = record(source)
      recorded.map(_._1) ==> List(
        "ключ", "значение", "😀", "\"日本\"", "list", "é", "x😀y"
      )
      for ((text, pos) <- recorded) {
        val n = text.getBytes("utf-8").length
        new String(bytes.slice(pos.index, pos.index + n), "utf-8") ==> text
      }
    }
    test("line and col") {
      val positions = record("ключ: значение\n😀: x😀y").map(_._2)
      positions.map(p => (p.line, p.col, p.index)) ==> List(
        (1, 1, 0), // ключ
        (1, 7, 10), // значение
        (2, 1, 27), // 😀
        (2, 4, 33) // x😀y
      )
    }
    test("byte order mark") {
      val List((_, key), (_, value)) = record("\uFEFFa: b"): @unchecked
      (key.line, key.col, key.index) ==> (1, 1, 3)
      (value.line, value.col, value.index) ==> (1, 4, 6)
    }
    test("crlf") {
      val positions = record("a: é\r\nb: c").map(_._2)
      positions.map(p => (p.line, p.col, p.index)) ==> List(
        (1, 1, 0),
        (1, 4, 3),
        (2, 1, 7),
        (2, 4, 10)
      )
    }
    test("error position") {
      val source = "😀é: \"x"
      val ex = error(source)
      ex.position.line ==> 1
      ex.position.col ==> 5
      ex.position.index ==> source.getBytes("utf-8").indexOf('"'.toByte)
    }
    test("pretty") {
      val source = "a: b\nкл: \"x\nc: d"
      val ex = error(source)
      ex.position.index ==> 11
      ex.pretty(source) ==>
        """|Expected closing " but reached EOF
           |virtual:2:5
           |кл: "x
           |    ^""".stripMargin
    }
    test("pretty indentation error") {
      val source = "日本:\n  a:\n b:\n"
      val ex = error(source)
      ex.pretty(source) ==>
        """|Entries within the same map must start at the same column.
           |virtual:3:2
           | b:
           | ^""".stripMargin
    }
    test("end of file") {
      def eof(source: String) = {
        val List((_, pos)) = record(source): @unchecked
        (pos.line, pos.col, pos.index)
      }
      eof("") ==> (1, 1, 0)
      eof("\n") ==> (2, 1, 1)
      eof("# c\n") ==> (2, 1, 4)
      eof("\n\n") ==> (3, 1, 2)
    }
    test("end of file error") {
      val ex = error("{a:\n")
      (ex.position.line, ex.position.col, ex.position.index) ==> (2, 1, 4)
      ex.message ==> "Expected a value, but reached EOF"
    }
    test("empty values") {
      val positions = record("a:\nb: \nc:\n  d: 1\ne:\n- \n- x\n-\n").map {
        case (text, p) => (text, p.line, p.col)
      }
      positions ==> List(
        ("a", 1, 1),
        ("", 1, 3), // just after the colon
        ("b", 2, 1),
        ("", 2, 3),
        ("c", 3, 1),
        ("d", 4, 3),
        ("1", 4, 6),
        ("e", 5, 1),
        ("", 6, 2), // just after the dash
        ("x", 7, 3),
        ("", 8, 2)
      )
    }
    test("empty value at end of file") {
      val positions = record("a:").map { case (text, p) => (text, p.line, p.col, p.index) }
      positions ==> List(("a", 1, 1, 0), ("", 1, 3, 2))
    }
    test("lineAt") {
      val bytes = "a\r\nбв\nc".getBytes("utf-8")
      Position.lineAt(bytes, 0) ==> "a"
      Position.lineAt(bytes, 3) ==> "бв"
      Position.lineAt(bytes, 5) ==> "бв"
      Position.lineAt(bytes, bytes.length) ==> "c"
      Position.lineAt(bytes, -1) ==> "a"
    }
  }
}
