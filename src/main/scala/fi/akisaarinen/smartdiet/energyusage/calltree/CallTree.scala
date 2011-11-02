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