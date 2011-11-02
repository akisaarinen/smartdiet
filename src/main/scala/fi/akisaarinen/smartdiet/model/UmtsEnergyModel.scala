/*
 * This file is part of SmartDiet.
 *
 * Copyright (C) 2011, Aki Saarinen.
 *
 * SmartDiet was developed in affiliation with Aalto University School
 * of Science, Department of Computer Science and Engineering. For
 * more information about the department, see <http://cse.aalto.fi/>.
 *
 * SmartDiet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SmartDiet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SmartDiet.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.akisaarinen.smartdiet.model

import fi.akisaarinen.smartdiet.measurement.networkpacket.NetworkPacket
import scala.math.max

class UmtsEnergyModel extends TcpEnergyModel {
  private val INFINITY = 1.0/0
  private val UMTS_VOLTAGE = 3.97

  case class EnergyState(name: String, timeoutMs: Double, power: Double)

  private val STATE_DCH = EnergyState("DCH", 3500.0, UMTS_VOLTAGE * 0.245)
  private val STATE_FACH = EnergyState("FACH", 8500.0, UMTS_VOLTAGE * 0.135)
  private val STATE_PCH_OR_IDLE = EnergyState("PCH_OR_IDLE", INFINITY, UMTS_VOLTAGE * 0.016)

  private val states = List(STATE_DCH, STATE_FACH, STATE_PCH_OR_IDLE)


  override def calculate(packets: List[NetworkPacket]) = calculate(packets, includeTailEnergy = true)

  def calculate(packets: List[NetworkPacket], includeTailEnergy: Boolean): TcpEnergyEstimate = {
    val packetPairs = packets.take(max(packets.size - 1, 0)).zip(packets.drop(1))
    var energyJoules = 0.0
    var avgPowerWatts = 0.0

    if (packets.size > 0) {
      packetPairs.foreach { case (p1, p2) =>
        val packetInterval = p2.timestamp - p1.timestamp
        energyJoules += umtsEnergyForPacket(packetInterval)
      }
      if (includeTailEnergy) {
        val lastPacketTailTime = STATE_DCH.timeoutMs + STATE_FACH.timeoutMs
        energyJoules += umtsEnergyForPacket(lastPacketTailTime)
      }
    }

    TcpEnergyEstimate(energyJoules, avgPowerWatts)
  }

  private def umtsEnergyForPacket(packetInterval: Double): Double = {
    val (timeLeft, energyJoules) = (packetInterval, 0.0)
    states.foldLeft((timeLeft, energyJoules)) { case ((timeLeft, energyJoules), state) =>
      val timeInState = Math.min(state.timeoutMs,timeLeft)
      val energyInState = timeInState / 1000.0 * state.power
      ((timeLeft - timeInState), energyJoules + energyInState)
    }._2
  }
}