package fi.akisaarinen.smartdiet.measurement.networkpacket

import scala.util.parsing.combinator._

object Nw extends RegexParsers {
  override val skipWhitespace = false
  def CRLF = "\r\n" | "\n"
  def SPACES = "[ \t]+".r  
  def TXT = "[^ \r\n]".r
  def file: Parser[List[List[String]]] = repsep(record, CRLF) <~ (CRLF?)
  def record: Parser[List[String]] = repsep(field, SPACES)
  def field: Parser[String] = (TXT*) ^^ { case ls => ls.mkString("") }
  def parse(s: String): List[List[String]] = parseAll(file, s) match {
    case Success(res, _) => res
    case e => throw new Exception(e.toString)
  }
}