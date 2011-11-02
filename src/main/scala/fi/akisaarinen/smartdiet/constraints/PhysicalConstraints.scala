package fi.akisaarinen.smartdiet.constraints

import org.apache.bcel.classfile.{JavaClass, Method}
import org.apache.bcel.util.Repository

object PhysicalConstraints {
  val InvokeInstructionExpr = "^[0-9]+:[\t ]+(invoke[^ \t]+)[ \t]+([^ \t;]+) \\(([^)]*)\\)([^ ]*).*".r

  type ClassAndMethod = (JavaClass, Method)
  type MethodInvokationMap = Map[ClassAndMethod, Seq[ClassAndMethod]]

  case class PhysicalConstraintAnalysis(appMethods: Seq[String],
                                        constrainedMethods: Seq[String],
                                        appMethodsFittingCriteria: Seq[String],
                                        appMethodsNotFittingCriteria: Seq[String])

  def findAllMethodsInvokingCriteria(allAppInvokations: MethodInvokationMap,
                                     interestingAppInvokations: List[ClassAndMethod],
                                     criteria: (ClassAndMethod) => Boolean) = {
    val allInvokedMethods = allAppInvokations.map { case ((c, m), i) => i }.flatten.toSeq.distinct
    val constrainedMethods = allInvokedMethods.filter(criteria)
    val allFittingMethodNames = constrainedMethods
      .map { lookupMethod =>
        findMethodsCallingGivenMethod(allAppInvokations, lookupMethod)
      }
      .flatten.map(asString).sorted
    val interestingAppMethodNames = interestingAppInvokations.map(asString).sorted
    PhysicalConstraintAnalysis(interestingAppMethodNames,
      constrainedMethods.map(asString).sorted,
      interestingAppMethodNames.filter(allFittingMethodNames.contains(_)),
      interestingAppMethodNames.filterNot(allFittingMethodNames.contains(_)))
  }

  def asString(cam: ClassAndMethod): String = {
    "%s.%s(%s): %s".format(cam._1.getClassName, cam._2.getName, cam._2.getSignature, cam._2.getReturnType.getSignature)
  }

  private def findMethodsCallingGivenMethod(allInvokations: MethodInvokationMap, lookupMethod: ClassAndMethod): Seq[ClassAndMethod] = {
    recursiveFind(List(), allInvokations.toList, lookupMethod)
  }

  def findAllInvokationsInApp(repo: Repository, appClazzes: Seq[JavaClass]): MethodInvokationMap = {
    appClazzes.map { clazz =>
      clazz.getMethods.toList.map { method =>
        try {
          val invoked = PhysicalConstraints.getAllInvokedMethods(repo, method)
          (clazz, method) -> invoked
        } catch {
          case e => throw new RuntimeException("Error while trying to get all invoked methods for method: '%s' from class '%s'".format(method.toString, clazz.getClassName), e)
        }
      }
    }.flatten.toMap
  }

  private def recursiveFind(stack: List[ClassAndMethod], allInvokations: Seq[(ClassAndMethod, Seq[ClassAndMethod])], lookupMethod: ClassAndMethod): Seq[ClassAndMethod] = {
    //println("  + Looking for %s.%s".format(lookupMethod._1.getClassName, lookupMethod._2.getName))
    //println("Stack: " + stack.map { s => s._1.getClassName + "." + s._2.getName }.mkString(","))
    val newDirectInvokations = allInvokations.filter { case ((c, m), i) =>
      !stack.contains((c, m)) && i.contains(lookupMethod)
    }.map { case ((c, m), i) => (c, m) }
    if (newDirectInvokations.size == 0) {
      List()
    } else {
      //println("  |-- %d new methods: %s".format(newDirectInvokations.size, newDirectInvokations.map(i => i._1.getClassName + "." + i._2.getName).mkString(" ")))
      val newStack = stack ++ newDirectInvokations
      newDirectInvokations ++ newDirectInvokations.map { newInvokation =>
        recursiveFind(newStack, allInvokations, newInvokation)
      }.flatten
    }
  }

