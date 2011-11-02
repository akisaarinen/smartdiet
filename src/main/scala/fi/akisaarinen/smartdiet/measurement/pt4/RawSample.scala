package fi.akisaarinen.smartdiet.measurement.pt4

import fi.akisaarinen.smartdiet.measurement.pt4.RawSample.{Current, Voltage}

case class RawSample(mainCurrent: Int, usbCurrent: Int, auxCurrent: Int, voltage: Int) {
  def mainCurrentInMilliAmps = {
    Current(mainCurrent).inMilliAmps
  }


  def voltageMissing = Voltage(voltage).missing
  def mainCurrentMissing = Current(mainCurrent).missing
  def usbCurrentMissing = Current(usbCurrent).missing
  def auxCurrentMissing = Current(auxCurrent).missing

  def missing = voltageMissing || mainCurrentMissing || usbCurrentMissing || auxCurrentMissing
}

object RawSample {
  case class Voltage(raw: Int) {
    def missing = (raw == RawSample.missingRawVoltage)
    def marker0 = (raw & RawSample.marker0Mask) != 0
    def marker1 = (raw & RawSample.marker1Mask) != 0
    def inVolts: Double = {
      val currentInVolts = (raw & (~RawSample.markerMask)) * 125.0 / 1e6
      currentInVolts
    }
  }

  case class Current(raw: Int) {
    def missing = (raw == RawSample.missingRawCurrent)
    def inMilliAmps: Double = {
      val isCoarse = (raw & RawSample.coarseMask) != 0
      val currentInMilliAmps = (raw & (~RawSample.coarseMask)) / 1000.0
      if (isCoarse) currentInMilliAmps * 250.0 else currentInMilliAmps
    }
  }

  private val missingRawCurrent = 0x8001
  private val missingRawVoltage = 0xFFFF
  private val coarseMask = 1
  private val marker0Mask = 1
  private val marker1Mask = 2
  private val markerMask = marker0Mask | marker1Mask
}
