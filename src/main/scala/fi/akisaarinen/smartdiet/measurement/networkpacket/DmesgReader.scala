/*
 * This file is part of SmartDiet.
 *
 * Copyright (C) 2011, Aki Saarinen.
 *
 * SmartDiet was developed in affiliation with Aalto University School
 * of Science, Department of Computer Science and Engineering. For
 * more information about the department, see <http://cse.aalto.fi/>.
 *
 * SmartDiet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SmartDiet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SmartDiet.  If not, see <http://www.gnu.org/licenses/>.
 */

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