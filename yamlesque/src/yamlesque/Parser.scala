package yamlesque

import java.io.InputStream
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer

/** A position in a YAML document.
  *
  * @param line 1-based line number, for display
  * @param col 1-based column, counted in unicode code points, for display
  * @param index 0-based byte offset into the UTF-8 encoded input. Use this to
  * look up text in the original document, e.g. with [[Position.lineAt]].
  */
case class Position(file: String, line: Int, col: Int, index: Int) {
  override def toString = s"$file:$line:$col"
}
object Position {
  import java.nio.charset.StandardCharsets.UTF_8

  private def lineBounds(source: Array[Byte], index: Int): (Int, Int, Int) = {
    val idx = math.max(0, math.min(index, source.length))
    var start = idx
    while (start > 0 && source(start - 1) != '\n') start -= 1
    var end = idx
    while (end < source.length && source(end) != '\n') end += 1
    (start, idx, end)
  }

  private def decode(source: Array[Byte], from: Int, until: Int): String = {
    val s = new String(source, from, until - from, UTF_8).replace("\r", "")
    if (from == 0 && s.startsWith("\uFEFF")) s.substring(1) else s
  }

  /** The text of the line containing the given byte index. */
  def lineAt(source: Array[Byte], index: Int): String = {
    val (start, _, end) = lineBounds(source, index)
    decode(source, start, end)
  }

  /** The 0-based column, in code points, of the given byte index within its line. */
  private[yamlesque] def colAt(source: Array[Byte], index: Int): Int = {
    val (start, idx, _) = lineBounds(source, index)
    val prefix = decode(source, start, idx)
    prefix.codePointCount(0, prefix.length)
  }
}

case class ParseException(
  position: Position,
  message: String
) extends Exception(message: String) {

  /** A human-readable description of this error, including the offending
    * line. `source` must be the document that was parsed.
    */
  def pretty(source: Array[Byte]): String = {
    val line = Position.lineAt(source, position.index)
    val caret = " " * Position.colAt(source, position.index) + "^"
    s"$message\n$position\n$line\n$caret"
  }
  def pretty(source: String): String =
    pretty(source.getBytes(java.nio.charset.StandardCharsets.UTF_8))
}

object Parser {
  /** Plain scalars which are interpreted as null. */
  private[yamlesque] def isNullLiteral(text: String): Boolean = text match {
    case "null" | "Null" | "NULL" | "~" => true
    case _ => false
  }

  private[yamlesque] def appendCodePoint(sb: StringBuilder, cp: Int): Unit = {
    if (cp < 0x10000) {
      sb += cp.toChar
    } else {
      val u = cp - 0x10000
      sb += (0xd800 + (u >> 10)).toChar
      sb += (0xdc00 + (u & 0x3ff)).toChar
    }
  }
}

class Parser(input: java.io.InputStream, filename: String) {
  import Parser.appendCodePoint

  // byte state
  private val bytes = new Array[Byte](8192)
  private var bytesPos = 0
  private var bytesLen = 0
  private var byteIdx = 0 // offset of the next byte to be read

  private def readByte(): Int = {
    while (bytesPos >= bytesLen) {
      bytesLen = input.read(bytes)
      bytesPos = 0
      if (bytesLen < 0) {
        bytesLen = 0
        return -1
      }
    }
    val b = bytes(bytesPos) & 0xff
    bytesPos += 1
    byteIdx += 1
    b
  }
  // only valid immediately after a readByte() which did not return -1
  private def unreadByte(): Unit = {
    bytesPos -= 1
    byteIdx -= 1
  }

  // Look at the n-th next unread byte (0-based) without consuming it. n must be
  // smaller than the buffer size.
  private def peekByte(n: Int): Int = {
    if (bytesPos + n >= bytesLen) {
      // move remaining bytes to the start of the buffer and fill up the rest
      System.arraycopy(bytes, bytesPos, bytes, 0, bytesLen - bytesPos)
      bytesLen -= bytesPos
      bytesPos = 0
      var eof = false
      while (bytesLen <= n && !eof) {
        val read = input.read(bytes, bytesLen, bytes.length - bytesLen)
        if (read < 0) eof = true else bytesLen += read
      }
    }
    if (bytesPos + n < bytesLen) bytes(bytesPos + n) & 0xff else -1
  }

