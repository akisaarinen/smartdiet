package fi.akisaarinen.smartdiet.energyusage.calltree

sealed class MethodCode
case object Enter extends MethodCode
case object Exit extends MethodCode
case object Unroll extends MethodCode

case class MethodCall(thread: Int, code: MethodCode, timestamp: Double, method: String, packetCount: Int, packetSize: Int, packetIndices: List[Int])
case object RootMethod extends MethodCall(-1, Enter, 0, "_root", 0, 0, List())