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

import org.apache.bcel.util.Repository
import fi.akisaarinen.smartdiet.constraints.ConstraintAnalyzer.{SerializableType, Relaxed}
import org.apache.bcel.classfile.{Method, Field, JavaClass}

object Constraints {
  def containsNonStaticMethods(c: JavaClass) = {
    val nonStaticNonConstructorMethods = c.getMethods.toList.
      filterNot(_.isStatic).
      filterNot(_.getName == "<init>")
    nonStaticNonConstructorMethods.size > 0
  }

  def getFieldClassName(f: Field): Option[String] = {
    getObjectClassNameFromSignature(f.getSignature)
  }

  def getObjectClassNameFromSignature(signature: String): Option[String] = {
    // Object or array of objects => return the object (drop array info)
    val ObjectType = "[\\[]*L(.*);".r
    // Primitive or array of primitives => return None
    val PrimitiveType = "[\\[]*([A-Z])".r
    signature match {
      case ObjectType(o) => Some(o.replace('/', '.'))
      case PrimitiveType(t) => None
      case _ => sys.error("Unknown signature '%s'".format(signature))
    }
  }

  def isProbablyAndroidResourceClass(clazz: JavaClass): Boolean = {
    val ResourceClassName = "(.*\\.R)(\\$[a-z]+$|$)".r
    val nameMatch = clazz.getClassName match {
      case ResourceClassName(outer,inner) => true
      case _ => false
    }
    nameMatch && clazz.isFinal && clazz.isPublic
  }

  def argsAndReturnTypeAreSerializable(repository: Repository, programClazzes: List[JavaClass], sType: SerializableType)(method: Method): Boolean = {
    val types = method.getArgumentTypes.toList :+ method.getReturnType
    val signatures = types.map(_.getSignature)
    signatures.forall(isSerializableSignature(repository, programClazzes, sType))
  }

  def implementsSerializable(javaClazz: JavaClass): Boolean = {
    val interfaces = javaClazz.getAllInterfaces.toList
    val isSerializable = interfaces.find(_.getClassName == "java.io.Serializable").isDefined
    isSerializable
  }

  def isThrowable(javaClazz: JavaClass): Boolean = {
    val superClasses = javaClazz.getSuperClasses.toList
    val isThrowable = superClasses.find(_.getClassName == "java.lang.Throwable").isDefined
    isThrowable
  }

  def hasZeroArgConstructor(c: JavaClass): Boolean = {
     c.getMethods.toList.find { m =>
        m.getName == "<init>" && m.getArgumentTypes.size == 0
     }.isDefined
    // todo: constructor heuristics could also be based on whether the arguments
    //       are serializable (might be easily converted to setters)
  }


  def containsMethodWithSerializableArguments(repository: Repository, programClazzes: List[JavaClass], sType: SerializableType)(javaClazz: JavaClass): Boolean = {
    val methods = javaClazz.getMethods.toList
    val serializableMethod = methods.filterNot(_.getName == "<init>").filterNot(_.getName == "<clinit>").find { m =>
      val args = m.getArgumentTypes.toList
      val argsSerializable = args.forall { a =>
        isSerializableSignature(repository, programClazzes, sType)(a.getSignature)
      }
      val returnTypeSerializable = isSerializableSignature(repository, programClazzes, sType)(m.getReturnType.getSignature)
      argsSerializable && returnTypeSerializable
    }
    //println("%s: %s".format(javaClazz.getClassName, serializableMethod.toString))
    serializableMethod.isDefined
  }

  def isSerializableSignature(repository: Repository, programClazzes: List[JavaClass], sType: SerializableType)(signature: String): Boolean = {
    val clazzName = getObjectClassNameFromSignature(signature)
    clazzName match {
      case Some(name) =>
        val clazz = repository.loadClass(name)
        // possibly looser for program classes
        if (sType == Relaxed && programClazzes.contains(clazz))
          allFieldsSerializable(repository, programClazzes)(clazz)
        // always strict for libs/SDK
        else
          implementsSerializable(clazz)
      case None =>
        true
    }
  }

