package yamlesque

import scala.collection.mutable

sealed trait Node {
  def isObj: Boolean = false
  def isArr: Boolean = false
  def isStr: Boolean = false
  def isNum: Boolean = false
  def isBool: Boolean = false
  def isNull: Boolean = false

  /** Returns the key-value map of this node. Fails if this is not a [[Obj]]. */
  def obj: mutable.Map[String, Node] = sys.error("not an object")
  def arr: mutable.ArrayBuffer[Node] = sys.error("not an array")
  def str: String = sys.error("not a string")
  def num: Double = sys.error("not a number")
  def bool: Boolean = sys.error("not a boolean")

}
object Node {
  import scala.language.implicitConversions
  // implicit def SeqToYaml[T](items: IterableOnce[T])
  //                            (implicit f: T => Node) = Arr.from(items.map(f))
  // implicit def JsonableDict[T](items: TraversableOnce[(String, T)])
  //                             (implicit f: T => Value)= Obj.from(items.map(x => (x._1, f(x._2))))
  implicit def StringToYaml(s: CharSequence): Str = Str(s.toString)
  implicit def ByteToYaml(x: Byte): Num = Num(x)
  implicit def ShortToYaml(x: Short): Num = Num(x)
  implicit def IntToYaml(x: Int): Num = Num(x)
  implicit def LongToYaml(x: Long): Num = Num(x)
  implicit def FloatToYaml(x: Float): Num = Num(x)
  implicit def DoubleToYaml(x: Double): Num = Num(x)
  implicit def BoolToYaml(x: Boolean): Bool = Bool(x)
  implicit def NullToYaml(x: scala.Null): Null.type = Null
}

case class Obj(override val obj: mutable.LinkedHashMap[String, Node])
    extends Node {
  override def isObj = true
}
object Obj {
  def apply(values: (String, Node)*): Obj = {
    val builder = mutable.LinkedHashMap.newBuilder[String, Node]
    builder.sizeHint(values.length)
    for (v <- values) {
      builder += v
    }
    Obj(builder.result())
  }
}

case class Arr(override val arr: mutable.ArrayBuffer[Node]) extends Node {
  override def isArr = true
}
object Arr {
  def apply(values: Node*): Arr = {
    val builder = mutable.ArrayBuffer.newBuilder[Node]
    builder.sizeHint(values.length)
    for (v <- values) {
      builder += v
    }
    Arr(builder.result())
  }
}

case class Str(override val str: String) extends Node {
  override def isStr: Boolean = true
}

case class Num(override val num: Double) extends Node {
  override def isNum: Boolean = true
}

case class Bool(override val bool: Boolean) extends Node {
  override def isBool: Boolean = true
}

case object Null extends Node {
  override def isNull = true
}
