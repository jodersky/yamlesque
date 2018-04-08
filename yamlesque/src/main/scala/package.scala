package yamlesque

object `package` {
  implicit class RichString(val str: String) extends AnyVal {
    def parseYaml: YamlValue = YamlParser(str.toIterator)
  }
}
