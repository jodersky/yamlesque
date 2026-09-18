import utest._

object DocumentTest extends TestSuite {
  import yamlesque._

  // returns at most one byte per read, to exercise buffer refills
  class TrickleInputStream(data: Array[Byte]) extends java.io.InputStream {
    private var pos = 0
    def read(): Int = if (pos < data.length) { pos += 1; data(pos - 1) & 0xff } else -1
    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      if (len == 0) 0
      else if (pos >= data.length) -1
      else { b(off) = data(pos); pos += 1; 1 }
    }
  }

  def tests = Tests {
    test("empty stream") {
      readDocuments("") ==> Seq()
      readDocuments("\n\n") ==> Seq()
      readDocuments("# comment\n# another") ==> Seq()
      readDocuments("...\n") ==> Seq()
    }
    test("single document") {
      readDocuments("a: b") ==> Seq(Obj("a" -> Str("b")))
      readDocuments("---\na: b") ==> Seq(Obj("a" -> Str("b")))
      readDocuments("---\na: b\n...\n") ==> Seq(Obj("a" -> Str("b")))
      readDocuments("a\n...\n") ==> Seq(Str("a"))
    }
    test("multiple documents") {
      readDocuments("a: b\n---\nc: d") ==> Seq(Obj("a" -> Str("b")), Obj("c" -> Str("d")))
      readDocuments("---\na: b\n---\nc: d\n") ==> Seq(Obj("a" -> Str("b")), Obj("c" -> Str("d")))
      readDocuments("a\n...\nb") ==> Seq(Str("a"), Str("b"))
      readDocuments("a\n...\n---\nb\n...\n") ==> Seq(Str("a"), Str("b"))
      readDocuments("- a\n---\n- b") ==> Seq(Arr(Str("a")), Arr(Str("b")))
      readDocuments("a:\n  b: c\n---\nd: e") ==> Seq(
        Obj("a" -> Obj("b" -> Str("c"))),
        Obj("d" -> Str("e"))
      )
      readDocuments("a:\n- x\n---\nb") ==> Seq(Obj("a" -> Arr(Str("x"))), Str("b"))
      readDocuments("a\nb\n---\nc") ==> Seq(Str("a b"), Str("c"))
      readDocuments("a: b\r\n---\r\nc: d\r\n") ==> Seq(Obj("a" -> Str("b")), Obj("c" -> Str("d")))
    }
    test("empty documents") {
      readDocuments("---") ==> Seq(Null())
      readDocuments("---\n---\n") ==> Seq(Null(), Null())
      readDocuments("a: b\n---\n") ==> Seq(Obj("a" -> Str("b")), Null())
      readDocuments("---\n# comment\n---\na") ==> Seq(Null(), Str("a"))
    }
    test("content on marker line") {
      readDocuments("--- a\n--- b") ==> Seq(Str("a"), Str("b"))
      readDocuments("--- # comment\na: b") ==> Seq(Obj("a" -> Str("b")))
      readDocuments("--- 'a'\n---\t\"b\"") ==> Seq(Str("a"), Str("b"))
    }
    test("block text") {
      readDocuments("--- |\n  x\n---\ny") ==> Seq(Str("x\n"), Str("y"))
      readDocuments("--- |\nx\ny\n---\nz") ==> Seq(Str("x\ny\n"), Str("z"))
      readDocuments("--- >\nx\ny\n...\n") ==> Seq(Str("x y\n"))
      readDocuments("--- |\n---\n") ==> Seq(Str(""), Null())
      readDocuments("--- |\nx\n----\n--- a\n") ==> Seq(Str("x\n----\n"), Str("a"))
      readDocuments("a: |\n  x\n---\nb") ==> Seq(Obj("a" -> Str("x\n")), Str("b"))
    }
    test("not markers") {
      read("----") ==> Str("----")
      read("---x") ==> Str("---x")
      read("....") ==> Str("....")
      read("..") ==> Str("..")
      read("--") ==> Str("--")
      read("a: ---") ==> Obj("a" -> Str("---"))
      read("a: ...") ==> Obj("a" -> Str("..."))
      read("- ---") ==> Arr(Str("---"))
      read("  ---") ==> Str("---")
      read("a\n  ---") ==> Str("a ---")
      read("'---'") ==> Str("---")
    }
    test("positions continue across documents") {
      val ex = assertThrows[ParseException](readDocuments("a: b\n---\nc: d\n e: f"))
      (ex.position.line, ex.position.col, ex.position.index) ==> (4, 2, 15)
    }
    test("read single document") {
      read("---\na: b") ==> Obj("a" -> Str("b"))
      read("---\na: b\n...\n") ==> Obj("a" -> Str("b"))
      read("---") ==> Null()
      read("a: b\n...\n# comment") ==> Obj("a" -> Str("b"))
      val ex = assertThrows[ParseException](read("a: b\n---\nc: d"))
      ex.message ==> "Expected a single document, but found another one. Use readDocuments to read multiple documents."
      (ex.position.line, ex.position.col) ==> (2, 1)
      assertThrows[ParseException](read("a: b\n---\n"))
      assertThrows[ParseException](read("a\n...\nb"))
    }
    test("trailing content") {
      // previously silently dropped
      val ex = assertThrows[ParseException](read("|\n  a\nb: c"))
      ex.message ==> "Expected end of document. Found: key"
      (ex.position.line, ex.position.col) ==> (3, 1)
    }
    test("buffer boundaries") {
      // place the marker across the parser's internal 8192 byte buffer boundary
      for (padding <- 8180 to 8200) {
        val source = "a: " + ("x" * padding) + "\n---\nb: c\n...\n"
        readDocuments(source) ==> Seq(
          Obj("a" -> Str("x" * padding)),
          Obj("b" -> Str("c"))
        )
      }
    }
    test("short reads") {
      val source = "a: ü😀\n---\n--- |\nx\n...\n"
      val in = new TrickleInputStream(source.getBytes("utf-8"))
      readDocuments(in) ==> Seq(Obj("a" -> Str("ü😀")), Null(), Str("x\n"))
    }
    test("write") {
      writeDocuments(Seq()) ==> ""
      writeDocuments(Seq(Obj("a" -> Str("b")), Str("x"))) ==> "---\na: b\n---\nx\n"
      writeDocuments(Seq(Null())) ==> "---\n\n"
    }
    test("write and read back") {
      val docs = Seq(
        Obj("a" -> Str("b"), "c" -> Arr(Str("d"), Null())),
        Arr(Str("x"), Obj("y" -> Str("z"))),
        Str("text"),
        Null(),
        Null(),
        Str("---"),
        Str("... x"),
        Str("---x"),
        Str(""),
        Obj("a" -> Str("---"))
      )
      readDocuments(writeDocuments(docs)) ==> docs

      val out = new java.io.ByteArrayOutputStream
      writeDocumentsToOutputStream(docs, out)
      readDocuments(out.toByteArray) ==> docs
    }
    test("write single marker-like string") {
      read(Str("---").render()) ==> Str("---")
      read(Str("...").render()) ==> Str("...")
    }
  }
}
