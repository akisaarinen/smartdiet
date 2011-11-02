package fi.akisaarinen.smartdiet.measurement.networkpacket

import org.scalatest.FunSuite
import io.Source
import fi.akisaarinen.smartdiet.measurement.networkpacket.DmesgReader.Line

class DmesgReaderTest extends FunSuite {
  test("simple dmesg") {
    val data = Source.fromFile("test-data/twitter-network.dmesg").getLines().mkString("\n")
    val lines = DmesgReader.parse(data)
    expect(1165) { lines.size }
    expect(Line(6, 989.979370, "TM :: MODULE INITIALIZATION, timestamp 1315820509665252")) { lines.head }
  }
}