package yamlesque

import spray.json._

trait JsonYamlFormats {

  implicit def jsonToYamlReader[A](implicit jsReader: JsonReader[A]): YamlReader[A] = new YamlReader[A] {
    override def read(yaml: YamlValue): A = jsReader.read(JsonFormats.yamlToJson(yaml))
  }

  implicit def jsonToYamlWriter[A](implicit jsWriter: JsonWriter[A]): YamlWriter[A] = new YamlWriter[A] {
    override def write(a: A): YamlValue = JsonFormats.jsonToYaml(jsWriter.write(a))
  }

}

object JsonFormats {

  def jsonToYaml(js: JsValue): YamlValue = js match {
    case JsNull => YamlScalar.Empty
    case JsNumber(number) => YamlScalar(number.toString)
    case JsBoolean(value) => YamlScalar(value.toString)
    case JsString(value) => YamlScalar(value)
    case JsArray(elements) => YamlSequence(elements.map(jsonToYaml _ ))
    case JsObject(fields) => YamlMapping(fields.mapValues(jsonToYaml _ ))
  }

  val JsNumberPattern = """([-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?)""".r

  def yamlToJson(yaml: YamlValue): JsValue = yaml match {
    case YamlScalar.Empty => JsNull
    case YamlScalar("true") => JsTrue
    case YamlScalar("false") => JsFalse
    case YamlScalar(JsNumberPattern(x)) => JsNumber(x.toDouble)
    case YamlScalar(x) => JsString(x)
    case YamlSequence(elements) => JsArray(elements.map(yamlToJson))
    case YamlMapping(fields) => JsObject(fields.mapValues(yamlToJson))
  }

}
