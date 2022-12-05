package yamlesque.upickle

import yamlesque.{ArrayVisitor, Ctx, ObjectVisitor, Visitor}

class UjsonDelegate[A](d: upickle.core.Visitor[_, A]) extends Visitor[A] {

  def visitBlockStringFolded(ctx: Ctx, text: CharSequence): A = d.visitString(text, -1)
  def visitBlockStringLiteral(ctx: Ctx, text: CharSequence): A = d.visitString(text, -1)
  def visitEmpty(ctx: Ctx): A = d.visitNull(-1)
  def visitQuotedString(ctx: Ctx, text: CharSequence): A = d.visitString(text, -1)

  def visitArray(ctx: Ctx): ArrayVisitor[A] = new UjsonArrayDelegate(d.visitArray(0, -1))

  def visitObject(ctx: Ctx): ObjectVisitor[A] = new UjsonObjectDelegate[A](d.visitObject(0, -1))
  def visitString(ctx: Ctx, text: CharSequence): A = text match {
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
  override def visitIndex(ctx: Ctx, idx: Int): Unit = ()
  override def subVisitor(): Visitor[A] = {
    new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  }
  override def visitValue(ctx: Ctx, value: Any): Unit = d.narrow.visitValue(value, -1)
  override def visitEnd(): A = d.visitEnd(-1)
}

class UjsonObjectDelegate[A](d: upickle.core.ObjVisitor[_, A]) extends ObjectVisitor[A] {
  def visitKey(ctx: Ctx, key: String): Unit = d.visitKeyValue(d.visitKey(-1).visitString(key, -1))
  def subVisitor(): Visitor[A] = new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  def visitValue(ctx: Ctx, value: Any): Unit = d.narrow.visitValue(value, -1)
  def visitEnd(): A = d.visitEnd(-1)
}
