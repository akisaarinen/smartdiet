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

package fi.akisaarinen.smartdiet.constraints

import japa.parser.JavaParser
import japa.parser.ast.CompilationUnit
import japa.parser.ast.visitor.VoidVisitorAdapter
import scala.collection.JavaConverters._
import java.io.{File, FileInputStream}
import scala.math.max
import japa.parser.ast.body._

object SourceAnalyzer {
  case class SourceClass(name: String, loc: Int)
  case class SourceTreeAnalysis(allClasses: List[SourceClass], trivialClasses: List[SourceClass])

  def analyzeSourceTree(sourceTreePath: String) = {
    val path = new File(sourceTreePath)
    val sourceFiles = FileFinder.getJavaFilePaths(FileFinder.recursiveListFiles(path))

    val cus = sourceFiles.map { filename =>
      val stream = new FileInputStream(filename)
      JavaParser.parse(stream)
    }

    val clazzesAndMethodsInCus = cus.map { cu =>
      val clazzesOrInterfaces = getWithVisitor(new ClassOrInterfaceVisitor, cu)
      val enums = getWithVisitor(new EnumVisitor, cu)
      val allClazzes: List[TypeDeclaration] = clazzesOrInterfaces ++ enums
      val methods = getWithVisitor(new MethodVisitor, cu)
      (allClazzes, methods)
    }


    val clazzToMethodMappings = cus.zip(clazzesAndMethodsInCus).map { case (cu, (clazzes, methods)) =>

      val cuPackage = getPackageName(cu)

      // Find the clazz for each method
      val clazzToMethodsMapping = methods.map { m =>
        val clazzesWhichEncapsulateMethod = clazzes.filter { c =>
          val clazzBeginFits = (c.getBeginLine < m.getBeginLine) ||
            (c.getBeginLine == m.getBeginLine && c.getBeginColumn < m.getBeginColumn)
          val clazzEndFits = (c.getEndLine > m.getEndLine) ||
            (c.getEndLine == m.getEndLine && c.getEndColumn > m.getEndColumn)
          clazzBeginFits && clazzEndFits
        }
        val closestToMethodBeginning = clazzesWhichEncapsulateMethod.sortBy { c =>
          m.getBeginLine - c.getBeginLine
        }.head
        val fullnameOfMatchingClazz = "%s.%s".format(cuPackage, getFullClazzName(closestToMethodBeginning, clazzes))
        (fullnameOfMatchingClazz, m)
      }
      clazzToMethodsMapping
    }.flatten

    val clazzStats = cus.zip(clazzesAndMethodsInCus).map { case (cu, (clazzes, methods)) =>
      val cuPackage = getPackageName(cu)
      clazzes.map { clazz =>
        val fullname = "%s.%s".format(cuPackage, getFullClazzName(clazz, clazzes))
        val innerClazzes = getInnerClazzes(clazz, clazzes)
        val totalLoc = getLoc(clazz)
        val innerLoc = innerClazzes.map(getLoc(_)).sum
        val clazzLoc = totalLoc - innerLoc
        (cu, fullname, clazzLoc, clazz)
      }
    }.flatten
    val allClasses = clazzStats.map { case (cu, fullname, loc, clazz) =>
      SourceClass(fullname, loc)
    }
    val trivialClasses = clazzStats.flatMap { case (cu, fullname, loc, clazz) =>
      val methods = clazzToMethodMappings
        .filter { case (clazzName, _) => clazzName == fullname }
        .map { case (_, method) => method }
      val realMethods = methods.filterNot(isGetterOrSimilar).filterNot(isSetter).filterNot(isToString)
      if (realMethods.size == 0)
        Some(SourceClass(fullname, loc))
      else
        None
    }
    SourceTreeAnalysis(allClasses, trivialClasses)
  }

  private def getLoc(c: TypeDeclaration) = {
    max(1, c.getEndLine - c.getBeginLine)
  }

  private def getFullClazzName(c: TypeDeclaration, all: List[TypeDeclaration]): String = {
    val allEncapsulating = getEncapsulatingClazzes(c, all)
    if (allEncapsulating.size == 0) {
      c.getName
    } else {
      val closest = allEncapsulating.sortBy(_.getBeginLine).last
      "%s$%s".format(getFullClazzName(closest, all),c.getName)
    }
  }

