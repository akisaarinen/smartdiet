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

package fi.akisaarinen.smartdiet.energyusage

import fi.akisaarinen.smartdiet.measurement.pt4.{Header, Sample, Pt4FileReader}
import fi.akisaarinen.smartdiet.measurement.networkpacket._
import fi.akisaarinen.smartdiet.model.{UmtsEnergyModel, WlanEnergyModel}
import java.io.File

case class Pt4Stats(lengthSeconds: Double, joules: Double, avgMw: Double)

object EnergyUsageAnalyzer {
  def printEstimatesForNwFiles(paths: List[String]) {
    paths.foreach { path =>
      val packets = NetworkTraceReader.readFromFile(path)
      val connections = packets.map(_.connId).distinct.size
      val estimate = (new WlanEnergyModel).calculate(packets)
      println("%s: %f J (%d packets, %d connections)".format(path, estimate.energyJoules, packets.size, connections))
    }
  }

  def printEstimatesForDmesgFiles(paths: List[String]) {
    paths.foreach { path =>
      val lines = DmesgReader.readFromFile(path)
      val packets = TrafficMonitorParser.parse(lines)

      val usedPackets = packets.filter(_.connId != 5555)

      val estimate = (new WlanEnergyModel).calculate(usedPackets)
      println("%s: %f J (using %d/%d)".format(path, estimate.energyJoules, usedPackets.size, packets.size))
    }
  }

  def printSummaryForPt4Files(paths: List[String]) {
    paths.foreach { path =>
      val (header,_,s) = Pt4FileReader.readAsVector(path)

      val stats = statsForPt4(header, s)

      val lengthHours = stats.lengthSeconds / 3600.0
      val avgMa = s.map(_.mainCurrent).sum / s.size
      val mah = avgMa * lengthHours

      val avgV = s.map(_.voltage).sum / s.size

      println("%s: (%d samples)".format(path, s.size))
      println("  Capture date: " + header.captureDate)
      println("  PowerMonitor version: " + header.applicationInfo.swVersion)
      println("  Time: %.2f seconds\n  Samples: %d\n  Consumed energy: %.3f mAh".format(stats.lengthSeconds, s.size, mah))
      println("  Avg Power: %.2f mW\n  Avg Current: %.2f mA\n  Avg voltage: %.2fV".format(stats.avgMw, avgMa, avgV))
      println("  Energy: %.2f J".format(stats.joules))
    }
  }
  private def statsForPt4(header: Header, s: IndexedSeq[Sample]): Pt4Stats = {
    val lengthSeconds = (header.sampleLengthMs * s.size) / 1000.0
    val avgMw = s.map { s => s.mainCurrent * s.voltage }.sum / s.size
    val avgWatts = avgMw / 1000.0
    val joules = avgWatts * lengthSeconds
    Pt4Stats(lengthSeconds, joules, avgMw)
  }

  case class SampleWithTimestamp(timestamp: Long, sample: Sample)
  case class NwAnalysis(packets: IndexedSeq[NetworkPacket],
                        wlanEnergyJoules: Double,
                        umtsEnergyJoules: Double,
                        timespanMs: Long,
                        totalBytes: Long)
  case class PtAnalysis(samples: IndexedSeq[SampleWithTimestamp],
                        energyJoules: Double,
                        timespanMs: Long)
  case class CombinedAnalysis(triggerTime: Long, nw: NwAnalysis, pt: Option[PtAnalysis])

  def findTriggerTimes(logcat: List[LogcatReader.Line]): List[Long] = {
    val triggerLines = logcat.filter(l => l.msg.contains("POWERPROFILER-TRIGGER"))
    val TriggerRegex = "POWERPROFILER-TRIGGER at ([0-9]+)".r
    val triggerTimestamps = triggerLines.map { tline => tline.msg match {
      case TriggerRegex(timestamp) => timestamp.toLong
      case m => sys.error("Not a matching trigger line '%s'".format(m))
    }}
    triggerTimestamps
  }