  // Decode the next UTF-8 code point. Malformed sequences yield U+FFFD.
  private def readCodePoint(): Int = {
    val b0 = readByte()
    if (b0 < 0x80) return b0 // ascii or EOF

    var n = 0
    var cp = 0
    var min = 0
    if ((b0 & 0xe0) == 0xc0) { n = 1; cp = b0 & 0x1f; min = 0x80 }
    else if ((b0 & 0xf0) == 0xe0) { n = 2; cp = b0 & 0x0f; min = 0x800 }
    else if ((b0 & 0xf8) == 0xf0) { n = 3; cp = b0 & 0x07; min = 0x10000 }
    else return 0xfffd

    while (n > 0) {
      val b = readByte()
      if (b == -1) return 0xfffd
      if ((b & 0xc0) != 0x80) {
        unreadByte()
        return 0xfffd
      }
      cp = (cp << 6) | (b & 0x3f)
      n -= 1
    }
    if (cp < min || cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) 0xfffd
    else cp
  }

  // character state
  private var cline = 1
  private var ccol = 0
  private var cidx = 0 // byte offset of the current char
  private var char: Int = -1 // current unicode code point
  private var indent = true // only spaces precede the current char on its line

  @inline private def readChar(): Unit = {
    if (char == '\n') {
      ccol = 0
      cline += 1
      indent = true
    } else if (char != ' ' && char != '\r' && char != -1) {
      indent = false
    }
    cidx = byteIdx
    char = readCodePoint()
    char match {
      case '\r' => readChar()
      case -1 =>
      case _ => ccol += 1
    }
  }
  readChar()
  if (char == 0xfeff) { // skip byte order mark
    ccol = 0
    readChar()
    indent = true
  }
  private def cpos = Position(filename, cline, ccol, cidx)

  // whether the current char starts a document marker, i.e. `---` or `...` at
  // the start of a line, followed by whitespace or the end of the line
  private def atMarker: Boolean = {
    ccol == 1 && (char == '-' || char == '.') &&
      peekByte(0) == char && peekByte(1) == char && {
        val next = peekByte(2)
        next == ' ' || next == '\t' || next == '\n' || next == '\r' || next == -1
      }
  }

  private sealed trait Token
  private case object Eof extends Token { override def toString = "EOF" }
  private case object Text extends Token { override def toString = "text" }
  private case object QText extends Token { override def toString = "quoted text" }
  private case object Key extends Token { override def toString = "key" }
  private case object Item extends Token { override def toString = "list item" }
  private case object FoldStyle extends Token { override def toString = ">" }
  private case object LitStyle extends Token { override def toString = "|" }
  private case object DocStart extends Token { override def toString = "---" }
  private case object DocEnd extends Token { override def toString = "..." }

  // whether the current token ends the current document
  private def atDocEnd = tok == Eof || tok == DocStart || tok == DocEnd

  // token state
  private var tline = cline
  private var tcol = ccol
  private var tidx = cidx
  private val tokenBuffer = new StringBuilder
  private var tok: Token = _
  private def tpos = Position(filename, tline, tcol, tidx)

  case class Ctx(pos: Position) extends yamlesque.Ctx

  private def readKeyOrText(): Unit = {
    while (true) {
      var spaceCount = 0
      while (char == ' ') {
        readChar()
        spaceCount += 1
      }
      char match {
        case ':' =>
          readChar()
          char match {
            case ' ' | '\n' | -1 =>
              //readChar()
              tok = Key
              return
            case other =>
              for (_ <- 0 until spaceCount) tokenBuffer += ' '
              tokenBuffer += ':'
              appendCodePoint(tokenBuffer, other)
              readChar()
          }
        case -1 | '\n' =>
          //readChar()
          tok = Text
          return
        case '#' =>
          if (spaceCount > 0) {
            tok = Text
            return
          } else {
            for (_ <- 0 until spaceCount) tokenBuffer += ' '
            tokenBuffer += '#'
            readChar()
          }
        case other =>
          for (_ <- 0 until spaceCount) tokenBuffer += ' '
          appendCodePoint(tokenBuffer, other)
          readChar()
      }
    }
  }

  private def readItem(): Unit = {
    char match {
      case '-' =>
        readChar()
        char match {
          case ' ' | '\n' | -1 =>
            tok = Item
          case other =>
            tokenBuffer += '-'
            readKeyOrText()
        }
      case other =>
        readKeyOrText()
    }
  }

  private def readQuotedText(): Unit = {
    while(char != '"') {
      char match {
        case '\\' =>
          readChar()
          if (char == '"') {
            tokenBuffer += '"'
            readChar()
          } else {
            tokenBuffer += '\\'
          }
        case -1 => tokenError("Expected closing \" but reached EOF")
        case other =>
          appendCodePoint(tokenBuffer, char)
          readChar()
      }
    }
    readChar()
    tok = QText
  }

  // single-quoted text has no escapes, except '' for a literal '
  private def readSingleQuotedText(): Unit = {
    while (true) {
      char match {
        case '\'' =>
          readChar()
          if (char == '\'') {
            tokenBuffer += '\''
            readChar()
          } else {
            tok = QText
            return
          }
        case -1 => tokenError("Expected closing ' but reached EOF")
        case other =>
          appendCodePoint(tokenBuffer, other)
          readChar()
      }
    }
  }

