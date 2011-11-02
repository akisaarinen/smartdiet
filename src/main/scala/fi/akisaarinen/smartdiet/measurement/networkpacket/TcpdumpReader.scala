package fi.akisaarinen.smartdiet.measurement.networkpacket

import scala.util.parsing.combinator._
import io.Source

object TcpdumpReader extends RegexParsers {
  case class IpInfo(ts: String, srcIp: String, srcPort: Int, dstIp: String, dstPort: Int)

  abstract class Packet
  case class TcpPacket(ipInfo: IpInfo, length: Long) extends Packet
  case class UdpPacket(ipInfo: IpInfo, length: Long) extends Packet

  override val skipWhitespace = false

  def TIMESTAMP = "[0-9.:]".r
  def IP = "[0-9]+.[0-9]+.[0-9]+.[0-9]+".r
  def CRLF = "\r\n" | "\n"
  def NUM = "[0-9]".r
  def ANY = "[^\r\n]".r
  def FLAG_CHARS = "[^\\]]".r

  def file = repsep(line, CRLF) <~ (CRLF?)

  def ts: Parser[String] = (TIMESTAMP*) ~ " IP " ^^ { case ts ~ _ => ts.mkString("") }
  def ip: Parser[String] = (IP*) ^^ { case ip => ip.mkString("") }
  def port: Parser[Int] = (NUM*) ^^ { case port => port.mkString("").toInt }
  def ipAndPort: Parser[(String, Int)] =
    ip ~ "." ~ port ^^ { case ip ~ _ ~ port => (ip.mkString(""), port) }

  def ipInfo: Parser[IpInfo] =
    ts ~ ipAndPort ~ " > " ~ ipAndPort ^^ {
      case ts ~ ((srcIp, srcPort)) ~ _ ~ ((dstIp, dstPort)) =>
        IpInfo(ts, srcIp, srcPort, dstIp, dstPort)
    }

  def udpLine: Parser[UdpPacket] =
    ipInfo ~ ": UDP, length " ~ (NUM*) ^^ {
      case ipInfo ~ _ ~ length =>
        UdpPacket(ipInfo, length.mkString("").toLong)
    }

  def NO_COMMA = "[^,\\r\\n]".r
  def tcpParam = (NO_COMMA*) ^^ { case c => c.mkString("").trim }

  def tcpLine: Parser[TcpPacket] =
    ipInfo ~ ": Flags [" ~ (FLAG_CHARS*) ~ "]," ~ repsep(tcpParam,",") ^^ {
      case ipInfo ~ _ ~ tcpFlags ~ _ ~ params =>
        val Length = "length ([0-9]+)".r
        val length = params.flatMap { p =>
          p match {
            case Length(l) => Some(l.toInt)
            case _ => None
          }
        }.head
        TcpPacket(ipInfo, length)
    }

  def line = (udpLine | tcpLine)

  def parse(s: String) = parseAll(file, s) match {
    case Success(res, _) => res
    case e => throw new Exception(e.toString)
  }

  def readFromFile(filename: String) = {
    val data = Source.fromFile(filename).getLines()
    parse(data.mkString("\n"))
  }
}