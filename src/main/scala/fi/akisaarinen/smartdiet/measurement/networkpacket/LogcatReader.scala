package fi.akisaarinen.smartdiet.measurement.networkpacket

import scala.util.parsing.combinator._
import io.Source


object LogcatReader extends RegexParsers {
  case class Line(level: String, id: String, process: Int, msg: String)

  override val skipWhitespace = false

  def CRLF = "\r\n" | "\n"
  def NUM = "[ 0-9.]".r
  def ANY = "[^\r\n]".r
  def TAG = "[^(]".r
  def LEVEL = "[A-Za-z]".r

  def file: Parser[List[Line]] = repsep(line, CRLF) <~ (CRLF?)
  def line: Parser[Line] = (LEVEL) ~ "/" ~ (TAG*) ~ "(" ~ (NUM*) ~ "): " ~ (ANY*) ^^ {
    case level ~ "/" ~ id ~ "(" ~ process ~ "): " ~ msg =>
      Line(level.mkString(""), id.mkString("").trim, process.mkString("").trim.toInt, msg.mkString(""))
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