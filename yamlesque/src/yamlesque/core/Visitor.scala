package yamlesque.core

trait Visitor[T]{
  def visitObject(): ObjectVisitor[T]
  def visitArray(): ArrayVisitor[T]
  def visitString(text: CharSequence): Visitor[T]
  def visitComment(text: CharSequence): Visitor[T]
}
trait ObjectVisitor[T]{
  def visitKey(key: String): Unit
  def subVisitor(): Visitor[T]
  def visitValue(value: T): Unit
  def visitEnd(): T
}

trait ArrayVisitor[T]{
  def visitIndex(idx: Int): Unit
  def subVisitor(): Visitor[T]
  def visitValue(value: T): Unit
  def visitEnd(): T
}
