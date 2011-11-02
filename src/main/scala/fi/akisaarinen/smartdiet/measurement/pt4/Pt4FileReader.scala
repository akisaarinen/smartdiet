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

class Pt4FileReader(filename: String) extends BitReader(filename) {
  def readHeader() = {
    val headerSize = readUnsignedInt32()
    val identifier = readString(20)
    val batterySize = readUnsignedInt32()
    val captureDate = readDateTime()
    val serial = readString(20)
    val calibrationStatus = readUnsignedInt32()
    val voutSetting = readUnsignedInt32()
    val voutValue = readFloat32()
    val hardwareRate = readUnsignedInt32()
    val softwareRate = readFloat32()

    val powerField = readUnsignedInt32()
    val currentField = readUnsignedInt32()
    val voltageField = readUnsignedInt32()

    val captureSetting = readString(30)
    val swVersion = readString(10)
    val runMode = readUnsignedInt32()
    val exitCode = readUnsignedInt32()
    val totalCount = readSignedInt64()

    val statusOffset = readUnsignedShort()
    val statusSize = readUnsignedShort()
    val sampleOffset = readUnsignedShort()
    val sampleSize = readUnsignedShort()

    val initialMainVoltage = readUnsignedShort()
    val initialUsbVoltage = readUnsignedShort()
    val initialAuxVoltage = readUnsignedShort()

    val captureDataMask = readUnsignedShort()

    val sampleCount = readUnsignedInt64()
    val missingCount = readUnsignedInt64()

    val sumMainVoltage = readFloat32()
    val sumMainCurrent = readFloat32()
    val sumMainPower = readFloat32()

    val sumUsbVoltage = readFloat32()
    val sumUsbCurrent = readFloat32()
    val sumUsbPower = readFloat32()

    val sumAuxVoltage = readFloat32()
    val sumAuxCurrent = readFloat32()
    val sumAuxPower = readFloat32()

    // Padded to 272 bytes, where status packet begins
    skipBytes(60)

    val applicationInfo = ApplicationInfo(
      captureSetting,
      swVersion,
      runMode,
      exitCode)
    val samples = Samples(
      captureDataMask,
      totalCount,
      statusOffset,
      statusSize,
      sampleOffset,
      sampleSize)

    val sumMain = SumValues(sumMainVoltage, sumMainCurrent, sumMainPower)
    val sumUsb = SumValues(sumUsbVoltage, sumUsbCurrent, sumUsbPower)
    val sumAux = SumValues(sumAuxVoltage, sumAuxCurrent, sumAuxPower)
    val sum = Sum(sumMain, sumUsb, sumAux)

    Header(headerSize,
      identifier,
      batterySize,
      captureDate,
      serial,
      calibrationStatus,
      voutSetting,
      voutValue,
      hardwareRate,
      softwareRate,
      powerField,
      currentField,
      voltageField,
      applicationInfo,
      samples,
      sum)
  }

  def readStatusPacket(): StatusPacket = {
    val length = readUnsignedByte()
    val packetType = readUnsignedByte()
    val firmwareVersion = readUnsignedByte()
    val protocolVersion = readUnsignedByte()

    val mainFineCurrent = readSignedShort()
    val usbFineCurrent = readSignedShort()
    val auxFineCurrent = readSignedShort()

    val voltage1 = readUnsignedShort()

    val mainCoarseCurrent = readSignedShort()
    val usbCoarseCurrent = readSignedShort()
    val auxCoarseCurrent = readSignedShort()

    val voltage2 = readUnsignedShort()

    val outputVoltageSetting = readUnsignedByte()
    val temperature = readUnsignedByte()
    val status = readUnsignedByte()

    skipBytes(3)

    val serialNumber = readUnsignedShort()
    val sampleRate = readUnsignedByte()

    skipBytes(11)

    val initialUsbVoltage = readUnsignedShort()
    val initialAuxVoltage = readUnsignedShort()
    val hardwareRevision = readUnsignedByte()

    skipBytes(11)

    val eventCode = readUnsignedByte()

    skipBytes(2)

    val checkSum = readUnsignedByte()

    // padded to 1024, where sample data begins
    skipBytes(692)

    val fineCurrent = Currents(mainFineCurrent, usbFineCurrent, auxFineCurrent)
    val coarseCurrent = Currents(mainCoarseCurrent, usbCoarseCurrent, auxCoarseCurrent)


    // Only support hardware revisions starting from 3 because older versions
    // have different interpretation of sample data
    if (hardwareRevision < 3) {
      throw new UnsupportedHardwareException("Old hardware revision (%d) is not supported due to changes in interpretation of sample data".format(hardwareRevision))
    }

    StatusPacket(length,
      packetType,
      firmwareVersion,
      protocolVersion,
      fineCurrent,
      coarseCurrent,
      voltage1,
      voltage2,
      outputVoltageSetting,
      temperature,
      status,
      serialNumber,
      sampleRate,
      initialUsbVoltage,
      initialAuxVoltage,
      hardwareRevision,
      eventCode,
      checkSum)
  }

  def readSample(header: Header): RawSample = {
    val mainCurrent = if ((header.samples.captureDataMask & 0x1000) != 0) readSignedShort() else 0
    val usbCurrent = if ((header.samples.captureDataMask & 0x2000) != 0) readSignedShort() else 0
    val auxCurrent = if ((header.samples.captureDataMask & 0x4000) != 0) readSignedShort() else 0
    val voltage = readUnsignedShort()

    RawSample(mainCurrent, usbCurrent, auxCurrent, voltage)
  }
}

object Pt4FileReader {
  def readAsVector(filename: String): (Header, StatusPacket, IndexedSeq[Sample]) = {
    val reader = new Pt4FileReader(filename)
    val header = reader.readHeader()
    val statusPacket = reader.readStatusPacket()
    var seq = IndexedSeq[Sample]()
    while (!reader.isFinished()) {
      val rawSample = reader.readSample(header)
      val sample = Sample.fromRaw(rawSample, statusPacket)
      seq = seq :+ sample
    }
    (header, statusPacket, seq)
  }
}