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