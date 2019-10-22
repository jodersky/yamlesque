package yamlesque

object Writer {

  def write(nodes: Iterable[Node]): String = {
    val buffer = new StringBuilder
    write(nodes, buffer)
    buffer.result()
  }
  def write(nodes: Iterable[Node], buffer: StringBuilder): Unit = {
    val it = nodes.iterator
    while (it.hasNext) {
      writeCompact(buffer, true, 0, it.next())
      if (it.hasNext) buffer ++= "---\n"
    }
  }

  private def writeCompact(
      buffer: StringBuilder,
      startOfLine: Boolean,
      indent: Int,
      node: Node
  ): Unit = {
    node match {
      case Null =>
        buffer ++= "null\n"
      case Bool(true) =>
        buffer ++= "true\n"
      case Bool(false) =>
        buffer ++= "false\n"
      case Num(num) =>
        buffer ++= num.toString
        buffer += '\n'
      case Str(value) =>
        buffer ++= value
        buffer += '\n'
      case Arr(values) =>
        var doIndent = startOfLine
        for (item <- values) {
          if (doIndent) {
            buffer ++= "  " * indent
          }
          doIndent = true
          item match {
            case Arr(_) =>
              buffer ++= "-\n"
              writeCompact(buffer, true, indent + 1, item)
            case _ =>
              buffer ++= "- "
              writeCompact(buffer, false, indent + 1, item)
          }
        }
      case Obj(values) =>
        var doIndent = startOfLine
        for ((key, value) <- values) {
          if (doIndent) {
            buffer ++= "  " * indent
          }
          doIndent = true

          buffer ++= key
          value match {
            case Str(_) | Bool(_) | Num(_) | Null =>
              buffer ++= ": "
              writeCompact(buffer, false, indent, value)
            case _ =>
              buffer ++= ":\n"
              writeCompact(buffer, true, indent + 1, value)
          }
        }
    }
  }

}
