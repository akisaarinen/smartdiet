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

package fi.akisaarinen.smartdiet.energyusage.calltree

import fi.akisaarinen.smartdiet.measurement.csv.Csv
import scala.io.Source

object MethodCallReader {
  def readFromFile(filename: String): IndexedSeq[MethodCall] = {
    val data = Source.fromFile(filename).getLines()
    data.drop(1).map { line =>
      val record = Csv.parseLine(line)
      MethodCall(record(1).toInt,
        toMethodCode(record(2)),
        record(3).toDouble,
        record(4),
        record(5).toInt,
        record(6).toInt,
        record.drop(6).headOption match {
          case Some("") => List()
          case Some(s) => toPacketList(s)
          case None => List()
        })
    }.toIndexedSeq
  }

  private def toMethodCode(s: String) = s match {
    case "ent" => Enter
    case "xit" => Exit
    case "unr" => Unroll
    case other => error("Unknown method code: " + other)
  }

  private def toPacketList(s: String) = s.split(",").map(_.toInt).toList
}