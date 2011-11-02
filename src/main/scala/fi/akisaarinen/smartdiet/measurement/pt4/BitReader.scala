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

import java.io.{FileInputStream, File, DataInputStream}
import org.joda.time.DateTime

class BitReader(dis: DataInputStream) {
  def this(fis: FileInputStream) = this(new DataInputStream(fis))
  def this(file: File) = this(new FileInputStream(file))
  def this(filename: String) = this(new File(filename))

  def skipBytes(count: Int) {
    dis.skipBytes(count)
  }

  def readUnsignedByte(): Int = {
    val b = dis.readByte() & 0xFF
    // println("Read unsigned byte: " + String.format("0x%02X", b.asInstanceOf[AnyRef]))
    b
  }

  def readSignedByte(): Int = {
    val b = dis.readByte().toInt
    //println("Read signed byte: " + String.format("%d", b.asInstanceOf[AnyRef]))
    b
  }

  def readFloat32(): Float = {
    java.lang.Float.intBitsToFloat(readUnsignedInt32)
  }

  def readUnsignedShort(): Int = {
    (readUnsignedByte() << 0) |
    (readSignedByte() << 8)
  }

  def readSignedShort(): Int = {
    (readUnsignedByte() << 0) |
    (readUnsignedByte() << 8)
  }

  def readUnsignedInt32(): Int = {
    (readUnsignedByte() << 0) |
    (readUnsignedByte() << 8) |
    (readUnsignedByte() << 16) |
    (readUnsignedByte() << 24)
  }

  def readSignedInt32(): Int = {
    (readUnsignedByte() << 0) |
    (readUnsignedByte() << 8) |
    (readUnsignedByte() << 16) |
    (readSignedByte() << 24)
  }

  def readUnsignedInt64(): Long = {
    (readUnsignedByte().toLong << 0) |
    (readUnsignedByte().toLong << 8) |
    (readUnsignedByte().toLong << 16) |
    (readUnsignedByte().toLong << 24) |
    (readUnsignedByte().toLong << 32) |
    (readUnsignedByte().toLong << 40) |
    (readUnsignedByte().toLong << 48) |
    (readUnsignedByte().toLong << 56)
  }

  def readSignedInt64(): Long = {
    (readUnsignedByte().toLong << 0) |
    (readUnsignedByte().toLong << 8) |
    (readUnsignedByte().toLong << 16) |
    (readUnsignedByte().toLong << 24) |
    (readUnsignedByte().toLong << 32) |
    (readUnsignedByte().toLong << 40) |
    (readUnsignedByte().toLong << 48) |
    (readUnsignedByte().toLong << 56)
  }

  def readString(len: Int) = {
    val arr = new Array[Byte](20)
    dis.readFully(arr)
    new String(arr).trim
  }

  def readDateTime(): DateTime = {
    val epochShiftValue = 62135596800000L
    val value = readSignedInt64()
    val kind = value >> 62
    val ticks = value & 0x3FFFFFFFFFFFFFFFL
    return new DateTime((ticks / 10000) - epochShiftValue)
  }

  def isFinished(): Boolean = {
    dis.available == 0
  }
}

