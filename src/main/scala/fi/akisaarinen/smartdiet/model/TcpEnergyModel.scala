package fi.akisaarinen.smartdiet.model

import fi.akisaarinen.smartdiet.measurement.networkpacket.NetworkPacket

case class TcpEnergyEstimate(energyJoules: Double, avgPowerWatts: Double)
trait TcpEnergyModel {
  def calculate(packets: List[NetworkPacket]): TcpEnergyEstimate
}