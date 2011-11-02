package fi.akisaarinen.smartdiet.measurement.pt4

case class StatusPacket(length: Int,
                   packetType: Int,
                   firmwareVersion: Int,
                   protocolVersion: Int,
                   fineCurrent: Currents,
                   coarseCurrent: Currents,
                   voltage1: Int,
                   voltage2: Int,
                   outputVoltageSetting: Int,
                   temperature: Int,
                   status: Int,
                   serialNumber: Int,
                   sampleRate: Int,
                   initialUsbVoltage: Int,
                   initialAuxVoltage: Int,
                   hardwareRevision: Int,
                   eventCode: Int,
                   checkSum: Int)


case class Currents(main: Int, usb: Int, aux: Int)