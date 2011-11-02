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