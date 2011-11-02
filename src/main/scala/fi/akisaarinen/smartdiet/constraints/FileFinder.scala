package fi.akisaarinen.smartdiet.constraints

import java.io.File

object FileFinder {
  def getJavaFilePaths(files: List[File]): List[String] = {
    val JavaRegex = "(.*)\\.java".r
    files.flatMap { f =>
      f.getName match {
        case JavaRegex(clazzName) => Some(f.getAbsolutePath())
        case _ => None
      }
    }
  }

  def getClassFilePaths(files: List[File]): List[String] = {
    val ClazzRegex = "(.*)\\.class$".r
    files.flatMap { f =>
      f.getName match {
        case ClazzRegex(clazzName) => Some(f.getAbsolutePath())
        case _ => None
      }
    }
  }

  def recursiveListFiles(f: File): List[File] = {
    val these = f.listFiles
    if (these != null) {
      val theseList = these.toList
      theseList ++ theseList.filter(_.isDirectory).flatMap(recursiveListFiles)
    } else {
      List()
    }
  }
}