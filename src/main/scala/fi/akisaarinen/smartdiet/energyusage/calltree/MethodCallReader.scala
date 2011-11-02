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