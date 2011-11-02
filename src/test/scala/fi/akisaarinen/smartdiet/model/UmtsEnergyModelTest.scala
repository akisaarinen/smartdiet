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

import org.scalatest.FunSuite
import fi.akisaarinen.smartdiet.measurement.networkpacket._

class UmtsEnergyModelTest extends FunSuite {
  val model = new UmtsEnergyModel

  test("without packets") {
    expect(0.0) { model.calculate(List()).energyJoules }
  }
  test("single incoming packet") {
    val packets = packet(0,103,In) :: Nil
    expect(7.95985) { model.calculate(packets).energyJoules }
  }
  test("single incoming burst") {
    val packets =
      packet(10,103,In) ::
      packet(20,123,In) ::
      packet(30,432,Out) ::
      packet(60,432,In) ::
      packet(110,423,In) ::
      Nil
    expect(8.057115) { model.calculate(packets).energyJoules }
  }
  test("three separate bursts") {
    val packets =
      packet(0,103,In) ::
      packet(100,123,In) ::
      packet(1200,432,Out) ::
      packet(1240,432, Out) ::
      packet(8240,432,In) ::
      Nil
    expect(14.446036) { model.calculate(packets).energyJoules }
  }

  def packet(timestamp: Long, size: Int, direction: Direction) = {
    NetworkPacket(timestamp, 1, direction, UnknownEvent, size, 0, 0)
  }
}