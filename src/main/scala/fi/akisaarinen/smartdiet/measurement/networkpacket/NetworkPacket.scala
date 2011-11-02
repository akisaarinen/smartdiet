package fi.akisaarinen.smartdiet.measurement.networkpacket

sealed class Direction
case object In extends Direction
case object Out extends Direction

sealed class Event
case object UnknownEvent extends Event

case class NetworkPacket(timestamp: Long, connId: Int, direction: Direction, event: Event, size: Int, ackId: Long, seqId: Long)