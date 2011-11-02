package fi.akisaarinen.smartdiet.energyusage.calltree

import collection.mutable.Stack

class CallTree(val root: MethodCall, val children: Vector[MethodCall]) {  
  def findOrCreatePath(stack: List[MethodCall]): CallTree = {
    stack.foldLeft(this) { (current, stackMethod) =>
      if (current.root == stackMethod) {
        current
      } else {
        new CallTree(stackMethod, Vector())
      }
    }
  }  
  
  override def toString = {
    "Node(" + root.method + ")\n" + 
    	children.foldLeft("")(_+_.toString)
  }
}

object CallTree {
  def create(calls: IndexedSeq[MethodCall]) {
    val packetful = calls.filter(_.packetSize > 0)
    var tree = new CallTree(RootMethod, Vector())
    println("Packetful count: " + packetful.size)
    eachMethodWithStack(calls) { (stack, method) =>
      if (!packetful.contains(method)) {
        val stackWithMethod = stack.toList ::: List(method)
        tree = tree.findOrCreatePath(stackWithMethod)
      }
    }
    println(tree.toString)
  }

  def eachMethodWithStack(methods: IndexedSeq[MethodCall])(callback: (Stack[MethodCall], MethodCall) => Unit) {
    val stack = new Stack[MethodCall]
    val threads = methods.map(_.thread).distinct
    val threadStacks = threads.map(t => t -> new Stack[MethodCall]).toMap

    methods.foreach { method =>
      val stack = threadStacks(method.thread)
      method.code match {
        case Enter =>
          stack.push(method)
          callback(stack, method)
        case _ =>
          if (stack.size == 0)
            println("Ignoring " + method)
          else {
            callback(stack, method)
            stack.pop
          }
      }
    }
  }
}