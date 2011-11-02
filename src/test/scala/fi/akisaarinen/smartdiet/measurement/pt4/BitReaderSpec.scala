package fi.akisaarinen.smartdiet.measurement.pt4

import org.specs.Specification

class BitReaderSpec extends Specification {
  "Bit reader with file containing various little endian numbers" should {
    val reader = new BitReader("test-data/test.pt4")

    "read unsigned byte" in {
      reader.readUnsignedByte must equalTo(0x90)
    }

    "read signed byte" in {
      reader.readSignedByte must equalTo(-112)
    }

    "read unsigned short" in {
      reader.skipBytes(0x1F)
      reader.readUnsignedShort must equalTo(-2014)
    }

    "read signed short" in {
      reader.skipBytes(0x1F)
      reader.readSignedShort must equalTo(0xF822)
    }

    "read unsigned int" in {
      reader.skipBytes(0x1F)
      reader.readUnsignedInt32 must equalTo(0xCDA6F822)
    }

    "read signed int" in {
      reader.skipBytes(0x1F)
      reader.readSignedInt32 must equalTo(-844695518)
    }

    "read unsigned long" in {
      reader.skipBytes(0x43)
      reader.readUnsignedInt64 must equalTo(0x9C40000000138840L)
    }

    "read signed long" in {
      reader.skipBytes(0x43)
      reader.readSignedInt64 must equalTo(-7187745005282031552L)
    }
  }
}