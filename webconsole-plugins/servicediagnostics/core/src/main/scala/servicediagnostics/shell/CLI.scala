/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.servicediagnostics.shell

import org.apache.felix.servicediagnostics.ServiceDiagnostics
import org.apache.felix.servicediagnostics.Util._

// old shell
import org.apache.felix.shell.Command
import java.io.PrintStream
// gogo shell
import org.apache.felix.service.command.Descriptor

class CLI extends Command
{
    var engine:ServiceDiagnostics = _ //dependency injection. see Activator.

    override def getName = "sd"
    override def getShortDescription = "Service Diagnostics"
    override def getUsage = "notavail|loops|using|providing"

    // for gogo
    def using = execute("sd using", System.out, System.err)
    def providing = execute("sd providing", System.out, System.err)
    def notavail = execute("sd notavail", System.out, System.err)
    def loops = execute("sd loops", System.out, System.err)

    // for old shell
    override def execute(commandLine:String, out:PrintStream, err:PrintStream) = commandLine.split(" ").toList.tail match {
        case "using"::Nil => 
            out.println(json(engine.usingBundles).toString(2))
        case "providing"::Nil => 
            out.println(json(engine.serviceProviders).toString(2))
        case "notavail"::Nil => 
            out.println(json(engine.notavail).toString(2))
        case "loops"::Nil => showloops(out)
      case _ => err.println(getUsage)
    }

    def showloops(out:PrintStream) = {
        val unresolved = engine.unresolved(false) // map(comp -> list(comp))
        out.println(json(unresolved).toString(2))
        def follow(n:String, stack:Set[String] = Set()) :Set[String] = 
            if (stack contains n) stack 
            else unresolved.get(n) match {
                case None => stack
                case Some(list) => list.toSet.flatMap { (d:String) => follow(d, stack+n) }
            }
        unresolved.keySet.map(follow(_)).foreach { loop => 
            if (loop.size > 1 && unresolved(loop.last) == loop.head) 
                out.println(loop.mkString("", " -> ", " -> "+loop.head)) 
        }
    }

}
