package yamlesque

import annotation.tailrec

class YamlPrinter(compact: Boolean = true) extends (YamlValue => String) {

  def apply(value: YamlValue): String = {
    val str = new StringBuilder()
    def p(value: YamlValue, indentation: Int): Unit = value match {
      case YamlScalar(value) =>
        str ++= "  " * indentation
        str ++= value
        str += '\n'
      case YamlSequence(items) =>
        for (item <- items) {
          str ++= "  " * indentation
          item match {
            case YamlScalar(v) if compact =>
              str ++= "- "
              str ++= v
              str += '\n'
            case _ =>
              str ++= "-\n"
              p(item, indentation + 1)
          }
        }
      case YamlMapping(fields) =>
        for ((key, value) <- fields) {
          str ++= "  " * indentation
          str ++= key
          value match {
            case YamlScalar(v) if compact =>
              str ++= ": "
              str ++= v
              str += '\n'
            case _ =>
              str ++= ":\n"
              p(value, indentation + 1)
          }
        }
      case YamlEmpty =>
        str += '\n'
    }
    p(value, 0)
    str.toString
  }

}
