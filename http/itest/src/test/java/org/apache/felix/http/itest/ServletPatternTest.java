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

package org.apache.felix.http.itest;

import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;

@RunWith(JUnit4TestRunner.class)
public class ServletPatternTest extends BaseIntegrationTest
{

    @Test
    public void testHighRankReplaces() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet lowRankServlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
            {
                resp.getWriter().print("lowRankServlet");
                resp.flushBuffer();
            }
        };

        TestServlet highRankServlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
            {
                resp.getWriter().print("highRankServlet");
                resp.flushBuffer();
            }
        };

        Dictionary<String, Object> lowRankProps = new Hashtable<String, Object>();
        String lowRankPattern[] = { "/foo", "/bar" };
        lowRankProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, lowRankPattern);
        lowRankProps.put(HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "lowRankServlet");
        lowRankProps.put(SERVICE_RANKING, 1);

        ServiceRegistration<?> lowRankReg = m_context.registerService(Servlet.class.getName(),
            lowRankServlet, lowRankProps);

        Dictionary<String, Object> highRankProps = new Hashtable<String, Object>();
        String highRankPattern[] = { "/foo", "/baz" };
        highRankProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, highRankPattern);
        highRankProps.put(HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "highRankServlet");
        highRankProps.put(Constants.SERVICE_RANKING, 2);

        ServiceRegistration<?> highRankReg = m_context.registerService(Servlet.class.getName(),
            highRankServlet, highRankProps);

        try
        {
            assertTrue(initLatch.await(5, TimeUnit.SECONDS));

            assertContent("highRankServlet", createURL("/foo"));
            assertContent("lowRankServlet", createURL("/bar"));
            assertContent("highRankServlet", createURL("/baz"));

        }
        finally
        {
            lowRankReg.unregister();
            highRankReg.unregister();
        }

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testHttpServiceReplaces() throws Exception
    {
        Dictionary<String, ?> properties = createDictionary(
            HTTP_WHITEBOARD_CONTEXT_NAME, "test",
            HTTP_WHITEBOARD_CONTEXT_PATH, "/test");

        ServletContextHelper servletContextHelper = new ServletContextHelper(m_context.getBundle()) {};
        m_context.registerService(ServletContextHelper.class.getName(), servletContextHelper, properties);

        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet whiteboardServlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().print("whiteboardServlet");
                resp.flushBuffer();
            }
        };

        TestServlet httpServiceServlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().print("httpServiceServlet");
                resp.flushBuffer();
            }
        };

        String whiteboardPattern[] = { "/foo", "/bar" };
        Dictionary<String, Object> whiteboardProps = new Hashtable<String, Object>();
        whiteboardProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test)");
        whiteboardProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, whiteboardPattern);
        whiteboardProps.put(SERVICE_RANKING, Integer.MAX_VALUE);
        ServiceRegistration<?> serviceRegistration = m_context.registerService(Servlet.class.getName(), whiteboardServlet, whiteboardProps);

        register("/test/foo", httpServiceServlet);

        try
        {
            assertTrue(initLatch.await(5, TimeUnit.SECONDS));

            assertContent("whiteboardServlet", createURL("/test/bar"));
            assertContent("httpServiceServlet", createURL("/test/foo"));
        }
        finally
        {
            serviceRegistration.unregister();
            unregister(httpServiceServlet);
        }
    }

    @Test
    public void testSameRankDoesNotReplace() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet servlet1 = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().print("servlet1");
                resp.flushBuffer();
            }
        };

        TestServlet servlet2 = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().print("servlet2");
                resp.flushBuffer();
            }
        };

        Dictionary<String, Object> props1 = new Hashtable<String, Object>();
        String lowRankPattern[] = { "/foo", "/bar" };
        props1.put(HTTP_WHITEBOARD_SERVLET_PATTERN, lowRankPattern);
        props1.put(HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "lowRankServlet");
        props1.put(SERVICE_RANKING, 2);

        ServiceRegistration<?> reg1 = m_context.registerService(Servlet.class.getName(),
            servlet1, props1);

        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        String highRankPattern[] = { "/foo", "/baz" };
        props2.put(HTTP_WHITEBOARD_SERVLET_PATTERN, highRankPattern);
        props2.put(HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "highRankServlet");
        props2.put(SERVICE_RANKING, 2);

        ServiceRegistration<?> reg2 = m_context.registerService(Servlet.class.getName(),
            servlet2, props2);

        try
        {
            assertTrue(initLatch.await(5, TimeUnit.SECONDS));

            assertContent("servlet1", createURL("/foo"));
            assertContent("servlet1", createURL("/bar"));
            assertContent("servlet2", createURL("/baz"));

        }
        finally
        {
            reg1.unregister();
            reg2.unregister();
        }

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }
}
