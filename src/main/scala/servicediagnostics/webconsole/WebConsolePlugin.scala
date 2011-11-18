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

import javax.servlet.http._

import org.apache.felix.webconsole.SimpleWebConsolePlugin

/**
 * This is the Apache Felix WebConsolePlugin implementation.
 * It simply returns a static html page; actual processing is done by an ajax request to the servlet.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class WebConsolePlugin extends SimpleWebConsolePlugin("servicegraph", "Service Graph", Array[String]()) 
{
    val TEMPLATE = readTemplateFile("/html/index.html")

    override def renderContent(req:HttpServletRequest, resp:HttpServletResponse) = resp.getWriter().print( TEMPLATE )
}
