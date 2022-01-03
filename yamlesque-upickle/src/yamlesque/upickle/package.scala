package yamlesque.upickle

object default {

  def read[A](readable: geny.Readable, filename: String = "virtual")(implicit reader: _root_.upickle.default.Reader[A]) = {
    val delegate = new UjsonDelegate[A](reader)
    readable.readBytesThrough{ s =>
      new yamlesque.Parser(s, filename).parseValue(0, delegate)
    }
  }

}
