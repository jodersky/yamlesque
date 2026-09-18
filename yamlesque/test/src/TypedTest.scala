import utest._

object TypedTest extends TestSuite {
  import yamlesque._

  def tests = Tests {
    test("booleans") {
      read("true") ==> Bool(true)
      read("True") ==> Bool(true)
      read("TRUE") ==> Bool(true)
      read("false") ==> Bool(false)
      read("False") ==> Bool(false)
      read("FALSE") ==> Bool(false)
      read("a: true\nb: false") ==> Obj("a" -> Bool(true), "b" -> Bool(false))
      read("- true\n- FALSE\n- True\n- False") ==> Arr(Bool(true), Bool(false), Bool(true), Bool(false))
      read("a: true # comment") ==> Obj("a" -> Bool(true))
    }
    test("boolean-like strings") {
      // only true/false, True/False and TRUE/FALSE are booleans; no Norway problem
      for (s <- Seq("tRUE", "fAlse", "TRue", "yes", "no", "Yes", "NO", "on", "off", "y", "n")) {
        read(s) ==> Str(s)
      }
      read("a: \"true\"") ==> Obj("a" -> Str("true"))
      read("a: 'false'") ==> Obj("a" -> Str("false"))
      read("a: |\n  true\n") ==> Obj("a" -> Str("true\n"))
      read("a: true story") ==> Obj("a" -> Str("true story"))
      read("a: true\n   story") ==> Obj("a" -> Str("true story"))
      read("true: x") ==> Obj("true" -> Str("x")) // keys are always strings
    }
    test("integers") {
      read("0") ==> Num(0)
      read("42") ==> Num(42)
      read("-17") ==> Num(-17)
      read("+17") ==> Num(17)
      read("007") ==> Num(7)
      read("a: 80") ==> Obj("a" -> Num(80))
      read("- 1\n- 2") ==> Arr(Num(1), Num(2))
    }
    test("floats") {
      read("1.5") ==> Num(1.5)
      read("-1.5") ==> Num(-1.5)
      read("1.") ==> Num(1)
      read(".5") ==> Num(0.5)
      read("-.5") ==> Num(-0.5)
      read("1e3") ==> Num(1000)
      read("1E3") ==> Num(1000)
      read("2.5e-3") ==> Num(0.0025)
      read("+1.5e+2") ==> Num(150)
    }
    test("hex and octal") {
      read("0x1F") ==> Num(31)
      read("0xff") ==> Num(255)
      read("0o17") ==> Num(15)
      read("0x") ==> Str("0x")
      read("0o8") ==> Str("0o8")
      read("0X1F") ==> Str("0X1F") // only lowercase prefixes
      read("-0x1F") ==> Str("-0x1F")
    }
    test("infinity and nan") {
      for (s <- Seq(".inf", ".Inf", ".INF", "+.inf")) read(s) ==> Num(Double.PositiveInfinity)
      for (s <- Seq("-.inf", "-.Inf", "-.INF")) read(s) ==> Num(Double.NegativeInfinity)
      for (s <- Seq(".nan", ".NaN", ".NAN")) assert(read(s).num.isNaN)
      read("inf") ==> Str("inf")
      read("NaN") ==> Str("NaN")
      read("-.nan") ==> Str("-.nan")
      read(".iNf") ==> Str(".iNf")
    }
    test("number-like strings") {
      for (s <- Seq("1.2.3", "1_000", "1,000", "0.0.0.0", "1e", "e3", ".", "+", "1-2", "12a", "0b101", "1 2")) {
        read(s"a: $s") ==> Obj("a" -> Str(s))
      }
      read("a: \"42\"") ==> Obj("a" -> Str("42"))
      read("a: '42'") ==> Obj("a" -> Str("42"))
      read("a: |\n  42\n") ==> Obj("a" -> Str("42\n"))
      read("a: 1\n   2") ==> Obj("a" -> Str("1 2")) // multi-line scalar
      read("42: x") ==> Obj("42" -> Str("x")) // keys are always strings
    }
    test("large integers") {
      // Values store Doubles, so precision beyond 2^53 is lost
      read("12345678901234567890") ==> Num(12345678901234567890.0)
      read("0xFFFFFFFFFFFFFFFFFF") ==> Num(BigInt("FFFFFFFFFFFFFFFFFF", 16).toDouble)
    }
    test("visitors see the original text") {
      val texts = collection.mutable.ArrayBuffer.empty[String]
      val visitor = new ValueBuilder {
        override def visitNumber(ctx: Ctx, text: CharSequence): Value = {
          texts += text.toString
          super.visitNumber(ctx, text)
        }
      }
      val in = new java.io.ByteArrayInputStream("12345678901234567890".getBytes("utf-8"))
      new Parser(in, "virtual").parseSingleDocument(visitor)
      texts.toList ==> List("12345678901234567890")
    }
    test("accessors") {
      read("1.5").num ==> 1.5
      read("1.5").numOpt ==> Some(1.5)
      read("a").numOpt ==> None
      read("true").bool ==> true
      read("true").boolOpt ==> Some(true)
      read("1").boolOpt ==> None
      assertThrows[Value.InvalidData](read("a").num)
      assertThrows[Value.InvalidData](read("1").bool)
      assertThrows[Value.InvalidData](read("1").str)
    }
    test("render") {
      Obj("a" -> Num(80)).render() ==> "a: 80"
      Obj("a" -> Num(1.5)).render() ==> "a: 1.5"
      Obj("a" -> Num(-3)).render() ==> "a: -3"
      Obj("a" -> Num(Double.NaN)).render() ==> "a: .nan"
      Obj("a" -> Num(Double.PositiveInfinity)).render() ==> "a: .inf"
      Obj("a" -> Num(Double.NegativeInfinity)).render() ==> "a: -.inf"
      Obj("a" -> Bool(true), "b" -> Bool(false)).render() ==> "a: true\nb: false"
      Obj("a" -> Str("80"), "b" -> Str("true"), "c" -> Str(".inf"), "d" -> Str("False")).render() ==>
        "a: '80'\nb: 'true'\nc: '.inf'\nd: 'False'"
      Obj("a" -> Str("1.2.3"), "b" -> Str("yes")).render() ==> "a: 1.2.3\nb: yes"
    }
    test("read back") {
      val value = Obj(
        "ints" -> Arr(Num(0), Num(42), Num(-17), Num(1e15), Num(1e20), Num(-0.0)),
        "floats" -> Arr(Num(1.5), Num(-2.25), Num(1e-7), Num(1.2345e300), Num(Double.MinPositiveValue)),
        "special" -> Arr(Num(Double.PositiveInfinity), Num(Double.NegativeInfinity)),
        "bools" -> Arr(Bool(true), Bool(false)),
        "strings" -> Arr(Str("42"), Str("1.5"), Str("true"), Str("True"), Str("FALSE"), Str("0x1F"), Str(".nan"), Str("yes"))
      )
      read(value.render()) ==> value
      assert(read(Num(Double.NaN).render()).num.isNaN)
    }
    test("multiple documents") {
      readDocuments("--- 1\n--- true\n--- x") ==> Seq(Num(1), Bool(true), Str("x"))
      val docs = Seq(Num(1.5), Bool(false), Str("2"))
      readDocuments(writeDocuments(docs)) ==> docs
    }
  }
}
