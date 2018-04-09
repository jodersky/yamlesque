package yamlesque

import utest._

object ParserTests extends TestSuite {

  val tests = Tests {
    "parse empty string" - {
      "".parseYaml ==> YamlEmpty
    }
    "parse simple scalar" - {
      "hello".parseYaml ==> YamlScalar("hello")
    }
    "parse scalar with space" - {
      "hello world".parseYaml ==> YamlScalar("hello world")
    }
    "parse scalar with a colon" - {
      "hello:world".parseYaml ==> YamlScalar("hello:world")
    }
    "parse scalar with a minus" - {
      "hello-world".parseYaml ==> YamlScalar("hello-world")
    }
    "parse scalar starting with a colon" - {
      ":hello world".parseYaml ==> YamlScalar(":hello world")
    }
    "parse scalar starting with a minus" - {
      "-hello world".parseYaml ==> YamlScalar("-hello world")
    }
    "parse empty list" - {
      "-".parseYaml ==> YamlSequence(YamlEmpty)
    }
    "parse a simple list" - {
      "-\n  a\n-\n  b\n-\n  c".parseYaml ==> YamlSequence(YamlScalar("a"),
                                                          YamlScalar("b"),
                                                          YamlScalar("c"))
    }
    "parse a simple compact list" - {
      "- a\n- b\n - c".parseYaml ==> YamlSequence(YamlScalar("a"),
                                                  YamlScalar("b"),
                                                  YamlScalar("c"))
    }
    "fail to parse a list with a non-item token" - {
      val e = intercept[ParseException] {
        "- a\n- b\n -c".parseYaml // -c is missing a space between '-' and 'c'
      }
      assert(e.message.contains("token kind"))
    }
    "parse a nested list" - {
      val ls =
        s"""|- a0
            |- b0
            |-
            |  - a1
            |  - b1
            |  -
            |    - a2
            |    - b2
            |- c0
            |- - a1
            |  - b1
            |- - - - a4
            |""".stripMargin
      val result = YamlSequence(
        YamlScalar("a0"),
        YamlScalar("b0"),
        YamlSequence(
          YamlScalar("a1"),
          YamlScalar("b1"),
          YamlSequence(
            YamlScalar("a2"),
            YamlScalar("b2")
          )
        ),
        YamlScalar("c0"),
        YamlSequence(
          YamlScalar("a1"),
          YamlScalar("b1")
        ),
        YamlSequence(
          YamlSequence(
            YamlSequence(
              YamlScalar("a4")
            )
          )
        )
      )
      ls.parseYaml ==> result
    }
    "parse a simple mapping" - {
      "a:\n  b".parseYaml ==> YamlMapping("a" -> YamlScalar("b"))
    }
    "parse a double mapping" - {
      "a:\n  b\nc:\n  d".parseYaml ==> YamlMapping(
        "a" -> YamlScalar("b"),
        "c" -> YamlScalar("d")
      )
    }
    "parse a simple compact mapping" - {
      "a: b".parseYaml ==> YamlMapping("a" -> YamlScalar("b"))
    }
    "parse a double compact mapping" - {
      "a: b\nc: d".parseYaml ==> YamlMapping(
        "a" -> YamlScalar("b"),
        "c" -> YamlScalar("d")
      )
    }
    "parse a simple mapping without a value" - {
      "a:\n".parseYaml ==> YamlMapping(
        "a" -> YamlEmpty
      )
    }
    "parse a mapping without a value" - {
      "k1: v1\nk2:\nk3: v3".parseYaml ==> YamlMapping(
        "k1" -> YamlScalar("v1"),
        "k2" -> YamlEmpty,
        "k3" -> YamlScalar("v3")
      )
    }
    "parse a nested mapping" - {
      val m =
        s"""|k1:
            |  k11: a
            |  k12: b
            |k2:
            |  k21:
            |    k31:
            |      k41: a
            |  k22:
            |    b
            |k3: a
            |k4: k41: k42: k43: a
            |""".stripMargin
      m.parseYaml ==> YamlMapping(
        "k1" -> YamlMapping(
          "k11" -> YamlScalar("a"),
          "k12" -> YamlScalar("b")
        ),
        "k2" -> YamlMapping(
          "k21" -> YamlMapping(
            "k31" -> YamlMapping(
              "k41" -> YamlScalar("a")
            )
          ),
          "k22" -> YamlScalar("b")
        ),
        "k3" -> YamlScalar("a"),
        "k4" -> YamlMapping(
          "k41" -> YamlMapping(
            "k42" -> YamlMapping(
              "k43" -> YamlScalar("a")
            )
          )
        )
      )
    }
    "maps and sequences" - {
      val yaml = YamlMapping(
        "key1" -> YamlScalar("value1"),
        "key2" -> YamlMapping(
          "key1" -> YamlScalar("value1"),
          "key2" -> YamlScalar("value1"),
          "key3" -> YamlSequence(
            YamlScalar("a1"),
            YamlSequence(
              YamlScalar("a1"),
              YamlScalar("a2"),
              YamlScalar("a3")
            ),
            YamlScalar("a3"),
            YamlMapping(
              "a1" -> YamlScalar("b"),
              "a2" -> YamlScalar("b"),
              "a3" -> YamlScalar("b"),
              "a4" -> YamlScalar("b")
            ),
            YamlScalar("a4"),
            YamlScalar("a4")
          ),
          "key4" -> YamlScalar("value1"),
          "key5" -> YamlScalar("value1"),
          "key6" -> YamlScalar("value1")
        ),
        "key3" -> YamlScalar("value3")
      )

      val string =
        s"""|
            |key1: value1
            |key2:
            |  key4:
            |    value1
            |  key5: value1
            |  key1: value1
            |  key2: value1
            |  key6: value1
            |  key3:
            |    - a1
            |    -
            |      - a1
            |      - a2
            |      - a3
            |    - a3
            |    -
            |      a1: b
            |      a2: b
            |      a3: b
            |      a4: b
            |    - a4
            |    - a4
            |key3: value3
            |""".stripMargin
      "parse" - {
        string.parseYaml ==> yaml
      }
      "print and parse" - {
        yaml.print.parseYaml ==> yaml
      }
    }
  }

}
