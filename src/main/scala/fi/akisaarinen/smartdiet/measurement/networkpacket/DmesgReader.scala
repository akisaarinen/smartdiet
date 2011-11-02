package fi.akisaarinen.smartdiet.measurement.networkpacket

import scala.util.parsing.combinator._
import io.Source


object DmesgReader extends RegexParsers {
  case class Line(id: Int, ts: Double, msg: String)

  override val skipWhitespace = false

  def CRLF = "\r\n" | "\n"
  def NUM = "[ 0-9.]".r
  def ANY = "[^\r\n]".r

  def file: Parser[List[Line]] = repsep(line, CRLF) <~ (CRLF?)
  def line: Parser[Line] = "<" ~ (NUM*) ~ ">[" ~ (NUM*) ~ "] " ~ (ANY*) ^^ {
    case "<" ~ id ~ ">[" ~ ts ~ "] " ~ msg => Line(id.mkString("").trim.toInt, ts.mkString("").trim.toDouble, msg.mkString(""))
  }

  def parse(s: String): List[Line] = parseAll(file, s) match {
    case Success(res, _) => res
    case e => throw new Exception(e.toString)
  }

  def readFromFile(filename: String): List[Line] = {
    val data = Source.fromFile(filename).getLines()
    parse(data.mkString("\n"))
  }
}