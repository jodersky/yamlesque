package yamlesque.upickle

import yamlesque.{ArrayVisitor, Ctx, ObjectVisitor, Visitor}

class UjsonDelegate[A](d: upickle.core.Visitor[_, A]) extends Visitor[A] {

  def visitBlockStringFolded(ctx: Ctx, text: CharSequence): A = d.visitString(text, ctx.pos.index)
  def visitBlockStringLiteral(ctx: Ctx, text: CharSequence): A = d.visitString(text, ctx.pos.index)
  def visitEmpty(ctx: Ctx): A = d.visitNull(ctx.pos.index)
  def visitQuotedString(ctx: Ctx, text: CharSequence): A = d.visitString(text, ctx.pos.index)

  def visitArray(ctx: Ctx): ArrayVisitor[A] = new UjsonArrayDelegate(d.visitArray(0, ctx.pos.index))

  def visitObject(ctx: Ctx): ObjectVisitor[A] = new UjsonObjectDelegate[A](d.visitObject(0, ctx.pos.index))
  def visitString(ctx: Ctx, text: CharSequence): A = text match {
    case "true" => d.visitTrue(ctx.pos.index)
    case "false" => d.visitFalse(ctx.pos.index)
    case _ =>
      try {
        val x = java.lang.Double.parseDouble(text.toString)
        d.visitFloat64(x, ctx.pos.index)
      } catch {
        case _: NumberFormatException =>
        d.visitString(text, ctx.pos.index)
      }
  }
}

class UjsonArrayDelegate[A](d: upickle.core.ArrVisitor[_, A]) extends ArrayVisitor[A] {
  override def visitIndex(ctx: Ctx, idx: Int): Unit = ()
  override def subVisitor(): Visitor[A] = {
    new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  }
  override def visitValue(ctx: Ctx, value: Any): Unit = d.narrow.visitValue(value, ctx.pos.index)
  override def visitEnd(): A = d.visitEnd(-1)
}

class UjsonObjectDelegate[A](d: upickle.core.ObjVisitor[_, A]) extends ObjectVisitor[A] {
  def visitKey(ctx: Ctx, key: String): Unit = d.visitKeyValue(d.visitKey(ctx.pos.index).visitString(key, ctx.pos.index))
  def subVisitor(): Visitor[A] = new UjsonDelegate[A](d.subVisitor.asInstanceOf[upickle.core.Visitor[_, A]])
  def visitValue(ctx: Ctx, value: Any): Unit = d.narrow.visitValue(value, ctx.pos.index)
  def visitEnd(): A = d.visitEnd(-1)
}
