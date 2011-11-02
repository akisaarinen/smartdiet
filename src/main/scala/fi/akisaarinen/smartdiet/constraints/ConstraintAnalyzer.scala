package fi.akisaarinen.smartdiet.constraints

import java.io.File
import org.apache.bcel.util.{SyntheticRepository, Repository}
import fi.akisaarinen.smartdiet.constraints.SourceAnalyzer.{SourceClass, SourceTreeAnalysis}
import org.apache.bcel.classfile.{Method, Field, JavaClass, ClassParser}
import PhysicalConstraints.asString

object ConstraintAnalyzer {
  class ParMode
  case object SingleThreaded extends ParMode
  case object MultiThreaded extends ParMode

  class OutputMode
  case object ReadableOutput extends OutputMode
  case object CsvOutput extends OutputMode

  abstract class SerializableType
  case object Strict extends SerializableType
  case object Relaxed extends SerializableType

  def doAnalysisForAll(outputMode: OutputMode, parMode: ParMode, configFile: String) = doAnalysisFor({ _ => true }, outputMode, parMode, configFile)
  def doAnalysisForApp(applicationName: String, outputMode: OutputMode, parMode: ParMode, configFile: String) = doAnalysisFor(_.name == applicationName, outputMode, parMode, configFile)

  private def doAnalysisFor(criteria: Config.Application => Boolean, outputMode: OutputMode, parMode: ParMode, configFile: String): Unit = {
    val config = Config.loadConfig(configFile)
    val matchingApps = config.apps.filter(criteria)
    if (matchingApps.size == 0) {
      sys.error("No application with given criteria is configured. Available: '%s'"
        .format(config.apps.map(_.name).mkString(",")))
    } else {
      if (outputMode == CsvOutput) printCsvHeaders()
      matchingApps.foreach { app =>
        try {
          analyze(config.sdkClassFilePath, app, outputMode, parMode)
        } catch {
          case e =>
            e.printStackTrace()
            println("Error in analysis of '%s'".format(app.name))
        }
      }
    }
  }

  private def printCsvHeaders() {
    println(
      "application,"+
        "all_method_count,"+
        "interesting_method_count,"+
        "not_strict_serializable_method_count,"+
        "not_relaxed_serializable_method_count," +
        "hard_constrained_method_count," +
        "state_constrained_method_count"
    )
  }

