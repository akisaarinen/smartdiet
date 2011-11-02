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

import fi.akisaarinen.smartdiet.measurement.networkpacket.DmesgReader.Line

object TrafficMonitorParser {
  val InitLine = "TM :: MODULE INITIALIZATION, timestamp ([0-9]+)".r
  val PacketLine = "TM ([<>=]+) ([0-9]+) ([^,]+), ack ([0-9]+), seq ([0-9]+), size ([0-9]+).*".r

  def parse(lines: List[Line]): List[NetworkPacket] = {
    val (startTime, localOffset) = lines.toStream.flatMap {
      line => line.msg match {
        case InitLine(timestamp) => Some((timestamp.toLong, line.ts))
        case _ => None
      }
    }.headOption match {
      case Some(x) => x
      case None => sys.error("No initialization line found from traffic log. Maybe module wasn't loaded correctly?")
    }
    val timestampAtZero = startTime / 1000.0 - localOffset * 1000.0
    lines.flatMap {
      line => line.msg match {
        case PacketLine(dir, connId, event, ackId, seqId, size) =>
          val timestamp = (timestampAtZero + line.ts * 1000).toLong
          Some(NetworkPacket(timestamp,
            connId.toInt,
            dir match {
              case "<=" => In
              case "=>" => Out
            },
            event match {
              case _ => UnknownEvent
            },
            size.toInt,
            ackId.toLong,
            seqId.toLong))
        case other => None
      }
    }
  }
}