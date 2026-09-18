package object yamlesque {

  /** Read a single YAML document. Fails if the input contains more than one
    * document; use [[readDocuments]] for those.
    */
  def read(readable: geny.Readable, filename: String = "virtual"): Value = readable.readBytesThrough{ s =>
    new Parser(s, filename).parseSingleDocument(new ValueBuilder)
  }

  /** Read all YAML documents (separated by `---`) of the input. */
  def readDocuments(readable: geny.Readable, filename: String = "virtual"): Seq[Value] = readable.readBytesThrough{ s =>
    val parser = new Parser(s, filename)
    val docs = collection.mutable.ArrayBuffer.empty[Value]
    while (parser.hasNextDocument) docs += parser.parseDocument(new ValueBuilder)
    docs.toSeq
  }

  def write(y: Value): String = y.render()

  def writeToOutputStream(t: Value, out: java.io.OutputStream) = t.writeBytesTo(out)

  /** Write multiple YAML documents, each one starting with `---`. */
  def writeDocuments(docs: Iterable[Value]): String = {
    val out = new java.io.ByteArrayOutputStream
    writeDocumentsToOutputStream(docs, out)
    new String(out.toByteArray(), "utf-8")
  }

  def writeDocumentsToOutputStream(docs: Iterable[Value], out: java.io.OutputStream): Unit = {
    for (doc <- docs) {
      out.write("---\n".getBytes("utf-8"))
      doc.writeBytesTo(out)
      out.write('\n')
    }
  }
}
