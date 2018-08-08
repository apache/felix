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
package org.apache.felix.systemready.impl.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyMonitor;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide aggregated alive information using a servlet
 */
@Component(
        name = SystemAliveServlet.PID,
        service = Servlet.class,
        property = {
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + SystemAliveServlet.DEFAULT_PATH,
        }
)
@Designate(ocd=SystemAliveServlet.Config.class)
public class SystemAliveServlet extends HttpServlet {
    public static final String PID = "org.apache.felix.systemready.impl.servlet.SystemAliveServlet";
    public static final String DEFAULT_PATH = "/systemalive";

    private static final Logger LOG = LoggerFactory.getLogger(SystemAliveServlet.class);

    private static final long serialVersionUID = 1L;

    @ObjectClassDefinition(
            name ="System Alive Servlet",
            description="Servlet exposing a http endpoint for retrieving the alive status"
    )
    public @interface Config {

        @AttributeDefinition(name = "Servlet Path")
        String osgi_http_whiteboard_servlet_pattern() default SystemAliveServlet.DEFAULT_PATH;
        
        @AttributeDefinition(name = "Servlet Context select")
        String osgi_http_whiteboard_context_select();

    }

    @Reference
    private SystemReadyMonitor monitor;
    
	private StatusReporter reporter;

    @Activate
    protected void activate(final BundleContext ctx, final Map<String, Object> properties, final Config config) {
        final String path = config.osgi_http_whiteboard_servlet_pattern();
        LOG.info("Registered servlet to listen on {}", path);
        reporter = new StatusReporter(monitor, StateType.ALIVE);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        reporter.reportState(response);
    }



}
