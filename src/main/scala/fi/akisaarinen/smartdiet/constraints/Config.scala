package fi.akisaarinen.smartdiet.constraints

import io.Source
import net.liftweb.json.{DefaultFormats, parse}

object Config {
  case class SourceConfig(sdkClassFilePath: String, apps: List[Application])
  case class Application(name: String,
                         appPath: String,
                         libPath: Option[String] = None,
                         appSrcPath: Option[String] = None)

  def loadConfig(filename: String): SourceConfig = {
    val data = Source.fromFile(filename).getLines().mkString("\n")
    val json = parse(data)
    implicit val formats = DefaultFormats
    json.extract[SourceConfig]
  }
}