  def getAllInvokedMethods(repository: Repository, m: Method): Seq[ClassAndMethod] = {
    if (m.isAbstract || m.isNative) {
      return List()
    }
    val invokeInstructionLines = m.getCode.toString
      .split("\n").toList
      .flatMap { instruction =>
        instruction match {
          case InvokeInstructionExpr(t, target, args, returnType) => Some((instruction, t, target, args, returnType))
          case _ => None
        }
      }

    val ClassWithMethod = "^(.*)\\.([^.]+)$".r
    invokeInstructionLines.map { case (instruction, invokeType, target, args, returnType) =>
      try {
        target match {
          case ClassWithMethod(clazzName, methodName) =>
            val clazz = repository.loadClass(clazzName)
            // TODO: use parameter types to find only the correct method
            val (nameMatchingMethods, exactMatchingMethods) = findMethodsFromClass(clazz, methodName, args, returnType)
            if (exactMatchingMethods.size == 0) {
              println("=== Superclasses===")
              clazz.getSuperClasses().foreach { c => println(c.toString) }
              println("=== Erroneous clazz ===")
              println(clazz.toString())
              sys.error("Unable to find exact invoked method (found %d) for %s.%s (%s) %s, name matches: (%s)".format(
                exactMatchingMethods.size,
                clazz.getClassName,
                methodName,
                args,
                returnType,
                nameMatchingMethods.map(_._2.getName).mkString(",")))
            } else if (exactMatchingMethods.size > 1) {
              //println("Overloaded methods (%d) for %s.%s".format(methodsWithClass.size, clazz.getClassName, methodName))
             // methodsWithClass.foreach { case (t, m) => println("  - %s".format(t.getClassName)) }
            }
            // don't map to the class where its actually defined, but instead directly to the class itself, it's more interesting
            exactMatchingMethods.map { case(actualClazzWhereMethodIsDefined, m) =>
              (clazz, m)
            }.take(1)
          case _ => sys.error("Unknown class name.method format '%s'".format(target))
        }
      } catch {
        case e => throw new RuntimeException("Error processing instruction '%s' (invoketype '%s', target '%s', args '%s', returnType '%s')".format(
              instruction,
              invokeType,
              target,
              args,
              returnType),
            e)
      }
    }.flatten.toSeq.distinct
  }

  def findMethodsFromClass(c: JavaClass, methodName: String, args: String, returnType: String): (List[ClassAndMethod], List[ClassAndMethod]) = {
    //println("Looking match for %s.%s (%s) %s".format(c.getClassName, methodName, args, returnType))

    // Constructor cant go to parents
    if (methodName == "<init>") {
      val exactMethods = c.getMethods.toList.filter(isExactMatch(methodName, args, returnType))
      val nameMatchingMethods = c.getMethods.toList.filter(_.getName == methodName)
      (nameMatchingMethods.map { m => (c, m) },
        exactMethods.map { m => (c, m) })
    // Other calls can
    } else {
      val allTypes = getAllTypes(c)
      val methodsFromClassAndSuperClasses = allTypes.map { t =>
        val methodsInType = t.getMethods.toList
        methodsInType.map { m => (t, m) }
      }.flatten
      val exactMethods = methodsFromClassAndSuperClasses.filter { case (t, m) =>
        isExactMatch(methodName, args, returnType)(m)
      }
      val nameMatchingMethods = methodsFromClassAndSuperClasses.filter { case (t, m) =>
        m.getName == methodName
      }
      (nameMatchingMethods, exactMethods)
    }
  }

  def getAllTypes(c: JavaClass): List[JavaClass] = {
    val classes = List(c) ++ c.getSuperClasses.toList
    val interfaces = classes.map(_.getInterfaces.toList).flatten
    (classes ++ interfaces ++ interfaces.map(getAllTypes(_)).flatten).distinct
  }

  def isExactMatch(methodName: String, args: String, returnType: String)(m: Method): Boolean = {
    m.getName == methodName &&
    m.getArgumentTypes.map(_.getSignature).mkString("") == args &&
    m.getReturnType.getSignature == returnType
  }
}