  private def getEncapsulatingClazzes(c: TypeDeclaration, all: List[TypeDeclaration]): List[TypeDeclaration] = {
    val (beginLine, endLine) = (c.getBeginLine, c.getEndLine)
    val (beginCol, endCol) = (c.getBeginColumn, c.getEndColumn)
    val allEncapsulating = all.filterNot(_ == c).filter {
      a =>
        val beginIsBefore = (a.getBeginLine < beginLine) ||
          (a.getBeginLine == beginLine && a.getBeginColumn < beginCol)
        val endIsAfter = (a.getEndLine > endLine) ||
          (a.getEndLine == endLine && a.getEndColumn > endCol)
        beginIsBefore && endIsAfter
    }
    allEncapsulating
  }

  private def getInnerClazzes(c: TypeDeclaration, all: List[TypeDeclaration]): List[TypeDeclaration] = {
    val (beginLine, endLine) = (c.getBeginLine, c.getEndLine)
    val (beginCol, endCol) = (c.getBeginColumn, c.getEndColumn)
    val allWithinClass = all.filterNot(_ == c).filter {
      a =>
        val beginIsAfter = (a.getBeginLine > beginLine) ||
          (a.getBeginLine == beginLine && a.getBeginColumn > beginCol)
        val endIsBefore = (a.getEndLine < endLine) ||
          (a.getEndLine == endLine && a.getEndColumn < endCol)
        beginIsAfter && endIsBefore
    }
    allWithinClass
  }

  private def getPackageName(cu: CompilationUnit): String = {
    val PackageExpr = "(?s)package (.*);.*".r
    cu.getPackage.toString match {
      case PackageExpr(name) => name
      case x => sys.error("Error parsing package declaration: '%s'".format(x.toString))
    }
  }

  private def isGetterOrSimilar(m: MethodDeclaration): Boolean = {
    val GetterName = "(get|is|has)(.*)".r
    val GetterBody = "return (.*);".r
    val body = m.getBody
    if (body == null) {
      false
    } else {
      val stmts = body.getStmts() match {
        case null => List()
        case s => s.asScala.toList
      }
      val nameMatches = GetterName.findFirstIn(m.getName).isDefined
      val bodyMatches =  (stmts.size == 1) &&
        GetterBody.findFirstIn(stmts.head.toString).isDefined
      nameMatches && bodyMatches
    }
  }

  private def isSetter(m: MethodDeclaration): Boolean = {
    val SetterName = "set(.*)".r
    val SetterBody = "(.*)=(.*);".r
    val body = m.getBody
    if (body == null) {
      false
    } else {
      val stmts = body.getStmts() match {
        case null => List()
        case s => s.asScala.toList
      }
      val nameMatches = SetterName.findFirstIn(m.getName).isDefined
      val bodyMatches =  (stmts.size == 1) &&
        SetterBody.findFirstIn(stmts.head.toString).isDefined
      nameMatches && bodyMatches
    }
  }

  private def isToString(m: MethodDeclaration): Boolean = {
    val nameMatches = (m.getName == "toString")
    val typeMatches = (m.getType.toString == "String")
    nameMatches && typeMatches
  }

  private def getWithVisitor[T](visitor: Visitor[T], cu: CompilationUnit): List[T] = {
    visitor.visit(cu, ())
    visitor.items
  }

  private def getWithVisitor[T](visitor: Visitor[T], c: ClassOrInterfaceDeclaration): List[T] = {
    visitor.visit(c, ())
    visitor.items
  }

  private def getWithVisitor[T](visitor: Visitor[T], e: EnumDeclaration): List[T] = {
    visitor.visit(e, ())
    visitor.items
  }

  private class Visitor[T] extends VoidVisitorAdapter[Unit] {
    var items: List[T] = List()
  }

  private class MethodVisitor extends Visitor[MethodDeclaration] {
    override def visit(n: MethodDeclaration, arg: Unit) {
      items = items :+ n
      super.visit(n, arg)
    }
  }

  private class FieldVisitor extends Visitor[FieldDeclaration] {
    override def visit(n: FieldDeclaration, arg: Unit) {
      items = items :+ n
      super.visit(n, arg)
    }
  }

  private class ClassOrInterfaceVisitor extends Visitor[ClassOrInterfaceDeclaration] {
    override def visit(n: ClassOrInterfaceDeclaration, arg: Unit) {
      items = items :+ n
      super.visit(n, arg)
    }
  }

  private class EnumVisitor extends Visitor[EnumDeclaration] {
    override def visit(n: EnumDeclaration, arg: Unit) {
      items = items :+ n
      super.visit(n, arg)
    }
  }
}