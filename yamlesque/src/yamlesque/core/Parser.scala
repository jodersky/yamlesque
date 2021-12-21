package yamlesque.core

import java.io.InputStream
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer

case class Position(file: String, line: Int, col: Int) {
  override def toString = s"$file:$line:$col"
}

case class ParseException(
  position: Position,
  message: String,
  line: String
) extends Exception(message: String)


class Parser(input: java.io.InputStream, filename: String) {

  // character state
  private var cline = 1
  private var ccol = 0
  private var char: Int = -1

  private val lineBuffer = new StringBuilder()
  @inline private def readChar(): Unit = {
    //if (ccol == 0) lineBuffer.clear()
    if (char == '\n') {
      lineBuffer.clear()
      ccol = 0
      cline += 1
    }
    char = input.read()
    char match {
      case '\r' => readChar()
      case -1 =>
        lineBuffer += '\n'
      case _ =>
        ccol += 1
        lineBuffer += char.toChar
    }

    // char match {
    //   case '\n' =>
    //     ccol = 0
    //     cline += 1
    //   case -1 | '\r' => // invisible chars, do nothing
    //   case _ =>
    //     ccol += 1
    //     lineBuffer += char.toChar
    // }
  }
  readChar()

  private sealed trait Token
  private case object Eof extends Token { override def toString = "EOF" }
  private case object Text extends Token { override def toString = "text" }
  private case object Key extends Token { override def toString = "key" }
  private case object Item extends Token { override def toString = "list item" }
  private case object FoldStyle extends Token { override def toString = ">" }
  private case object LitStyle extends Token { override def toString = "|" }

  // token state
  private var tline = cline
  private var tcol = ccol
  private val tokenBuffer = new StringBuilder
  private var tok: Token = _

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
              tokenBuffer += other.toChar
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
          tokenBuffer += other.toChar
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
          tokenBuffer += char.toChar
          readChar()
      }
    }
    readChar()
    tok = Text
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
          tokenBuffer += char.toChar
      }
      readChar()
    }
  }


  private def readToken(): Unit = {
    while (char == ' ' || char == '\n') readChar()
    tokenBuffer.clear()
    tline = cline
    tcol = ccol
    char match {
      case -1 => tok = Eof
      case '-' => readItem()
      case '|' | '>' =>
        val c = char
        readChar()
        char match {
          case ' ' | '\n' | -1 =>
            if (c == '|') tok = LitStyle else tok = FoldStyle
          case other =>
            tokenBuffer += c.toChar
            tokenBuffer += other.toChar
            readKeyOrText()
        }
      case '#' =>
        readChar()
        readComment()
        readToken()
      case '"' =>
        readChar()
        readQuotedText()
      case other => readKeyOrText()
    }
    //println("token: " + tok + " tcol: " + tcol)
  }
  readToken()


  private def tokenError(message: String) = {
    // read until end of line
    while (!(char == -1 || char == '\n')) {
      readChar()
    }

    val line = lineBuffer.result()
    val caret = " " * (tcol - 1) + "^"
    val pos = Position(filename, tline, tcol)
    val pretty = s"$message\n$pos\n$line\n$caret"
    throw new ParseException(pos, pretty, line)
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
    val data = LinkedHashMap.empty[String, Value]
    while (tcol == scol && tok != Eof) {
      val key = parseKey()
      visitor.visitKey(key)

      if (scol < tcol) {
        val value = parseValue(scol + 1, visitor.subVisitor())
        visitor.visitValue(value)
      } else if (scol == tcol && tok == Item) { // special case: lists can start at same indentation as keys
        val value = parseList(visitor.subVisitor().visitArray())
        visitor.visitValue(value)
      } else {
        val value = visitor.subVisitor().visitEmpty()
        visitor.visitValue(value)
      }
    }
    if (scol < tcol && tok != Eof) tokenError("Entries within the same map must start at the same column.")
    visitor.visitEnd()
  }


  private def parseList[T](visitor: ArrayVisitor[T]): T = {
    val scol = tcol
    val data = ArrayBuffer.empty[Value]
    var idx = 0
    while (tcol == scol && tok != Eof) {
      visitor.visitIndex(idx)
      tok match {
        case Item =>
          readToken()
          if (scol < tcol) {
            val value = parseValue(scol + 1, visitor.subVisitor())
            visitor.visitValue(value)
          } else {
            val value = visitor.subVisitor().visitEmpty()
            visitor.visitValue(value)
          }
        case other => tokenExpectedError(Item)
      }
      idx += 1
    }
    if (scol < tcol && tok != Eof) tokenError("Items within the same list must start at the same column.")
    visitor.visitEnd()
  }

  private def parseText(): String = {
    val scol = tcol
    val data = new StringBuilder()

    if (tok != Text) tokenExpectedError(Text)
    var previousLine = tline
    data ++= tokenBuffer.result()
    readToken()
    while (scol <= tcol && tok != Eof) {
      if (tok != Text) tokenExpectedError(Text)

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
    if (minCol <= ccol && char != -1) {
      val scol = ccol

      if (literal) {
        for (_ <- 0 until lineCount) tokenBuffer += '\n'
      } else if (lineCount > 1) { // fold style but more than one empty line
        for (_ <- 0 until lineCount - 1) tokenBuffer += '\n'
      }
      lineCount = 0
      spaceCount = 0
      while (!(char == ' ' || char == '\n' || char == -1)) {
        tokenBuffer += char.toChar
        readChar()
      }

      while (scol <= ccol && char != -1) {
        while (char == ' ' || char == '\n') {
          if (char == '\n') {
            lineCount += 1
            spaceCount = 0
          }
          if (char == ' ' && scol <= ccol) spaceCount +=1
          readChar()
        }
        if (scol <= ccol && char != -1) {
          if (literal && lineCount > 0) {
            for (_ <- 0 until lineCount) tokenBuffer += '\n'
          } else if (lineCount == 1) {
            tokenBuffer += ' '
          } else if (lineCount > 1) {
            for (_ <- 0 until lineCount - 1) tokenBuffer += '\n'
          }
          for (_ <- 0 until spaceCount) tokenBuffer += ' '
          tokenBuffer += char.toChar
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
    tok match {
      case Eof => visitor.visitEmpty()
      case Key => parseMap(visitor.visitObject())
      case Text => visitor.visitString(parseText())
      case Item => parseList(visitor.visitArray())
      case LitStyle => visitor.visitBlockStringLiteral(parseTextBlock(minCol))
      case FoldStyle => visitor.visitBlockStringFolded(parseTextBlock(minCol))
    }
  }
}

object `package` {

  def read(readable: geny.Readable, filename: String = "virtual"): Value = readable.readBytesThrough{ s =>
    new Parser(s, filename).parseValue(0, new ValueBuilder)
  }

}
