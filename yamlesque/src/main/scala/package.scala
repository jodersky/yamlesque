package yamlesque

object `package` {

  def deserializationError(msg: String,
                           cause: Throwable = null,
                           fieldNames: List[String] = Nil) =
    throw new DeserializationException(msg, cause, fieldNames)
  def serializationError(msg: String) = throw new SerializationException(msg)

  implicit class RichAny[A](val any: A) extends AnyVal {
    def toYaml(implicit writer: YamlWriter[A]): YamlValue = writer.write(any)
  }

  implicit class RichString(val str: String) extends AnyVal {
    def parseYaml: YamlValue = YamlParser(str.toIterator)
  }

}

case class DeserializationException(msg: String,
                                    cause: Throwable = null,
                                    fieldNames: List[String] = Nil)
    extends RuntimeException(msg, cause)

class SerializationException(msg: String) extends RuntimeException(msg)
