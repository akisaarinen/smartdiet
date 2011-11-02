package fi.akisaarinen.smartdiet.measurement.networkpacket

import org.scalatest.FunSuite
import io.Source

class TrafficMonitorParserTest extends FunSuite {
  test("twitter traffic") {
    val data = Source.fromFile("test-data/twitter-network.dmesg").getLines().mkString("\n")
    val lines = DmesgReader.parse(data)
    val packets = TrafficMonitorParser.parse(lines)
    expect(835) { packets.size }
    expect(NetworkPacket(1315820509682L, 5555, Out, UnknownEvent, 78, 822801964L, 3400168017L)) { packets.head }
    expect(NetworkPacket(1315820575225L, 5555, In, UnknownEvent, 76, 1102017105L, 286127660L)) { packets.last }
  }
}