package fi.akisaarinen.smartdiet.measurement.csv

import scala.util.parsing.combinator._

// A CSV parser based on RFC4180
// http://tools.ietf.org/html/rfc4180
// http://stackoverflow.com/questions/5063022/use-scala-parser-combinator-to-parse-csv-files
object Csv extends RegexParsers {
  override val skipWhitespace = false

  def COMMA = ","
  def DQUOTE = "\""
  def DQUOTE2 = "\"\"" ^^ { case _ => "\"" }
  def CRLF = "\r\n" | "\n"
  def TXT = "[^\",\r\n]".r
  def SPACES = "[ \t]+".r

  def file: Parser[List[List[String]]] = repsep(record, CRLF) <~ (CRLF?)
  def record: Parser[List[String]] = repsep(field, COMMA)
  def field: Parser[String] = escaped | nonescaped
  def escaped: Parser[String] = {
    ((SPACES?) ~> DQUOTE ~> ((TXT | COMMA | CRLF | DQUOTE2)*) <~ DQUOTE <~ (SPACES?)) ^^ {
      case ls => ls.mkString("")
    }
  }
  def nonescaped: Parser[String] = (TXT*) ^^ { case ls => ls.mkString("") }
  
  def parseLine(s: String) = parseAll(record, s) match {
    case Success(res, _) => res
    case e => sys.error(e.toString)
  }
  
  def parseFile(s: String) = parseAll(file, s) match {
    case Success(res, _) => res
    case e => sys.error(e.toString)
  }
}