  def nwAnalysis(triggerTimes: List[Long], lines: List[DmesgReader.Line]): List[NwAnalysis] = {
    val wlanModel = new WlanEnergyModel
    val umtsModel = new UmtsEnergyModel
    val packets = TrafficMonitorParser.parse(lines).toIndexedSeq
    val startTimes = triggerTimes.size match {
      case 0 => List(packets.head.timestamp)
      case _ => triggerTimes
    }
    startTimes.map { startTime =>
      val packetsAfterTrigger = packets.filter(p => p.timestamp >= startTime)
      val triggerAfterThis = triggerTimes.filter(_ > startTime).headOption
      val untilTime = triggerAfterThis match {
        case Some(nextTrigger) =>
          // If there's another trigger after this, select packets until that moment
          nextTrigger
        case None =>
          // No next trigger, select just time range until last packet minus some tolerance
          // value, so that we can filter out the adb re-activation when stopping the
          // recordings.
          val adbTurnOffToleranceMs = 2000
          packets.last.timestamp - adbTurnOffToleranceMs
      }

      val selectedPackets = packetsAfterTrigger.filter(p => p.timestamp <= untilTime)
      val selectedWlanEnergy = wlanModel.calculate(selectedPackets.toList)
      val selectedUmtsEnergy = umtsModel.calculate(selectedPackets.toList, includeTailEnergy = false)
      val timespan = selectedPackets.last.timestamp - selectedPackets.head.timestamp
      val totalBytes = selectedPackets.map(_.size).sum
      NwAnalysis(selectedPackets, selectedWlanEnergy.energyJoules, selectedUmtsEnergy.energyJoules, timespan, totalBytes)
    }
  }

  def readPt4(filename: String): (Option[Header], IndexedSeq[Sample]) = {
      fileExists(filename) match {
        case true =>
          val (header, _, samples) = Pt4FileReader.readAsVector(filename)
          (Some(header), samples)
        case false =>
          (None, IndexedSeq())
      }
  }

  def ptAnalysis(nw: NwAnalysis, header: Header, samples: IndexedSeq[Sample]): PtAnalysis = {
    val startTime = nw.packets.head.timestamp
    val untilTime = nw.packets.last.timestamp

    val samplesInRange = samples.zipWithIndex.flatMap {
      case (s, i) =>
        val sampleTs = header.sampleTimestamp(i).toInstant.getMillis
        if (sampleTs >= startTime && sampleTs <= untilTime)
          Some(SampleWithTimestamp(sampleTs, s))
        else
          None
    }
    val stats = statsForPt4(header, samplesInRange.map(_.sample))
    val timespan = if (samplesInRange.size == 0) 0 else samplesInRange.last.timestamp - samplesInRange.head.timestamp
    PtAnalysis(samplesInRange, stats.joules, timespan)
  }

  def analyzeAtTriggers(path: String): List[CombinedAnalysis] = {
    val nwLines = DmesgReader.readFromFile(path + "dmesg.out")
    val logcatLines = LogcatReader.readFromFile(path + "logcat.out")
    val triggerTimes = findTriggerTimes(logcatLines)
    val nws = nwAnalysis(triggerTimes, nwLines)
    val (header, samples) = readPt4(path + "power.pt4")
    val pts = nws.map { nw => header.map { h => ptAnalysis(nw, h, samples) } }
    triggerTimes.zip(nws).zip(pts).map { case ((t, n), p) => CombinedAnalysis(t,n,p) }
  }

  def printCsvAnalysisForEcScriptOutputs(paths: List[String]) {
    println("id,path,packet_count,packet_bytes,packet_timespan_ms,est_wlan_mj,est_umts_mj,pt_count,pt_timespan_ms,pt_mj")
    try {
      val lines = paths.map { path =>
        analyzeAtTriggers(path + "/") map { a =>
          val header = "%s,%d".format(path, a.triggerTime)
          val nw = ",%d,%d,%d,%d,%d".format(a.nw.packets.size, a.nw.totalBytes, a.nw.timespanMs,
            (a.nw.wlanEnergyJoules * 1000.0).toInt,
            (a.nw.umtsEnergyJoules * 1000.0).toInt)
          val pt = a.pt match {
            case Some(p) =>
              ",%d,%d,%d".format(p.samples.size, p.timespanMs, (p.energyJoules * 1000.0).toInt)
            case None => ""
          }
          "%s%s%s".format(header,nw,pt)
        }
      }.flatten
      lines.zipWithIndex.foreach { case (line, i) =>
        println("%d,%s".format(i, line))
      }
    } catch {
      case e =>
        e.printStackTrace
        throw e
    }
  }

  def printAnalysisForEcScriptOutputs(path: String = "") {
    try {
      analyzeAtTriggers(path) foreach { a =>
        println("==============================")
        println("Trigger at " + a.triggerTime)
        println("==============================")
        println("Packets: %d (over %d ms)".format(a.nw.packets.size, a.nw.timespanMs))
        println("Est. energy: %.2f J".format(a.nw.wlanEnergyJoules))
        a.pt match {
          case Some(p) =>
            println("Samples: %d (over %d ms)".format(p.samples.size, p.timespanMs))
            println("Mes. energy: %.2f J".format(p.energyJoules))
          case None =>
            println("No pt4 data available")
        }
      }
    } catch {
      case e => e.printStackTrace
    }
  }

  private def fileExists(path: String) = new File(path).exists()
}
