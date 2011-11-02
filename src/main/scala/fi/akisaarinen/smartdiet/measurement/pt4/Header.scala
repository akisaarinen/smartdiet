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

package fi.akisaarinen.smartdiet.measurement.pt4

import org.joda.time.DateTime

case class Header(headerSize: Int,
                  identifier: String,
                  batterySize: Int,
                  captureDate: DateTime,
                  serial: String,
                  calibrationStatus: Int,
                  voutSetting: Int,
                  voutValue: Double,
                  hardwareRate: Int,
                  softwareRate: Double,
                  powerField: Int,
                  currentField: Int,
                  voltageField: Int,
                  applicationInfo: ApplicationInfo,
                  samples: Samples,
                  sum: Sum) {
  val sampleLengthMs = 1000.0 / hardwareRate.toDouble
  def sampleTimestamp(index: Int) = captureDate.plusMillis((sampleLengthMs*index).toInt)
}

case class ApplicationInfo(captureSetting: String, swVersion: String, runMode: Int, exitCode: Int)
case class Samples(captureDataMask: Int,
                   totalCount: Long,
                   statusOffset: Int,
                   statusSize: Int,
                   sampleOffset: Int,
                   sampleSize: Int)
case class SumValues(voltage: Double, current: Double, power: Double)
case class Sum(main: SumValues, usb: SumValues, aux: SumValues)