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

// constants
object CLI {
    val scope = "sd"
    val usage = "notavail|loops|users|providers|b2b"
    val description = """Service Diagnostics
        |sd notavail: dumps a summary of leaf missing dependencies as a json map of component to list of dependencies
        |sd loops [-o]: checks for unresolvable components (typically circular dependencies). -o to include optionals.
        |sd users: dumps service usage as a json map of service to list of using bundles 
        |sd providers: dumps service providers as a json map of bundle to list of services
        |sd b2b: shows bundle to bundle links via service usage (merged users and providers) as a json map of provider to list of users
        |""".stripMargin
}
class CLI extends Command
{
    var engine:ServiceDiagnostics = _ //dependency injection. see Activator.

    override def getName = CLI.scope
    override def getShortDescription = CLI.description
    override def getUsage = CLI.usage

    // for gogo
    def users = execute("sd users", System.out, System.err)
    def providers = execute("sd providers", System.out, System.err)
    def b2b = execute("sd b2b", System.out, System.err)
    def notavail = execute("sd notavail", System.out, System.err)
    def loops = execute("sd loops", System.out, System.err)

    // for old shell
    override def execute(commandLine:String, out:PrintStream, err:PrintStream) = commandLine.split(" ").toList.tail match {
        case "users"::Nil => 
            out.println(json(engine.usingBundles).toString(2))
        case "providers"::Nil => 
            out.println(json(engine.serviceProviders).toString(2))
        case "b2b"::Nil => 
            out.println(json(engine.b2b).toString(2))
        case "notavail"::Nil => 
            out.println(json(engine.notavail).toString(2))
        case "loops"::tail => tail match {
            case "-o"::Nil => showloops(out, true)
            case _  => showloops(out, false)
        }
      case _ => err.println(getUsage)
    }

    def showloops(out:PrintStream, o:Boolean) = {
        val unresolved = engine.unresolved(o) // map(comp -> list(comp))
        out.println(json(unresolved).toString(2))
        def follow(n:String, stack:Set[String] = Set()) :Set[String] = 
            if (stack contains n) stack 
            else unresolved.get(n) match {
                case None => stack
                case Some(list) => list.toSet.flatMap { (d:String) => follow(d, stack+n) }
            }
        unresolved.keySet.map(follow(_)).foreach { loop => 
            if (loop.size > 1 && unresolved(loop.last).contains(loop.head))
                out.println(loop.mkString("", " -> ", " -> "+loop.head)) 
        }
    }

}
