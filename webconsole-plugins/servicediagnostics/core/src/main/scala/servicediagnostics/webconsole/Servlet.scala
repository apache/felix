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
package org.apache.felix.servicediagnostics.webconsole

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => mMap}

import java.io.PrintStream
import javax.servlet.http._

import org.json.JSONObject

import org.osgi.service.http.HttpContext
import org.osgi.service.http.HttpService

import org.apache.felix.servicediagnostics.ServiceDiagnostics

/**
 * This is the servlet responding to the ajax requests, using the ServiceDiagnostics service
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class Servlet extends HttpServlet 
{

    var engine:ServiceDiagnostics = _ //dependency injection. see Activator.

    /*
    * dependency injection. see Activator.
    * registers this servlet into the HttpService
    */
    def setHttpService(hs:HttpService) = 
    {
        val hc = hs.createDefaultHttpContext
        hs.registerServlet("/servicegraph/json", this, null, hc)
        hs.registerResources("/servicegraph", "/html", hc)
    }

    override def service(req:HttpServletRequest, resp:HttpServletResponse) = 
    {
        val cmd = req.getPathInfo
        if(cmd.endsWith("all")) reply(resp, engine.allServices)
        else if(cmd.endsWith("notavail")) reply(resp, engine.notavail)
        else println("Unrecognized cmd: "+cmd)
    }

    /** 
    * turn the ServiceDiagnostics output into a JSON representation.
    */
    private def reply(resp:HttpServletResponse, map:Map[String,List[AnyRef]]) = 
    {
        new PrintStream(resp.getOutputStream, true).println(
            new JSONObject(asJavaMap(mMap() ++ map.map(kv => (kv._1, asJavaList(kv._2))))))
    }
}

