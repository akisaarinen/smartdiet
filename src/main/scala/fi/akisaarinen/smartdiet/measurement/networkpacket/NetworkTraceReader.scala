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
import scala.io.Source

object NetworkTraceReader {
  def readFromFile(filename: String): List[NetworkPacket] = {
    val data = Source.fromFile(filename).getLines()
    val nw = Nw.parse(data.mkString("\n"))
    nw.drop(1).map { record =>
      NetworkPacket(record(0).toLong,
        record(1).toInt,
        record(2) match {
          case "2" => Out
          case "1" => In
          case other => sys.error("Unknown direction: " + other)

        },
        record(3) match {
          case _ => UnknownEvent
        },
        record(4).toInt,
        record(5).toLong,
        record(6).toLong)
    }
  }
}