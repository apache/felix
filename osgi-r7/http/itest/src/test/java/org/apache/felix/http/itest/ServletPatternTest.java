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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.itest.HttpServiceRuntimeTest.TestResource;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ServletPatternTest extends BaseIntegrationTest
{
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private CountDownLatch initLatch;
    private CountDownLatch destroyLatch;

    public void setupLatches(int count)
    {
        initLatch = new CountDownLatch(count);
        destroyLatch = new CountDownLatch(count);
    }

    public void setupServlet(final String name, String[] path, int rank, String context) throws Exception
    {
        Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
        servletProps.put(HTTP_WHITEBOARD_SERVLET_NAME, name);
        servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, path);
        servletProps.put(SERVICE_RANKING, rank);
        if (context != null)
        {
            servletProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + context + ")");
        }

        TestServlet servletWithErrorCode = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
            {
                resp.getWriter().print(name);
                resp.flushBuffer();
            }
        };

        registrations.add(m_context.registerService(Servlet.class.getName(), servletWithErrorCode, servletProps));
    }

    private void setupContext(String name, String path) throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_PATH, path);

        ServletContextHelper servletContextHelper = new ServletContextHelper(m_context.getBundle()){
            // test helper
        };
        registrations.add(m_context.registerService(ServletContextHelper.class.getName(), servletContextHelper, properties));

        Thread.sleep(500);
    }

    @After
    public void unregisterServices() throws InterruptedException
    {
        for (ServiceRegistration<?> serviceRegistration : registrations)
        {
            serviceRegistration.unregister();
        }

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        Thread.sleep(500);
    }

    @Test
    public void testHighRankReplaces() throws Exception
    {
        setupLatches(2);

        setupServlet("lowRankServlet", new String[] { "/foo", "/bar" }, 1, null);
        setupServlet("highRankServlet", new String[] { "/foo", "/baz" }, 2, null);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("highRankServlet", createURL("/foo"));
        assertContent("lowRankServlet", createURL("/bar"));
        assertContent("highRankServlet", createURL("/baz"));
    }

    @Test
    public void testHttpServiceReplaces() throws Exception
    {
        setupLatches(2);

        setupContext("contextA", "/test");
        setupServlet("whiteboardServlet", new String[]{ "/foo", "/bar" }, Integer.MAX_VALUE, "contextA");

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

        register("/test/foo", httpServiceServlet);

        try
        {
            assertTrue(initLatch.await(5, TimeUnit.SECONDS));

            assertContent("whiteboardServlet", createURL("/test/bar"));
            assertContent("whiteboardServlet", createURL("/test/bar"));
        }
        finally
        {
            unregister(httpServiceServlet);
        }
    }

    @Test
    public void testSameRankDoesNotReplace() throws Exception
    {
        setupLatches(2);

        setupServlet("servlet1", new String[]{ "/foo", "/bar" }, 2, null);
        setupServlet("servlet2", new String[]{ "/foo", "/baz" }, 2, null);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("servlet1", createURL("/foo"));
        assertContent("servlet1", createURL("/bar"));
        assertContent("servlet2", createURL("/baz"));
    }

    @Test
    public void testHighRankResourceReplaces() throws Exception
    {
        setupLatches(1);

        setupServlet("lowRankServlet", new String[]{ "/foo" }, 1, null);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
        assertContent("lowRankServlet", createURL("/foo"));

        Dictionary<String, Object> resourceProps = new Hashtable<String, Object>();
        String highRankPattern[] = { "/foo" };
        resourceProps.put(HTTP_WHITEBOARD_RESOURCE_PATTERN, highRankPattern);
        resourceProps.put(HTTP_WHITEBOARD_RESOURCE_PREFIX, "/resource/test.html");
        resourceProps.put(SERVICE_RANKING, 2);

        registrations.add(m_context.registerService(TestResource.class.getName(),
            new TestResource(), resourceProps));

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
        Thread.sleep(500);

        assertContent(getTestHtmlContent(), createURL("/foo"));
    }

    private String getTestHtmlContent() throws IOException
    {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/resource/test.html");
        return slurpAsString(resourceAsStream);
    }

    @Test
    public void contextWithLongerPrefixIsChosen() throws Exception
    {
        setupLatches(2);

        setupContext("contextA", "/a");
        setupContext("contextB", "/a/b");

        setupServlet("servlet1", new String[]{ "/b/test" }, 1, "contextA");

        Thread.sleep(500);
        assertEquals(1, initLatch.getCount());
        assertContent("servlet1", createURL("/a/b/test"));

        setupServlet("servlet2", new String[]{ "/test" }, 1, "contextB");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
        assertContent("servlet2", createURL("/a/b/test"));
    }

    @Test
    public void contextWithLongerPrefixIsChosenWithWildcard() throws Exception
    {
        setupLatches(2);

        setupContext("contextA", "/a");
        setupContext("contextB", "/a/b");

        setupServlet("servlet1", new String[]{ "/b/test/servlet" }, 1, "contextA");

        Thread.sleep(500);
        assertEquals(1, initLatch.getCount());
        assertContent("servlet1", createURL("/a/b/test/servlet"));

        setupServlet("servlet2", new String[]{ "/test/*" }, 1, "contextB");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
        assertContent("servlet2", createURL("/a/b/test/servlet"));
    }

    @Test
    public void pathMatchingTest() throws Exception
    {
        setupLatches(1);

        setupContext("contextA", "/a");

        setupServlet("servlet1", new String[]{ "/servlet/*" }, 1, "contextA");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
        assertContent("servlet1", createURL("/a/servlet/foo"));
        assertContent("servlet1", createURL("/a/servlet"));
    }
}
