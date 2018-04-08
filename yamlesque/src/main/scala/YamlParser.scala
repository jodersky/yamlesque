package yamlesque

import annotation.{switch, tailrec}
import scala.collection.mutable.ListBuffer

object YamlParser extends (Iterator[Char] => YamlValue) {

  sealed trait TokenKind
  object TokenKind {
    case object EOF extends TokenKind
    case object BAD extends TokenKind
    case object DOCSTART extends TokenKind
    case object DOCEND extends TokenKind
    case object MAPPING extends TokenKind
    case object ITEM extends TokenKind
    case object IDENTIFIER extends TokenKind
    case object COMMENT extends TokenKind
  }
  import TokenKind._

  case class Token(val kind: TokenKind, value: String = "") {
    var line: Int = 0
    var col: Int = 0
    def setPos(line: Int, col: Int): this.type = {
      this.col = col
      this.line = line
      this
    }
    override def toString() = {
      s"($line, $col): " + super.toString
    }
  }

  object Chars {
    final val LF = '\u000A'
    final val CR = '\u000D'
    final val SU = '\u001A'

    @inline def isSpace(ch: Char): Boolean = ch match {
      case ' ' | '\t' => true
      case _          => false
    }

    @inline def isBlank(ch: Char): Boolean = ch match {
      case ' ' | '\t' | CR | LF | SU => true
      case _                         => false
    }
  }

  class Scanner(chars: Iterator[Char]) extends Iterator[Token] {
    import Chars._

    private var ch0: Char = 0
    private var ch1: Char = 0
    private var ch2: Char = 0
    private var pos: Long = 0
    private var line: Int = 0
    private var col: Int = 0

    private def skipChar(): Unit = {
      val ch: Char = if (chars.hasNext) {
        chars.next()
      } else {
        SU
      }
      pos += 1
      col += 1
      ch0 = ch1
      ch1 = ch2
      ch2 = ch
    }
    private def skipChars(n: Int): Unit = {
      var i = 0
      while (i < n) { skipChar(); i += 1 }
    }
    def init() = {
      skipChars(3)
      pos = 0
      col = 0
      line = 0
    }

    private var buffer = new StringBuilder()
    private def putChar(): Unit = {
      buffer.append(ch0)
      skipChars(1)
    }
    private def tokenValue(): String = {
      val str = buffer.result()
      buffer.clear()
      str
    }

    private var token: Token = Token(BAD, "not yet initialized")

    @tailrec private def fetchToken(): Unit = {
      ch0 match {
        case ':' if isBlank(ch1) =>
          token = Token(MAPPING).setPos(line, col)
          skipChars(1)
        case '-' if isBlank(ch1) =>
          token = Token(ITEM).setPos(line, col)
          skipChars(1)
        case '-' if ch1 == '-' && ch2 == '-' =>
          token = Token(DOCSTART).setPos(line, col)
          skipChars(3)
        case '.' if ch1 == '.' && ch2 == '.' =>
          token = Token(DOCEND).setPos(line, col)
          skipChars(3)
        case '#' =>
          val l = line
          val c = col
          skipChars(1)
          while (ch0 != LF && ch0 != SU) {
            putChar()
          }
          token = Token(COMMENT, tokenValue()).setPos(l, c)
          buffer.clear()
        case c if isSpace(c) =>
          skipChars(1)
          fetchToken()
        case LF =>
          skipChars(1)
          col = 0
          line += 1
          fetchToken()
        case CR =>
          skipChars(1)
          if (ch0 == LF) {
            skipChars(1)
          }
          col = 0
          line += 1
          fetchToken()
        case SU =>
          token = Token(EOF).setPos(line, col)
          skipChars(1)
        case _ => fetchScalar()
      }
    }

    private def fetchScalar(): Unit = {
      def finishScalar() = token = Token(IDENTIFIER, tokenValue())
      @tailrec def fetchRest(): Unit = ch0 match {
        case ':' if isBlank(ch1) =>
          finishScalar()
        case LF =>
          finishScalar()
        case SU =>
          finishScalar()
        case c =>
          putChar()
          fetchRest()
      }
      val l = line
      val c = col
      fetchRest()
      token.setPos(l, c)
    }

    override def hasNext: Boolean = true
    override def next(): Token = {
      fetchToken()
      token
    }
    init()
  }

  def parse(tokens: Iterator[Token]): YamlValue = {
    var token0 = tokens.next()
    var token1 = tokens.next()

    def readNext(): Unit = {
      token0 = token1
      token1 = tokens.next()
    }

    def fatal(message: String, token: Token) = {
      val completeMessage =
        s"parse error at line ${token.line}, column ${token.col}: $message"
      throw new ParseException(completeMessage)
    }

    def wrongKind(found: Token, required: TokenKind*) = {
      fatal(
        s"token kind not allowed at this position\n" +
          s"  found: ${found.kind}\n" +
          s"  required: ${required.mkString(" or ")}\n" +
          " " * found.col + found.value + "\n" +
          " " * found.col + "^",
        found
      )
    }

    def nextSequence() = {
      val startCol = token0.col
      val items = new ListBuffer[YamlValue]
      while (startCol <= token0.col && token0.kind != EOF) {
        token0.kind match {
          case ITEM =>
            readNext()
            items += nextBlock(startCol)
          case _ => wrongKind(token0, ITEM)
        }
      }
      YamlSequence(items.toVector)
    }

    def nextMapping() = {
      val startCol = token0.col
      val fields = new ListBuffer[(String, YamlValue)]
      while (startCol <= token0.col && token0.kind != EOF) {
        token0.kind match {
          case IDENTIFIER =>
            val key = token0.value
            readNext()
            token0.kind match {
              case MAPPING =>
                readNext()
                val value = nextBlock(startCol)
                fields += key -> value
              case _ => wrongKind(token0, MAPPING)
            }

          case _ => wrongKind(token0, IDENTIFIER)
        }
      }
      YamlMapping(fields.toMap)
    }

    def nextBlock(startCol: Int): YamlValue = {
      if (token0.col < startCol) {
        YamlScalar.Empty
      } else {
        token0.kind match {
          case IDENTIFIER =>
            if (token1.kind == MAPPING && token0.line == token1.line) {
              nextMapping()
            } else {
              val y = YamlScalar(token0.value)
              readNext()
              y
            }
          case ITEM =>
            nextSequence()
          case EOF => YamlScalar.Empty
          case _   => wrongKind(token0, IDENTIFIER, ITEM)
        }
      }
    }

    nextBlock(0)
  }

  def apply(data: Iterator[Char]): YamlValue = parse(new Scanner(data))

}

class ParseException(val message: String) extends Exception(message)
