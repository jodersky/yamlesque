package object yamlesque {
  def read(readable: geny.Readable, filename: String = "virtual"): Value = readable.readBytesThrough{ s =>
    new Parser(s, filename).parseValue(0, new ValueBuilder)
  }

  def write(y: Value): String = y.render()

  def writeToOutputStream(t: Value, out: java.io.OutputStream) = t.writeBytesTo(out)
}