  // TODO: call this to make comments available to user code
  private def readComment() = {
    while (char == ' ') readChar()
    var spaceCount = 0
    while (!(char == '\n' || char == -1)) {
      char match {
        case ' ' => spaceCount += 1
        case nonspace =>
          for (_ <- 0 until spaceCount) tokenBuffer += ' '
          spaceCount = 0
          appendCodePoint(tokenBuffer, char)
      }
      readChar()
    }
  }

  private def readToken(): Unit = {
    // position of the first tab used for indentation on the current line, if any
    var tab: Position = null
    while (char == ' ' || char == '\n' || char == '\t') {
      if (char == '\n') tab = null
      else if (char == '\t' && indent && tab == null) tab = cpos
      readChar()
    }
    // tabs are only an error if they indent content, not on blank or comment lines
    if (tab != null && char != -1 && char != '#') {
      throw new ParseException(tab, "Tabs are not allowed for indentation")
    }
    tokenBuffer.clear()
    tline = cline
    tcol = ccol
    tidx = cidx
    char match {
      case -1 => tok = Eof
      case '-' | '.' if atMarker =>
        tok = if (char == '-') DocStart else DocEnd
        readChar()
        readChar()
        readChar()
      case '-' => readItem()
      case '|' | '>' =>
        val c = char
        readChar()
        char match {
          case ' ' | '\n' | -1 =>
            if (c == '|') tok = LitStyle else tok = FoldStyle
          case other =>
            tokenBuffer += c.toChar
            readKeyOrText()
        }
      case '#' =>
        readChar()
        readComment()
        readToken()
      case '"' =>
        readChar()
        readQuotedText()
      case '\'' =>
        readChar()
        readSingleQuotedText()
      case other => readKeyOrText()
    }
    //println("token: " + tok + " tcol: " + tcol)
  }
  readToken()


  private def tokenError(message: String) = {
    throw new ParseException(tpos, message)
  }
  private def tokenExpectedError(expected: Token) = {
    tokenError(s"Expected $expected. Found: $tok")
  }

  private def parseKey(): String = {
    tok match {
      case Key =>
        val res = tokenBuffer.result()
        readToken()
        res
      case other => tokenExpectedError(Key)
    }
  }

  private def parseMap[T](visitor: ObjectVisitor[T]): T = {
    val scol = tcol
    while (tcol == scol && !atDocEnd) {
      val p = tpos
      val key = parseKey()
      visitor.visitKey(Ctx(p), key)

      val ctx = Ctx(tpos)
      if (scol < tcol) {
        val value = parseValue(scol + 1, visitor.subVisitor())
        visitor.visitValue(ctx, value)
      } else if (scol == tcol && tok == Item) { // special case: lists can start at same indentation as keys
        val value = parseList(visitor.subVisitor().visitArray(ctx), inMap = true)
        visitor.visitValue(ctx, value)
      } else {
        val value = visitor.subVisitor().visitEmpty(ctx)
        visitor.visitValue(ctx, value)
      }
    }
    if (scol < tcol && !atDocEnd) tokenError("Entries within the same map must start at the same column.")
    visitor.visitEnd()
  }

  // `inMap`: this list is a map value starting at the same column as its key,
  // so a following key at that column ends the list rather than being an error
  private def parseList[T](visitor: ArrayVisitor[T], inMap: Boolean = false): T = {
    val scol = tcol
    var idx = 0
    while (tcol == scol && !atDocEnd && !(inMap && tok != Item)) {
      val ctx = Ctx(tpos)
      visitor.visitIndex(ctx, idx)
      tok match {
        case Item =>
          readToken()
          val ctx = Ctx(tpos)
          if (scol < tcol) {
            val value = parseValue(scol + 1, visitor.subVisitor())
            visitor.visitValue(ctx, value)
          } else {
            val value = visitor.subVisitor().visitEmpty(ctx)
            visitor.visitValue(ctx, value)
          }
        case other => tokenExpectedError(Item)
      }
      idx += 1
    }
    if (scol < tcol && !atDocEnd) tokenError("Items within the same list must start at the same column.")
    visitor.visitEnd()
  }

  // NOTE: minCol is the minimal column of continuation lines. It is given by
  // the enclosing map or list, not by the start of the text itself. E.g.
  //   somekey: foo
  //    bar
  //    ^ minimum continuation position in this case
  private def parseText(minCol: Int): String = {
    val data = new StringBuilder()

    if (tok != Text && tok != QText) tokenExpectedError(Text)
    var previousLine = tline
    data ++= tokenBuffer.result()
    readToken()
    while (minCol <= tcol && !atDocEnd) {
      if (tok != Text && tok != QText) tokenExpectedError(Text)

      if (tline - previousLine > 1) {
        for (_ <- 0 until tline - previousLine - 1) data += '\n'
      } else {
        data += ' '
      }
      previousLine = tline
      data ++= tokenBuffer.result()
      readToken()
    }
    data.result()
  }

