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

import java.io.PrintStream

import javax.servlet.http._

import org.json.JSONObject
import org.json.JSONArray

import org.apache.felix.webconsole.SimpleWebConsolePlugin

import org.apache.felix.servicediagnostics.ServiceDiagnostics
import org.apache.felix.servicediagnostics.Util._

/**
 * This is the Apache Felix WebConsolePlugin implementation.
 * It simply returns a static html page; actual processing is done by an ajax request to the servlet.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class WebConsolePlugin extends SimpleWebConsolePlugin("servicegraph", "Service Graph", "OSGi", Array[String]()) 
{
    var engine:ServiceDiagnostics = _ //dependency injection. see Activator.

    val TEMPLATE = readTemplateFile("/html/index.html")

    /**
     * used for initial request, returns the contents of index.html.
     * the rest is done via ajax calls
     */
    override def renderContent(req:HttpServletRequest, resp:HttpServletResponse) = 
        resp.getWriter.print(TEMPLATE)

    /**
     * override doGet to handle ajax requests
     */
    override def doGet(req:HttpServletRequest, resp:HttpServletResponse) = 
        req.getPathInfo match {
            case "/servicegraph/users" => resp.getWriter.println(json(engine.usingBundles))
            case "/servicegraph/providers" => resp.getWriter.println(json(engine.serviceProviders))
            case "/servicegraph/b2b" => resp.getWriter.println(json(engine.b2b))
            case "/servicegraph/notavail" => resp.getWriter.println(new JSONObject()
                                  .put("notavail", json(engine.notavail))
                                  .put("unresolved", 
                                      json(engine.unresolved(
                                          Option(req.getParameter("optionals")).isDefined))))
            case x => super.doGet(req, resp)
          }
}
