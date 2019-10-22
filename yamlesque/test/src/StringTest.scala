package yamlesque

import utest._

object StringTest extends TestSuite {
  def tests = Tests {
    "quoted simple" - {
      read(""""a"""") ==> Str("a")
      read(""" "a" """) ==> Str("a")
    }
    "quoted non-strings" - {
      read(""""1"""") ==> Str("1")
      read(""""1.2"""") ==> Str("1.2")
      read(""""true"""") ==> Str("true")
      read(""""false"""") ==> Str("false")
      read(""""null"""") ==> Str("null")
    }
    "quoted comment" - {
      read(""""#hello"""") ==> Str("#hello")
      read(""""a #hello"""") ==> Str("a #hello")
    }
    "scalar with quote" - {
      read(""" a" """) ==> Str("a\"")
      read(""" a"hmm" """) ==> Str("a\"hmm\"")
      read(""" -"a" """) ==> Str("-\"a\"")
      read(""" :"a" """) ==> Str(":\"a\"")
    }
    "quoted key" - {
      read(""" "a # b": a """) ==> Obj("a # b" -> Str("a"))
      read(""" "a # b" : a """) ==> Obj("a # b" -> Str("a"))
    }
  }
}
