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