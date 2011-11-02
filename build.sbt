name := "powerprofiler"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Scala Tools Nexus Releases" at "http://nexus.scala-tools.org/content/repositories/releases/"

resolvers += "JBoss Glassfish" at "https://repository.jboss.org/nexus/content/repositories/glassfish"

resolvers += "JavaParser" at "http://javaparser.googlecode.com/svn/maven2"

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "1.6.1" % "test",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test",
    "org.scalaz" %% "scalaz-core" % "6.0.3",
    "joda-time" % "joda-time" % "1.6.2",
    "com.github.scopt" %% "scopt" % "1.1.2",
    "org.apache.bcel" % "bcel" % "5.2",
    "net.liftweb" %% "lift-json" % "2.4-M4",
    "com.google.code.javaparser" % "javaparser" % "1.0.8"
)


seq(sbtassembly.Plugin.assemblySettings: _*)

jarName in Assembly <<= (name,version) { (n,v) => "%s-%s.jar".format(n,v) }

packageOptions <<= (packageOptions, version) map { (opts, ver) =>
    val verWithDate = "%s (compiled %s)".format(ver, (new java.util.Date).toString)
    opts :+ Package.ManifestAttributes(java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> verWithDate)
}
