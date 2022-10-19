package yamlesque.upickle

import yamlesque.{ArrayVisitor, ObjectVisitor, Visitor}

class UjsonDelegate[A](d: upickle.core.Visitor[_, A]) extends Visitor[A] {

  def visitBlockStringFolded(text: CharSequence): A = d.visitString(text, -1)
  def visitBlockStringLiteral(text: CharSequence): A = d.visitString(text, -1)
  def visitEmpty(): A = d.visitNull(-1)
  def visitQuotedString(text: CharSequence): A = d.visitString(text, -1)

  def visitArray(): ArrayVisitor[A] = new UjsonArrayDelegate(d.visitArray(0, -1))

  def visitObject(): ObjectVisitor[A] = new UjsonObjectDelegate[A](d.visitObject(0, -1))
  def visitString(text: CharSequence): A = text match {
    case "null" => d.visitNull(-1)
    case "true" => d.visitTrue(-1)
    case "false" => d.visitFalse(-1)
    case _ =>
      try {
        val x = java.lang.Double.parseDouble(text.toString)
        d.visitFloat64(x, -1)
      } catch {
        case _: NumberFormatException =>
        d.visitString(text, -1)
      }
  }
}

class UjsonArrayDelegate[A](d: upickle.core.ArrVisitor[_, A]) extends ArrayVisitor[A] {
  override def visitIndex(idx: Int): Unit = ()
  override def subVisitor(): Visitor[A] = {
    new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  }
  override def visitValue(value: Any): Unit = d.narrow.visitValue(value, -1)
  override def visitEnd(): A = d.visitEnd(-1)
}

class UjsonObjectDelegate[A](d: upickle.core.ObjVisitor[_, A]) extends ObjectVisitor[A] {
  def visitKey(key: String): Unit = d.visitKeyValue(d.visitKey(-1).visitString(key, -1))
  def subVisitor(): Visitor[A] = new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  def visitValue(value: Any): Unit = d.narrow.visitValue(value, -1)
  def visitEnd(): A = d.visitEnd(-1)

}
