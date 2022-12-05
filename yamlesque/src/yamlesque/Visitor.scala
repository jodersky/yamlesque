package yamlesque

trait Ctx {
  def pos: Position
  def line: String
}

trait Visitor[+T]{
  def visitObject(ctx: Ctx): ObjectVisitor[T]
  def visitArray(ctx: Ctx): ArrayVisitor[T]
  def visitEmpty(ctx: Ctx): T

  def visitString(ctx: Ctx, text: CharSequence): T
  def visitQuotedString(ctx: Ctx, text: CharSequence): T
  def visitBlockStringLiteral(ctx: Ctx, text: CharSequence): T
  def visitBlockStringFolded(ctx: Ctx, text: CharSequence): T
}
trait ObjectVisitor[+T]{
  def visitKey(ctx: Ctx, key: String): Unit
  def subVisitor(): Visitor[_]
  def visitValue(ctx: Ctx, value: Any): Unit
  def visitEnd(): T
}
trait ArrayVisitor[+T]{
  def visitIndex(ctx: Ctx, idx: Int): Unit
  def subVisitor(): Visitor[_]
  def visitValue(ctx: Ctx, value: Any): Unit
  def visitEnd(): T
}

class ValueBuilder() extends Visitor[Value] {
  var value: Value = Null()

  def visitObject(ctx: Ctx): ObjectVisitor[Value] = new ObjectBuilder()
  def visitArray(ctx: Ctx): ArrayVisitor[Value] = new ArrayBuilder()
  def visitEmpty(ctx: Ctx): Value = Null()

  def visitString(ctx: Ctx, text: CharSequence): Value = Str(text.toString())

  def visitQuotedString(ctx: Ctx, text: CharSequence): Value = Str(text.toString())
  def visitBlockStringLiteral(ctx: Ctx, text: CharSequence) = Str(text.toString())
  def visitBlockStringFolded(ctx: Ctx, text: CharSequence) = Str(text.toString())
}

class ObjectBuilder() extends ObjectVisitor[Value] {
  val obj = Obj()
  private var _key: String = _

  def visitKey(ctx: Ctx, key: String): Unit = _key = key
  def subVisitor(): Visitor[Value] =  new ValueBuilder()
  def visitValue(ctx: Ctx, value: Any): Unit = {
    obj.values += _key -> value.asInstanceOf[Value]
  }
  def visitEnd(): Value = obj
}

class ArrayBuilder() extends ArrayVisitor[Value] {
  val arr = Arr()
  var _idx = 0

  def visitIndex(ctx: Ctx, idx: Int): Unit = _idx = idx
  def subVisitor(): Visitor[Value] = new ValueBuilder()
  def visitValue(ctx: Ctx, value: Any): Unit = arr.values.insert(_idx, value.asInstanceOf[Value])
  def visitEnd(): Value = arr
}


// class Printer(out: java.io.PrintStream, indent: Int) extends Visitor[Unit] {
//   def visitObject(): ObjectVisitor[Unit] = new ObjectPrinter(out, indent + 1)
//   def visitArray(): ArrayVisitor[Unit] = new ArrayPrinter(out, indent + 1)
//   def visitEmpty(): Unit = out.println("")

//   def visitString(text: CharSequence): Unit = {
//     for (line <- text.toString().linesIterator) {
//       for (_ <- 0 until indent) out.print("  ")
//       out.println(text)
//     }
//   }

//   def visitBlockStringLiteral(text: CharSequence): Unit = visitString(text)
//   def visitBlockStringFolded(text: CharSequence): Unit = visitString(text)
//   def visitQuotedString(text: CharSequence): Unit = visitString(text)
// }

// class ArrayPrinter(out: java.io.PrintStream, indent: Int) extends ArrayVisitor[Unit] {
//   def visitIndex(idx: Int): Unit = {
//     for (_ <- 0 until indent) out.print("  ")
//     out.println("- ")
//   }

//   def subVisitor(): Visitor[Unit] = new Printer(out, indent + 1)
//   def visitValue(value: Unit): Unit = ()
//   def visitEnd(): Unit = ()
// }


// class ObjectPrinter(out: java.io.PrintStream, indent: Int) extends ObjectVisitor[Unit] {
//   def visitKey(key: String): Unit = {
//     for (_ <- 0 until indent) out.print("  ")
//     out.print(key)
//     out.println(": ")
//   }
//   def subVisitor(): Visitor[Unit] = new Printer(out, indent + 1)
//   def visitValue(value: Unit): Unit = ()
//   def visitEnd(): Unit = ()
// }


class CompactPrinter(out0: java.io.OutputStream) extends Visitor[Unit] with ArrayVisitor[Unit] with ObjectVisitor[Unit] {
  val Indent = 2
  val out = new java.io.PrintStream(out0)

  private val cols = collection.mutable.Stack.empty[Int]
  private def col = cols.head
  private val isMaps = collection.mutable.Stack.empty[Boolean]
  private def isMap = isMaps.head

  cols.push(0)
  isMaps.push(false)

  def subVisitor() = this
  def visitEnd(): Unit = {}
  def visitIndex(ctx: Ctx, idx: Int): Unit = {
    out.println()
    for (_ <- 0 until col) out.print(' ')
    out.print("- ")
    cols.push(col + Indent)
    isMaps.push(false)
  }

  def visitKey(ctx: Ctx, key: String): Unit = {
    if (isMap) {
      out.println()
      for (_ <- 0 until col) out.print(' ')
    }
    isMaps.pop()
    isMaps.push(true)
    out.print(key)
    out.print(": ")
    cols.push(col + Indent)
    isMaps.push(true)
  }

  def visitValue(ctx: Ctx, value: Any): Unit = {
    cols.pop()
    isMaps.pop()
  }

  def visitArray(ctx: Ctx): ArrayVisitor[Unit] = {
    this
  }
  def visitObject(ctx: Ctx): ObjectVisitor[Unit] = {
    this
  }

  def visitEmpty(ctx: Ctx): Unit = ()

  def visitBlockStringFolded(ctx: Ctx, text: CharSequence): Unit = visitString(ctx, text)
  def visitBlockStringLiteral(ctx: Ctx,text: CharSequence): Unit = visitString(ctx, text)

  def visitQuotedString(ctx: Ctx,text: CharSequence): Unit = visitString(ctx, text)

  // TODO: handle multi-line text
  def visitString(ctx: Ctx, text: CharSequence): Unit = out.print(text)

}
