package fi.akisaarinen.smartdiet.measurement.pt4

import org.specs.Specification
import org.joda.time.DateTime

class Pt4FileReaderSpec extends Specification {
  val delta = 0.0001

  "Bit reader with test.pt4" should {
    val captureDate = new DateTime(2011,3,2,15,51,58,622)
    val (header, status, samples) = Pt4FileReader.readAsVector("test-data/test.pt4")

    "read header" in {
      header.headerSize must equalTo(144)
      header.identifier must equalTo("Power Tool")
      header.batterySize must equalTo(1000)
      header.captureDate must equalTo(captureDate)
      header.serial must equalTo("2911")
      header.calibrationStatus must equalTo(0)
      header.voutSetting must equalTo(0)
      header.voutValue must beCloseTo(3.7, delta)
      header.hardwareRate must equalTo(5000)
      header.softwareRate must beCloseTo(5000.0, delta)
      header.powerField must equalTo(9)
      header.currentField must equalTo(9)
      header.voltageField must equalTo(9)


      header.applicationInfo.captureSetting must equalTo("ATA")
      header.applicationInfo.swVersion must equalTo("4.0.3e")
      header.applicationInfo.runMode must equalTo(1)
      header.applicationInfo.exitCode must equalTo(0)

      header.samples.totalCount must equalTo(5655L)
      header.samples.statusOffset must equalTo(272)
      header.samples.statusSize must equalTo(60)
      header.samples.sampleOffset must equalTo(1024)
      header.samples.sampleSize must equalTo(4)
    }

    "read status packet" in {
      status.length must equalTo(59)
      status.packetType must equalTo(0x10)
      status.firmwareVersion must equalTo(19)
      status.protocolVersion must equalTo(17)

      status.voltage1 must equalTo(29564)
      status.voltage2 must equalTo(29564)

      status.fineCurrent.main must equalTo(1961)
      status.fineCurrent.usb must equalTo(48)
      status.fineCurrent.aux must equalTo(316)

      status.coarseCurrent.main must equalTo(65531)
      status.coarseCurrent.usb must equalTo(27)
      status.coarseCurrent.aux must equalTo(224)

      status.voltage1 must equalTo(29564)
      status.voltage2 must equalTo(29564)

      status.outputVoltageSetting must equalTo(170)
      status.temperature must equalTo(30)
      status.status must equalTo(16)
      status.serialNumber must equalTo(2911)
      status.sampleRate must equalTo(5)
      status.initialUsbVoltage must equalTo(1024)
      status.initialAuxVoltage must equalTo(4609)
      status.hardwareRevision must equalTo(4)

      status.eventCode must equalTo(0)
      status.checkSum must equalTo(31)
    }

    "read a few first samples successfully" in {
      val currentTolerance = 0.001
      val voltageTolerance = 0.001

      val sample1 = samples.head
      sample1.mainCurrent must beCloseTo(3.152, currentTolerance)
      sample1.usbCurrent must beCloseTo(0.0, currentTolerance)
      sample1.auxCurrent must beCloseTo(0.0, currentTolerance)
      sample1.voltage must beCloseTo(3.695, currentTolerance)

      val sample2 = samples.drop(1).head
      sample2.mainCurrent must beCloseTo(3.028, currentTolerance)
      sample2.usbCurrent must beCloseTo(0.0, currentTolerance)
      sample2.auxCurrent must beCloseTo(0.0, currentTolerance)
      sample2.voltage must beCloseTo(3.695, currentTolerance)

      val sample3 = samples.drop(2).head
      sample3.mainCurrent must beCloseTo(3.536, currentTolerance)
      sample3.usbCurrent must beCloseTo(0.0, currentTolerance)
      sample3.auxCurrent must beCloseTo(0.0, currentTolerance)
      sample3.voltage must beCloseTo(3.695, currentTolerance)
    }

    "read all samples and check mix/max values" in {
      samples.map(_.voltage).min must beGreaterThan(3.60)
      samples.map(_.voltage).max must beLessThan(3.80)
      samples.map(_.mainCurrent).min must beGreaterThan(2.573)
      samples.map(_.mainCurrent).max must beLessThan(251.6)
      samples.map(_.auxCurrent).sum must equalTo(0.0)
      samples.map(_.usbCurrent).sum must equalTo(0.0)
    }

    "count number of samples" in {
      samples.size must equalTo(5655)
    }
  }

  /*
  "Bit reader with 5 minute recording of N810 usage" should {
    val captureDate = new DateTime(2011,3,11,10,18,1,271)
    val (reader, header, status, sampleStream) = Pt4FileReader.readAsStream("n810-5min.pt4")

    "read header" in {
      header.headerSize must equalTo(144)
      header.identifier must equalTo("Power Tool")
      header.batterySize must equalTo(1000)
      header.captureDate must equalTo(captureDate)
      header.serial must equalTo("2911")
      header.calibrationStatus must equalTo(0)
      header.voutSetting must equalTo(0)
      header.voutValue must beCloseTo(3.7, delta)
      header.hardwareRate must equalTo(5000)
      header.softwareRate must beCloseTo(5000.0, delta)
      header.powerField must equalTo(9)
      header.currentField must equalTo(9)
      header.voltageField must equalTo(9)


      header.applicationInfo.captureSetting must equalTo("ATA")
      header.applicationInfo.swVersion must equalTo("4.0.3e")
      header.applicationInfo.runMode must equalTo(1)
      header.applicationInfo.exitCode must equalTo(0)

      header.samples.totalCount must equalTo(1563314L)
      header.samples.statusOffset must equalTo(272)
      header.samples.statusSize must equalTo(60)
      header.samples.sampleOffset must equalTo(1024)
      header.samples.sampleSize must equalTo(4)
    }

    "read status packet" in {
      status.length must equalTo(59)
      status.packetType must equalTo(0x10)
      status.firmwareVersion must equalTo(19)
      status.protocolVersion must equalTo(17)

      /*
      status.voltage1 must equalTo(29556)
      status.voltage2 must equalTo(29556)

      status.fineCurrent.main must equalTo(1961)
      status.fineCurrent.usb must equalTo(48)
      status.fineCurrent.aux must equalTo(316)

      status.coarseCurrent.main must equalTo(65531)
      status.coarseCurrent.usb must equalTo(27)
      status.coarseCurrent.aux must equalTo(224)

      status.voltage1 must equalTo(29564)
      status.voltage2 must equalTo(29564)

      status.outputVoltageSetting must equalTo(170)
      status.temperature must equalTo(30)
      status.status must equalTo(16)
      status.serialNumber must equalTo(2911)
      status.sampleRate must equalTo(5)
      status.initialUsbVoltage must equalTo(1024)
      status.initialAuxVoltage must equalTo(4609)
      status.hardwareRevision must equalTo(4)
      */

      status.eventCode must equalTo(0)
      status.checkSum must equalTo(102) //TODO: CHeck that this is the real checksum
    }

    "read all samples and check mix/max values" in {
      val samples = sampleStream.toList
      samples.size must equalTo(1563314L)
      reader.isFinished must equalTo(true)

      samples.map(_.voltage).min must beGreaterThan(-0.1)
      samples.map(_.voltage).max must beLessThan(3.80)
      samples.map(_.mainCurrent).min must beGreaterThan(-0.01)
      samples.map(_.mainCurrent).max must beLessThan(8192.1)
      samples.map(_.auxCurrent).sum must equalTo(0.0)
      samples.map(_.usbCurrent).sum must equalTo(0.0)
    }
  }
  */
}