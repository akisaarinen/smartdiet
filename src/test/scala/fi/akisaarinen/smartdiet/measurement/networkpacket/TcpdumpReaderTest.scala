package fi.akisaarinen.smartdiet.measurement.networkpacket

import org.scalatest.FunSuite
import io.Source
import fi.akisaarinen.smartdiet.measurement.networkpacket.TcpdumpReader.{TcpPacket, UdpPacket}

class TcpdumpReaderTest extends FunSuite {
  test("example tcp dump") {
    val data = Source.fromFile("test-data/tcp.dump").getLines().mkString("\n")
    val lines = TcpdumpReader.parse(data)

    expect(12) { lines.size }
    expect(2) { lines.filter(_.isInstanceOf[UdpPacket]).size }
    expect(10) { lines.filter(_.isInstanceOf[TcpPacket]).size }
  }
}