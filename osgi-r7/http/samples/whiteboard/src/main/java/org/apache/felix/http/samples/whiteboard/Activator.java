/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.samples.whiteboard;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class Activator
    implements BundleActivator
{

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        // create a servlet context
        final TestServletContext servletContext = new TestServletContext(context.getBundle());

        // register the servlet context with name "filtersample" at "/filtersample"
        final Dictionary<String, Object> servletContextProps = new Hashtable<String, Object>();
        servletContextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "filtersample");
        servletContextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/filtersample");
        context.registerService(ServletContextHelper.class, servletContext, servletContextProps);

        // create and register servlets
        final TestServlet servlet1 = new TestServlet("servlet1");
        final Dictionary<String, Object> servlet1Props = new Hashtable<String, Object>();
        servlet1Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");
        servlet1Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=filtersample)");
        context.registerService(Servlet.class, servlet1, servlet1Props);

        final TestServlet servlet2 = new TestServlet("servlet2");
        final Dictionary<String, Object> servlet2Props = new Hashtable<String, Object>();
        servlet2Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/other/*");
        servlet2Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=filtersample)");
        context.registerService(Servlet.class, servlet2, servlet2Props);

        // create and register filters
        final TestFilter filter1 = new TestFilter("filter1");
        final Dictionary<String, Object> filter1Props = new Hashtable<String, Object>();
        filter1Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
        filter1Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=filtersample)");
        context.registerService(Filter.class, filter1, filter1Props);

        final TestFilter filter2 = new TestFilter("filter2");
        final Dictionary<String, Object> filter2Props = new Hashtable<String, Object>();
        filter2Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/other/*");
        filter2Props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=filtersample)");
        context.registerService(Filter.class, filter2, filter2Props);
    }

    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        // nothing to do, services are unregistered automatically
    }
}