  // This drops down to reading individual characters instead of tokens
  //
  // NOTE: minCol is the minimal column where the text block may start. Since
  // the blocks may start before the indicator char, this parameter cannot
  // simply taken from the current token position. E.g.
  //   somekey: >
  //    foo
  //    ^ minimum start position in this case
  private def parseTextBlock(minCol: Int): String = {
    val literal = tok match {
      case LitStyle => true
      case FoldStyle => false
      case other => tokenError(s"Expected | or >, found $other")
    }

    // consume remainder of line
    while (!(char == '\n' || char == -1)) {
      char match {
        case '#' => while (!(char == '\n' || char == -1)) readChar()
        case ' ' =>
          readChar()
        case other =>
          tokenError("A text block must start after '>' or '|', not on the same line")
          readChar()
      }
    }
    readChar()

    var lineCount = 0
    var spaceCount = 0

    // determine starting column
    while (char == ' ' || char == '\n') {
      if (char == '\n') {
        lineCount += 1
      }
      readChar()
    }

    tokenBuffer.clear()
    if (minCol <= ccol && char != -1 && !atMarker) {
      val scol = ccol

      if (literal) {
        for (_ <- 0 until lineCount) tokenBuffer += '\n'
      } else if (lineCount > 1) { // fold style but more than one empty line
        for (_ <- 0 until lineCount - 1) tokenBuffer += '\n'
      }
      lineCount = 0
      spaceCount = 0
      while (!(char == ' ' || char == '\n' || char == -1)) {
        appendCodePoint(tokenBuffer, char)
        readChar()
      }

      while (scol <= ccol && char != -1 && !atMarker) {
        while (char == ' ' || char == '\n') {
          if (char == '\n') {
            lineCount += 1
            spaceCount = 0
          }
          if (char == ' ' && scol <= ccol) spaceCount +=1
          readChar()
        }
        if (scol <= ccol && char != -1 && !atMarker) {
          if (literal && lineCount > 0) {
            for (_ <- 0 until lineCount) tokenBuffer += '\n'
          } else if (lineCount == 1) {
            tokenBuffer += ' '
          } else if (lineCount > 1) {
            for (_ <- 0 until lineCount - 1) tokenBuffer += '\n'
          }
          for (_ <- 0 until spaceCount) tokenBuffer += ' '
          appendCodePoint(tokenBuffer, char)
          readChar()
          lineCount = 0
          spaceCount = 0
        }
      }
    }
    val r = tokenBuffer.result()
    readToken() // since this function worked directly on chars, we need to pull in the next token
    r
  }

  def parseValue[T](minCol: Int, visitor: Visitor[T]): T = {
    val ctx = Ctx(tpos)
    tok match {
      case Eof | DocStart | DocEnd => visitor.visitEmpty(ctx)
      case Key => parseMap(visitor.visitObject(ctx))
      case Text =>
        val text = parseText(minCol)
        if (Parser.isNullLiteral(text)) visitor.visitEmpty(ctx)
        else visitor.visitString(ctx, text)
      case QText => visitor.visitQuotedString(ctx, parseText(minCol))
      case Item => parseList(visitor.visitArray(ctx))
      case LitStyle => visitor.visitBlockStringLiteral(ctx, parseTextBlock(minCol))
      case FoldStyle => visitor.visitBlockStringFolded(ctx, parseTextBlock(minCol))
    }
  }

  /** Whether another document follows in the input. */
  def hasNextDocument: Boolean = {
    while (tok == DocEnd) readToken() // `...` without a following document
    tok != Eof
  }

  /** Parse the next document of the input.
    *
    * A document may be preceded by `---`, and must be followed by `---`,
    * `...` or the end of the input.
    */
  def parseDocument[T](visitor: Visitor[T]): T = {
    while (tok == DocEnd) readToken()
    if (tok == DocStart) readToken()
    val value = parseValue(0, visitor)
    tok match {
      case Eof | DocStart =>
      case DocEnd => readToken()
      case other => tokenError(s"Expected end of document. Found: $other")
    }
    value
  }

  /** Parse an input which must contain at most one document. */
  def parseSingleDocument[T](visitor: Visitor[T]): T = {
    val value = parseDocument(visitor)
    if (hasNextDocument) {
      tokenError("Expected a single document, but found another one. Use readDocuments to read multiple documents.")
    }
    value
  }
}
