package yamlesque
import java.io.Reader

trait Tokenizer {

  def in: Reader

  protected sealed trait TokenKind
  protected case object Key extends TokenKind
  protected case object Item extends TokenKind
  protected case object Scalar extends TokenKind
  protected case object Start extends TokenKind
  protected case object End extends TokenKind

  protected case object QuotedString extends TokenKind // "", may contain comment
  protected case object Verbatim extends TokenKind // | or >

  protected val EOF = -1.toChar

  private var line = 1
  private var col = 0
  protected var ch: Char = 0
  private var cr: Boolean = false // was the previous char a carriage return?

  protected var tokenKind: TokenKind = End
  protected var tokenValue: String = ""
  protected var tokenLine: Int = 1
  protected var tokenCol: Int = 1

  private def readChar(): Unit = if (ch != EOF) {
    ch = in.read().toChar
    col += 1
    if (cr) {
      cr = false
      line += 1
      col = 1
    }
    if (ch == '\n') {
      cr = true
    }
  }
  readChar()

  @inline private def accept(c: Char) =
    if (ch == c) {
      readChar(); true
    } else false

  @inline private def skipSpace(): Unit = while (ch == ' ') readChar()

  private val buffer = new StringBuilder

  @inline private def nextStringOrKey() = {
    var done = false
    while (!done) {
      if (accept('\n') || accept(EOF)) {
        tokenKind = Scalar
        tokenValue = buffer.result().trim()
        done = true
      } else if (accept(' ')) {
        if (ch == '#') {
          tokenKind = Scalar
          tokenValue = buffer.result().trim()
          done = true
        } else {
          buffer += ' '
        }
      } else if (accept(':')) {
        if (accept(' ') || accept('\n') || accept(EOF)) {
          tokenKind = Key
          tokenValue = buffer.result().trim()
          done = true
        } else {
          buffer += ':'
          buffer += ch
          readChar()
        }
      } else {
        buffer += ch
        readChar()
      }
    }
  }

  @inline private def nextQuoteOrKey() = {
    buffer.clear()
    while (ch != '"' && ch != EOF) {
      if (accept('\\')) {
        if (ch != EOF) {
          buffer += ch
          readChar()
        }
      } else {
        buffer += ch
        readChar()
      }
    }
    readChar()
    tokenValue = buffer.result()

    skipSpace()
    if (accept(':')) {
      if (accept(' ') || accept('\n') || accept(EOF)) {
        tokenKind = Key
      } else {
        // this is an irrefular situation and the parser will error out later
        tokenKind = QuotedString
      }
    } else {
      tokenKind = QuotedString
    }
  }

  @annotation.tailrec
  @inline
  protected final def nextToken(): Unit = {
    buffer.clear()
    skipSpace()
    if (accept(EOF)) {
      tokenKind = End
      tokenLine = line
      tokenCol = col - 1
    } else if (accept('#')) {
      while (ch != '\n' && ch != EOF) {
        readChar()
      }
      nextToken()
    } else if (accept('\n')) {
      nextToken()
    } else if (accept('-')) {
      tokenLine = line
      tokenCol = col - 1
      if (accept('-')) {
        if (accept('-')) {
          while (ch != '\n' && ch != EOF) readChar()
          tokenKind = Start
        } else {
          buffer ++= "--"
          buffer += ch
          readChar()
          nextStringOrKey()
        }
      } else if (accept(' ') || accept('\n') || accept(EOF)) {
        tokenKind = Item
      } else {
        buffer += '-'
        buffer += ch
        readChar()
        nextStringOrKey()
      }
    } else if (ch == '|' || ch == '>') {
      var marker = ch
      readChar()
      if (accept('\n') || accept(EOF)) {
        nextVerbatimBlock(tokenCol, marker == '>')
      } else {
        buffer += marker
        buffer += ch
        readChar()
        nextStringOrKey()
      }
    } else if (accept('"')) {
      tokenLine = line
      tokenCol = col - 1
      nextQuoteOrKey()
    } else {
      tokenLine = line
      tokenCol = col
      nextStringOrKey()
    }
  }
  nextToken()

  protected def nextVerbatimBlock(minCol: Int, foldLines: Boolean) = {
    buffer.clear()
    var startCol = 0
    var lastNonEmptyLine = line

    // find start column, whitespace is significant
    while (accept('\n')) {
      buffer += '\n'
    }
    skipSpace()
    startCol = col
    tokenLine = line

    if (startCol <= minCol) {
      tokenCol = minCol + 1
      tokenKind = Verbatim
      tokenValue = ""
    } else {
      var done = false
      while (!done) {
        // skip spaces until we reach starting column
        while (ch == ' ' && col < startCol && ch != EOF) readChar()

        if (ch == '\n') {
          readChar()
          done = ch == EOF
        } else if (col == startCol) {
          for (i <- lastNonEmptyLine until line - 1) { buffer += '\n' }
          lastNonEmptyLine = line
          var eol = false
          while (!eol) {
            if (ch == '\n' || ch == EOF) eol = true
            if (ch != EOF) {
              buffer += ch
              readChar()
            }
          }
          done = ch == EOF
        } else {
          done = true
        }
      }

      tokenKind = Verbatim
      tokenCol = startCol
      tokenValue = buffer.result()
    }
  }

}

