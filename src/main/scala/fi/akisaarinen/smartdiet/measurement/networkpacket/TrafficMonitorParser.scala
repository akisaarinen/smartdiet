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