  private def analyze(sdkClazzFilePath: String, app: Config.Application, outputMode: OutputMode, parMode: ParMode) {
    val sourceAnalysis = app.appSrcPath.map { srcPath =>
      SourceAnalyzer.analyzeSourceTree(srcPath)
    }.getOrElse(SourceTreeAnalysis(List(), List()))

    val repo = loadAndroidRepository(sdkClazzFilePath)
    val programClazzesRaw = loadClassesToRepository(app.appPath, repo)
      .filterNot(isThinkAirLibraryClass)
    app.libPath.foreach { lcp => loadClassesToRepository(lcp, repo) }

    val programClazzes = parMode match {
      case SingleThreaded => programClazzesRaw
      case MultiThreaded => programClazzesRaw.par
    }

    // Filter out  generated crap
    val nonGeneratedClazzes = programClazzes
      .filterNot(Constraints.isAidlInterface(repo))
      .filterNot(Constraints.isInterfaceStubOrProxy(repo))
      .filterNot(Constraints.isProbablyAndroidResourceClass)

    // Filter out those containing no logic at all (of no interest)
    val clazzesWithSomeLogic = nonGeneratedClazzes
      .filterNot { c => sourceAnalysis.trivialClasses.map(_.name).contains(c.getClassName) }
      .filterNot(_.isInterface)
      .filterNot(Constraints.isThrowable)
    val interestingClazzList = clazzesWithSomeLogic.toList

    // Find interesting methods
    val allAppMethods = PhysicalConstraints.findAllInvokationsInApp(repo, programClazzes.toList)
    val interestingAppMethods = allAppMethods.toList
      .filter { case ((c,m), i) => interestingClazzList.contains(c) }
      .map { case ((c,m), i) => (c,m) }

    // Find strictly serializable methods
    val strictlySerializableMethods = interestingAppMethods
      .filter { case (c,m) => Constraints.implementsSerializable(c) }
      .filter { case (c,m) => Constraints.argsAndReturnTypeAreSerializable(repo, programClazzes.toList, Strict)(m) }

    val strictAndConstructorSerializableMethods = strictlySerializableMethods
      .filter { case (c,m) => Constraints.hasZeroArgConstructor(c) }

    // Find relaxedly serializable methods
    val heuristicallySerializableMethods = interestingAppMethods
      .filter { case (c,m) => Constraints.allFieldsSerializable(repo, programClazzes.toList, List())(c) }
      .filter { case (c,m) => Constraints.argsAndReturnTypeAreSerializable(repo, programClazzes.toList, Relaxed)(m) }
    val relaxedSerializableMethods =
      (strictlySerializableMethods ++ heuristicallySerializableMethods).distinct

    // this is not enabled, checking network status might be ok in remote end also...
    //"^android\\.net\\.ConnectivityManager",

    val hardConstraintConfig = List(
      "^android\\.app\\.AccessibilityService",
      "^android\\.app\\.NotificationManager",
      "^android\\.app\\.UIModeManager",
      "^android\\.app\\.admin\\.DevicePolicyManager",
      "^android\\.appwidget\\..*",
      "^android\\.bluetooth\\..*",
      "^android\\.hardware\\.SensorManager",
      "^android\\.hardware\\.usb\\.UsbManager",
      "^android\\.location\\..*",
      "^android\\.media\\..*",
      "^android\\.net\\.wifi\\.WifiManager",
      "^android\\.nfc\\.Nfc.*",
      "^android\\.speech\\..*",
      "^android\\.telephony\\.TelephonyManager",
      "^android\\.text\\.ClipboardManager",
      "^android\\.os\\.PowerManager",
      "^android\\.os\\.Vibrator",
      "^android\\.view\\..*",
      "^android\\.view\\.WindowManager",
      "^android\\.widget\\..*"
    )

    val hardConstraintCollection = parMode match {
      case SingleThreaded => hardConstraintConfig
      case MultiThreaded => hardConstraintConfig.par
    }
    val hardConstraints = hardConstraintCollection.map { case (clazzName) =>
      val hc = PhysicalConstraints.findAllMethodsInvokingCriteria(allAppMethods, interestingAppMethods, classNameMatches(clazzName))
      (clazzName, hc)
    }

    val stateConstraintConfig = List(
      "^java\\.io\\.File",
      "^android\\.content\\.SharedPreferences",
      "^android\\.database\\.sqlite\\..*"
    )

    val stateConstraintCollection = parMode match {
      case SingleThreaded => stateConstraintConfig
      case MultiThreaded => stateConstraintConfig.par
    }
    val stateConstraints = stateConstraintCollection.map { case (clazzName) =>
      val hc = PhysicalConstraints.findAllMethodsInvokingCriteria(allAppMethods, interestingAppMethods, classNameMatches(clazzName))
      (clazzName, hc)
    }

    val hcAllAppMethodsFittingCriteria = hardConstraints.map(_._2.appMethodsFittingCriteria).flatten.toList.distinct
    val scAllAppMethodsFittingCriteria = stateConstraints.map(_._2.appMethodsFittingCriteria).flatten.toList.distinct

    outputMode match {
      case ReadableOutput =>
        println("======= Method details =======")
        println("---( strict serializable )---")
        strictlySerializableMethods.foreach { m => println("* " + asString(m)) }
        println("---( relaxed serializable )---")
        relaxedSerializableMethods.foreach { m => println("* " + asString(m)) }
        println("---( classes considered trivial )---")
        sourceAnalysis.trivialClasses.foreach { c => println("* " + c.name ) }

        hardConstraints.foreach { case (name, hc) =>
          println("---( Hard constraint '%s' listing )---".format(name))
          println("* Constrained methods that were called")
          hc.constrainedMethods.foreach { m => println("  + " + m) }
          println("* App methods calling constrained methods")
          hc.appMethodsFittingCriteria.foreach { m => println("  + " + m) }
        }

        println("======== Per-method statistics ========")

        allAppMethods.foreach { case ((c,m), i) =>
          val methodAsString = asString((c,m))
          println("* %s".format(methodAsString))
          if (strictlySerializableMethods.contains((c,m))) {
            println("  + Strictly serializable")
          }
          if (relaxedSerializableMethods.contains((c,m))) {
            println("  + Relaxedly serializable")
          }
          hardConstraints.foreach { case (name, hc) =>
            if (hc.appMethodsFittingCriteria.contains(methodAsString)) {
              println("  + Hard constraint: '%s'".format(name))
            }
          }
          stateConstraints.foreach { case (name, sc) =>
            if (sc.appMethodsFittingCriteria.contains(methodAsString)) {
              println("  + State constraint: '%s'".format(name))
            }
          }
        }

        println("======== Per-method csv crap ========")

        println("\"method name\",\"strict\",\"relaxed\",\"hard\",\"state\"")
        allAppMethods.foreach { case ((c,m), i) =>
          val methodAsString = asString((c,m))
          val isStrict = if (strictlySerializableMethods.contains((c,m))) 1 else 0
          val isRelaxed = if (relaxedSerializableMethods.contains((c,m))) 1 else 0
          val numberOfHard = hardConstraints.filter { case (name, hc) =>
            hc.appMethodsFittingCriteria.contains(methodAsString)
          }.size
          val numberOfState = stateConstraints.filter { case (name, sc) =>
            sc.appMethodsFittingCriteria.contains(methodAsString)
          }.size
          println("\"%s\",\"%d\",\"%d\",\"%d\",\"%d\"".format(
            methodAsString,
            isStrict,
            isRelaxed,
            numberOfHard,
            numberOfState))
        }

        println("======= Offloadable summary analysis ========")
        println("SDK path: %s".format(sdkClazzFilePath))
        println("App.class path: %s".format(app.appPath))
        println("App.java path: %s".format(app.appSrcPath))
        println("Lib.class path: %s".format(app.libPath))
        println("---( without filtering )---")
        println("- %d total unfiltered classes".format(programClazzes.size))
        println("- %d total classes detected from source".format(sourceAnalysis.allClasses.size))
        println("---( filters for classes )---")
        println("- %d non-generated classes".format(nonGeneratedClazzes.size))
        println("- %d trivial classes".format(sourceAnalysis.trivialClasses.size))
        println("- %d with some logic (== interesting classes)".format(clazzesWithSomeLogic.size))
        println("---( offloading/serializable numbers )---")
        println("- %d total methods".format(allAppMethods.size))
        println("- %d strictly serializable methods".format(strictlySerializableMethods.size))
        println("- %d strictly serializable methods with no-arg constructors".format(strictAndConstructorSerializableMethods.size))
        println("- %d relaxed serializable methods".format(relaxedSerializableMethods.size))

        hardConstraints.foreach { case (name, hc) =>
          println("---( Hard constraint '%s' totals )---".format(name))
          if (hc.appMethodsFittingCriteria.size > 0) {
            println("- Methods fit to criteria: %d".format(hc.appMethodsFittingCriteria.size))
            println("- Matching invokations: %d".format(hc.constrainedMethods.size))
          }
        }

        stateConstraints.foreach { case (name, sc) =>
          println("---( State constraint '%s' totals )---".format(name))
          if (sc.appMethodsFittingCriteria.size > 0) {
            println("- Methods fit to criteria: %d".format(sc.appMethodsFittingCriteria.size))
            println("- Matching invokations: %d".format(sc.constrainedMethods.size))
          }
        }

        println("---( Hard constraints combined )---")
        println("- Methods fit to some criteria: %d".format(hcAllAppMethodsFittingCriteria.size))

        println("---( State constraints combined )---")
        println("- Methods fit to some criteria: %d".format(scAllAppMethodsFittingCriteria.size))


      case CsvOutput =>
        /*
        "application,"+
        "all_method_count,"+
        "interesting_method_count,"+
        "not_strict_serializable_method_count,"+
        "not_relaxed_serializable_method_count," +
        "hard_constrained_method_count," +
        "state_constrained_method_count"
         */
        println("%s,%d,%d,%d,%d,%d,%d".format(
          app.name,

          allAppMethods.size,
          interestingAppMethods.size,
          interestingAppMethods.size - strictlySerializableMethods.size,
          interestingAppMethods.size - relaxedSerializableMethods.size,
          hcAllAppMethodsFittingCriteria.size,
          scAllAppMethodsFittingCriteria.size))
    }
  }

  private def classNameMatches(s: String)(c: (JavaClass, Method)): Boolean = {
    s.r.findFirstIn(c._1.getClassName).isDefined
  }

  private def loadAndroidRepository(sdkPath: String): Repository = {
    val repository = SyntheticRepository.getInstance
    val files = FileFinder.recursiveListFiles(new File(sdkPath))
    val clazzFiles = FileFinder.getClassFilePaths(files)
    clazzFiles.foreach { clazzPath =>
      val javaClazz = new ClassParser(clazzPath).parse()
      repository.storeClass(javaClazz)
    }
    repository
  }

  private def loadClassesToRepository(path: String, repository: Repository): List[JavaClass] = {
    val files = FileFinder.recursiveListFiles(new File(path))
    val clazzFiles = FileFinder.getClassFilePaths(files)
    val clazzes = clazzFiles.map { path => new ClassParser(path).parse() }
    clazzes.foreach { c =>
      repository.storeClass(c)
      c.setRepository(repository)
    }
    clazzes
  }

  private def isThinkAirLibraryClass(c: JavaClass) = {
    c.getClassName.startsWith("de.tlabs.thinkAir.")
  }
}