object Parser {
  case class ParseException(message: String) extends RuntimeException(message)
}

class Parser(val in: Reader) extends Tokenizer with Iterator[Node] {

  private def friendlyKind(kind: TokenKind) = kind match {
    case Key          => "map key"
    case Item         => "list item"
    case Scalar       => "scalar"
    case QuotedString => "string"
    case Verbatim     => "verbatim block"
    case Start        => "start of document"
    case End          => "EOF"
  }

  private def friendlyValue = tokenKind match {
    case Key          => tokenValue + ":"
    case Item         => "-"
    case Scalar       => tokenValue
    case QuotedString => s""""$tokenValue""""
    case Verbatim     => "verbatim block " + tokenValue.takeWhile(_ != '\n') + "..."
    case Start        => "---"
    case End          => "EOF"
  }

  private def fatal(message: String): Nothing = {
    val info = s"$tokenLine:$tokenCol: $message\n"
    val token = (" " * tokenCol) + friendlyValue + "\n"
    val caret = (" " * tokenCol) + "^\n"
    throw new Parser.ParseException(info + token + caret)
  }

  private var node: Node = null

  // the first document does not strictly need to be started with a ---
  private def initDocument() = {
    if (tokenKind == Start) {
      nextDocument()
    } else {
      nextNode()
    }
  }
  initDocument()

  // subsequent documents require an explicit start
  private def nextDocument() = {
    tokenKind match {
      case Start =>
        nextToken()
        nextNode()
      case _ =>
        fatal(
          s"expected ${friendlyKind(Start)}, but found ${friendlyKind(tokenKind)}"
        )
    }
  }

  private def nextNode(): Unit = {
    tokenKind match {
      case Key    => nextMap()
      case Item   => nextList()
      case Scalar => nextString()
      case QuotedString =>
        node = Str(tokenValue)
        nextToken()
      case Verbatim =>
        node = Str(tokenValue)
        nextToken()
      case Start | End =>
        node = Null
    }
  }

  private def nextMap(): Unit = {
    val y = Obj()
    val startCol = tokenCol

    do {
      if (tokenKind != Key) {
        fatal(
          s"expected ${friendlyKind(Key)}, but found ${friendlyKind(tokenKind)}"
        )
      }
      if (tokenCol != startCol) {
        fatal(s"${friendlyKind(Key)} is not aligned")
      }

      val key = tokenValue
      nextToken()

      tokenKind match {
        case Start | End =>
          y.obj(key) = Null
        // special case: we allow lists to start after a key without requiring an indent
        case Item if tokenCol == startCol =>
          nextNode()
          y.obj(key) = node
        case _ if tokenCol <= startCol =>
          y.obj(key) = Null
        case _ =>
          nextNode()
          y.obj(key) = node
      }
    } while (tokenCol >= startCol && tokenKind != Start && tokenKind != End)
    node = y
  }

  private def nextList(): Unit = {
    val y = Arr()
    val startCol = tokenCol

    do {
      if (tokenKind != Item) {
        fatal(
          s"expected ${friendlyKind(Item)}, but found ${friendlyKind(tokenKind)}"
        )
      }
      if (tokenCol != startCol) {
        fatal(s"${friendlyKind(Item)} is not aligned")
      }

      nextToken()

      tokenKind match {
        case Start | End =>
          y.arr += Null
        case _ if tokenCol <= startCol =>
          y.arr += Null
        case _ =>
          nextNode()
          y.arr += node
      }
    } while (tokenCol >= startCol && tokenKind != Start && tokenKind != End)
    node = y
  }

  private def nextString(): Unit = {
    val buffer = new StringBuilder
    val startCol = tokenCol

    buffer ++= tokenValue
    nextToken()

    while (tokenCol >= startCol && tokenKind != Start && tokenKind != End) {
      if (tokenKind != Scalar) {
        fatal(
          s"expected ${friendlyKind(Scalar)}, but found ${friendlyKind(tokenKind)}"
        )
      }
      buffer += ' '
      buffer ++= tokenValue
      nextToken()
    }
    node = specializeString(buffer.result())
  }

  private def specializeString(str: String) = str match {
    case "null"  => Null
    case "true"  => Bool(true)
    case "false" => Bool(false)
    case s =>
      try {
        Num(s.toDouble)
      } catch {
        case _: NumberFormatException => Str(s)
      }
  }

  private var reachedEnd = false
  def hasNext: Boolean = !reachedEnd
  def next(): Node = {
    val result = node
    if (tokenKind == End) {
      reachedEnd = true
    } else {
      nextDocument()
    }
    result
  }

}
