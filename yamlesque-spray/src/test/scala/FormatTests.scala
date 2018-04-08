package yamlesque

import spray.json._
import utest._

object FormatTests extends TestSuite {

  case class A(a1: Int, a2: Seq[B])
  case class B(a: String, b: Option[Boolean])

  object Protocol extends DefaultJsonProtocol with JsonYamlFormats {
    implicit def bFormat = jsonFormat2(B)
    implicit def aFormat = jsonFormat2(A)
  }
  import Protocol._

  val tests = Tests {
    "parse" - {
      val str =
        s"""|a1: 42
            |a2:
            |  - a: hello world
            |    b: true
            |  - a: yoyo
            |""".stripMargin

      "parse yaml" - {
        str.parseYaml ==> YamlMapping(
          "a1" -> YamlScalar("42"),
          "a2" -> YamlSequence(
            YamlMapping(
              "a" -> YamlScalar("hello world"),
              "b" -> YamlScalar("true")
            ),
            YamlMapping(
              "a" -> YamlScalar("yoyo")
            )
          )
        )
      }
      "parse with json readers" - {
        str.parseYaml.convertTo[A] ==> A(
          42,
          Seq(
            B("hello world", Some(true)),
            B("yoyo", None),
          )
        )
      }
    }
  }

}
