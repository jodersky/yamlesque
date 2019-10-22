package yamlesque

import utest._
import java.io.StringReader

// test multiple documents
object StreamTest extends TestSuite {
  def tests = Tests {
    "empty doc" - {
      readAll("") ==> Null :: Nil
    }
    "empty doc, start only" - {
      // first --- is optional
      readAll("---") ==> Null :: Nil
    }
    "empty docs" - {
      readAll("---\n---") ==> Null :: Null :: Nil
      readAll("---\n---\n---") ==> Null :: Null :: Null :: Nil
    }
    "empty and non-empty docs" - {
      val s = """|---
                 |a
                 |---
                """.stripMargin
      readAll(s) ==> Str("a") :: Null :: Nil
    }
    "non-empty doc, implicit start" - {
      val s = """|a
                 |""".stripMargin
      readAll(s) ==> Str("a") :: Nil
    }
    "non-empty doc, explicit start" - {
      val s = """|---
                 |a
                 |""".stripMargin
      readAll(s) ==> Str("a") :: Nil
    }
    "non-empty docs, implicit start" - {
      val s = """|a
                 |---
                 |b
                """.stripMargin
      readAll(s) ==> Str("a") :: Str("b") :: Nil
    }
    "non-empty docs, explicit start" - {
      val s = """|---
                 |a
                 |---
                 |b
                """.stripMargin
      readAll(s) ==> Str("a") :: Str("b") :: Nil
    }
  }
}
