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

package fi.akisaarinen.smartdiet

import constraints.ConstraintAnalyzer
import energyusage.EnergyUsageAnalyzer
import scopt.OptionParser
import collection.mutable.{ArrayBuffer, Buffer}

object SmartDiet {
  sealed abstract class Mode
  case object HelpMode extends Mode
  case object VersionMode extends Mode
  case class JavaSingleMode(appName: String) extends Mode
  case object JavaAllMode extends Mode
  case object DmesgMode extends Mode
  case object NwMode extends Mode
  case object Pt4Mode extends Mode
  case object PpMode extends Mode
  case object PpCsvMode extends Mode

  case class Config(var paths: Buffer[String] = new ArrayBuffer[String],
                    var mode: Mode = HelpMode,
                    var suppressOutput: Boolean = false,
                    var javaConfigFile: String = "sources.json",
                    var javaOutputMode: ConstraintAnalyzer.OutputMode = ConstraintAnalyzer.ReadableOutput,
                    var javaParMode: ConstraintAnalyzer.ParMode = ConstraintAnalyzer.SingleThreaded)

  def main(args: Array[String]) {
    val start = System.currentTimeMillis
    val config = Config()
    val parser = new OptionParser("smartdiet") {
      opt("csv", "Use CSV output for java analysis", { config.javaOutputMode = ConstraintAnalyzer.CsvOutput })
      opt("par", "Use parallelized mode (might speed things up, but errors are confusing)", { config.javaParMode = ConstraintAnalyzer.MultiThreaded })
      opt("j", "java", "Analyze java classes for a single application", "<appName>", { appName: String => config.mode = JavaSingleMode(appName) })
      opt("java-all", "Analyze java classes for all configured applications", { config.mode = JavaAllMode })
      opt(None, "java-config", "Use different java source configuration file", "<filename.json>", { f: String => config.javaConfigFile = f })
      opt("s", "suppress", "Suppress extra output (useful when outputting CSV)", { config.suppressOutput = true })
      opt("v", "version", "Print program version and exit", { config.mode = VersionMode })
      opt("dmesg", "<paths> are .dmesg files", { config.mode = DmesgMode })
      opt("nw", "<paths> are .nw files", { config.mode = NwMode })
      opt("pt4", "<paths> are .pt4 files", { config.mode = Pt4Mode })
      opt("pp", "<paths> are pp script output paths", { config.mode = PpMode })
      opt("ppcsv", "<paths> are pp script output paths and export aggregated CSV output", { config.mode = PpCsvMode })
      arglistOpt("<path> <path> ...", "Paths to files/paths under analysis", { path: String =>
        config.paths += path
      })
    }
    if (parser.parse(args)) {
      try {
        config.mode match {
          case HelpMode => parser.showUsage
          case VersionMode =>
            val version = getClass.getPackage.getImplementationVersion
            println("SmartDiet v" + version)
          case JavaSingleMode(appName) =>
            ConstraintAnalyzer.doAnalysisForApp(appName, config.javaOutputMode, config.javaParMode, config.javaConfigFile)
          case JavaAllMode =>
            ConstraintAnalyzer.doAnalysisForAll(config.javaOutputMode, config.javaParMode, config.javaConfigFile)
          case DmesgMode =>
            EnergyUsageAnalyzer.printEstimatesForDmesgFiles(config.paths.toList)
          case NwMode =>
            EnergyUsageAnalyzer.printEstimatesForNwFiles(config.paths.toList)
          case Pt4Mode =>
            EnergyUsageAnalyzer.printSummaryForPt4Files(config.paths.toList)
          case PpMode =>
            config.paths.foreach { path =>
              EnergyUsageAnalyzer.printAnalysisForEcScriptOutputs(path + "/")
            }
          case PpCsvMode =>
            EnergyUsageAnalyzer.printCsvAnalysisForEcScriptOutputs(config.paths.toList)
        }
      } catch {
        case e =>
          println("Error in analysis: " + e)
          e.printStackTrace()
      }
      if (!config.suppressOutput) {
        println("Total time used for analysis: %d ms".format(System.currentTimeMillis - start))
      }
    }
  }
}