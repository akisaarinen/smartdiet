package fi.akisaarinen.smartdiet.model

import scala.math.max
import fi.akisaarinen.smartdiet.measurement.networkpacket.{Out, In, NetworkPacket}

class WlanEnergyModel extends TcpEnergyModel {
  private val WLAN_TIMEOUT_MS = 100.0

  // Packet processing time
  private val WLAN_MAX_PACKET_SIZE_BYTES = 1500.0
  private val WLAN_MAX_THROUGHPUT_BYTES_PER_SECOND = 4000000.0
  private val WLAN_PACKET_PROCESSING_TIME_MS = (WLAN_MAX_PACKET_SIZE_BYTES / WLAN_MAX_THROUGHPUT_BYTES_PER_SECOND) * 1000.0

  // Power values
  private val WLAN_P_TRANSMIT = 1.258
  private val WLAN_P_RECEIVE = 1.181
  private val WLAN_P_SLEEP = 0.042
  private val WLAN_P_IDLE = 0.884

  override def calculate(packets: List[NetworkPacket]) = calculate(packets, 6)

  def calculate(packets: List[NetworkPacket], burstThresholdMs: Int): TcpEnergyEstimate = {
    val packetPairs = packets.take(max(packets.size - 1, 0)).zip(packets.drop(1))
    var energyJoules = 0.0
    var avgPowerWatts = 0.0

    if (packets.size > 0) {
      var currentBurst = List(packets.head)
      packetPairs.foreach { case(p1, p2) =>
        val packetInterval = p2.timestamp - p1.timestamp
        val isBurstOngoing = packetInterval <= burstThresholdMs
        if (p2.timestamp < p1.timestamp) sys.error("Negative time diff between packets")
        if (isBurstOngoing) {
          currentBurst = currentBurst ::: p2 :: Nil
        } else {
          energyJoules += wlanEnergyForBurst(currentBurst, Some(p2))
          currentBurst = List(p2)
        }
      }
      energyJoules += wlanEnergyForBurst(currentBurst.toList, None)
    }

    TcpEnergyEstimate(energyJoules, avgPowerWatts)
  }

  private def wlanEnergyForBurst(burst: List[NetworkPacket], nextPacket: Option[NetworkPacket]): Double = {
    val burstLength = burst.last.timestamp - burst.head.timestamp
    val intervalToNextPacket = nextPacket match {
      case Some(packet) => packet.timestamp - burst.last.timestamp
      case None => WLAN_TIMEOUT_MS
    }

    val tBurst = burstLength + WLAN_PACKET_PROCESSING_TIME_MS
    val tIdle = if (intervalToNextPacket > WLAN_TIMEOUT_MS) WLAN_TIMEOUT_MS else intervalToNextPacket
    val tSleep = if (intervalToNextPacket > WLAN_TIMEOUT_MS) intervalToNextPacket - WLAN_TIMEOUT_MS else 0.0

    ((tBurst * wlanPowerConstantForBurst(burst)) + (tIdle * WLAN_P_IDLE) + (tSleep * WLAN_P_SLEEP)) / 1000.0
  }

  private def wlanPowerConstantForBurst(burst: List[NetworkPacket]): Double = {
    val receivedBytes = burst.filter(_.direction == In).map(_.size).foldLeft(0)(_+_)
    val transmitBytes = burst.filter(_.direction == Out).map(_.size).foldLeft(0)(_+_)
    val isReceiveBurst = receivedBytes >= transmitBytes
    if (isReceiveBurst) WLAN_P_RECEIVE else WLAN_P_TRANSMIT
  }
}