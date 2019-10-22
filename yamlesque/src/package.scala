package object yamlesque {
  import java.io.StringReader

  def read(input: String): Node = {
    (new Parser(new StringReader(input))).next()
  }

  def tryRead(input: String): Either[String, Node] =
    try {
      Right(read(input))
    } catch {
      case Parser.ParseException(msg) => Left(msg)
    }

  def readAll(input: String): List[Node] = {
    (new Parser(new StringReader(input))).toList
  }

  // TODO: the parser can actually recover from errors when a new document begins
  def tryReadAll(input: String): Either[String, List[Node]] =
    try {
      Right((new Parser(new StringReader(input))).toList)
    } catch {
      case Parser.ParseException(msg) => Left(msg)
    }

  def write(nodes: Node*): String = write(nodes)
  def write(nodes: Iterable[Node]): String = Writer.write(nodes)

}
