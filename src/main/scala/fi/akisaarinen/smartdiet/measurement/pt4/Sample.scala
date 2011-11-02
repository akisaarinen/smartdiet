package fi.akisaarinen.smartdiet.measurement.pt4

case class Sample(mainCurrent: Double, usbCurrent: Double, auxCurrent: Double, voltage: Double)

object Sample {
  def fromRaw(rawSample: RawSample, statusPacket: StatusPacket) = {
    val mainCurrent = RawSample.Current(rawSample.mainCurrent)
    val usbCurrent = RawSample.Current(rawSample.usbCurrent)
    val auxCurrent = RawSample.Current(rawSample.auxCurrent)
    val voltage = RawSample.Voltage(rawSample.voltage)

    Sample(mainCurrent.inMilliAmps,
      usbCurrent.inMilliAmps,
      auxCurrent.inMilliAmps,
      voltage.inVolts)
  }
}