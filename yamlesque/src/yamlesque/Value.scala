package yamlesque

sealed trait Value
case class Str(value: String) extends Value
case class Arr(value: collection.mutable.ArrayBuffer[Value]) extends Value
case class Obj(value: collection.mutable.LinkedHashMap[String, Value]) extends Value