  def allFieldsSerializable(repository: Repository, programClazzes: List[JavaClass], stack: List[JavaClass] = List())(javaClazz: JavaClass): Boolean = {
    try {
      val clazzAndSuperClazzes = javaClazz :: javaClazz.getSuperClasses.toList
      val fields = clazzAndSuperClazzes.map(_.getFields.toList).flatten
      val stackWithCurrent = stack :+ javaClazz
      val serializableFields = fields.filter { f =>
        val clazzName = getFieldClassName(f)
        clazzName match {
          // object type => need to check if iether transient(OK) or serializable(OK)
          case Some(name) =>
            val clazz = repository.loadClass(name)
            val isCyclicDependency = stackWithCurrent.contains(clazz)
            // Assume that cyclic doesn't reduce serializability. Not sure if
            // this is a correct assumption, but it's the least conservative one
            // which makes sense in this context (i.e. we don't drop candidates out
            // just because there is a cyclic dependency)
            if (isCyclicDependency) {
              true
            } else {
              // Only allow field-serialibility to be the same as 'implements Serializable'
              // within the program source code itself, because that can be fixed just by
              // making the class Serializable (if conditions are met), whereas libraries
              // can not be changed that easily.
              val isProgramClazz = programClazzes.contains(clazz)
              f.isTransient ||
                clazz.isTransient ||
                implementsSerializable(clazz) ||
                (isProgramClazz && allFieldsSerializable(repository, programClazzes, stackWithCurrent)(clazz))
            }
          // primitive type => always serializable
          case None =>
            true
        }
      }
      serializableFields.size == fields.size
    // If stack overflows, there's a loop and the thing is not serializable
    } catch {
      case e: StackOverflowError =>
        println("Stack overflow while analyzing if all fields serializable for '%s'".format(javaClazz.getClassName))
        false
    }
  }

  def isAndroidActivity(c: JavaClass): Boolean = {
    c.getSuperClasses.toList.find(_.getClassName == "android.app.Activity").isDefined
  }

  def isGeneratedActivitySubclass(repository: Repository)(c: JavaClass): Boolean = {
    val ActivitySubclass = "^([^$]+)\\$([0-9]+)$".r
    val matchingParentClazz = c.getClassName match {
      case ActivitySubclass(parentClassName, subclass) => Some(repository.loadClass(parentClassName))
      case _ => None
    }
    matchingParentClazz match {
      case Some(parentClazz) => isAndroidActivity(parentClazz)
      case None => false
    }
  }

  def isAndroidService(c: JavaClass): Boolean = {
    c.getSuperClasses.toList.find(_.getClassName == "android.app.Service").isDefined
  }

   def isGeneratedServiceSubclass(repository: Repository)(c: JavaClass): Boolean = {
    val ServiceSubclass = "^([^$]+)\\$([0-9]+)$".r
    val matchingParentClazz = c.getClassName match {
      case ServiceSubclass(parentClassName, subclass) => Some(repository.loadClass(parentClassName))
      case _ => None
    }
    matchingParentClazz match {
      case Some(parentClazz) => isAndroidService(parentClazz)
      case None => false
    }
  }

  def isAidlInterface(repository: Repository)(c: JavaClass): Boolean = {
    c.getInterfaces().map(_.getClassName).contains("android.os.IInterface")
  }

  def isInterfaceStubOrProxy(repository: Repository)(c: JavaClass): Boolean = {
    val InterfaceStubOrProxy = "^([^$]+)\\$Stub($|\\$Proxy$)".r
    val matchingParentClazz = c.getClassName match {
      case InterfaceStubOrProxy(parentClassName, proxy) =>
        Some(repository.loadClass(parentClassName))
      case _ => None
    }
    matchingParentClazz match {
      case Some(parentClazz) => parentClazz.isInterface
      case None => false
    }
  }

  def isDerivedFromPackage(repository: Repository, pName: String)(c: JavaClass): Boolean = {
    val packageNames = (List(c) ++ c.getSuperClasses().toList).map(_.getPackageName)
    packageNames.find(p => p.startsWith(pName)).isDefined
  }
}