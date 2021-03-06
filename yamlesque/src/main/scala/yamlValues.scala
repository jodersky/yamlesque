package yamlesque

sealed trait YamlValue {
  def print: String = YamlValue.DefaultPrinter(this)
  def convertTo[A: YamlReader]: A = implicitly[YamlReader[A]].read(this)
}
object YamlValue {
  val DefaultPrinter = new YamlPrinter(compact = true)
}

case class YamlMapping(fields: Map[String, YamlValue]) extends YamlValue
object YamlMapping {
  def apply(items: (String, YamlValue)*) = new YamlMapping(Map(items: _*))
}

case class YamlSequence(items: Vector[YamlValue]) extends YamlValue
object YamlSequence {
  def apply(items: YamlValue*) = new YamlSequence(items.toVector)
}

case class YamlScalar(value: String) extends YamlValue

case object YamlEmpty extends YamlValue
