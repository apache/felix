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

import static javax.servlet.RequestDispatcher.FORWARD_CONTEXT_PATH;
import static javax.servlet.RequestDispatcher.FORWARD_PATH_INFO;
import static javax.servlet.RequestDispatcher.FORWARD_QUERY_STRING;
import static javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI;
import static javax.servlet.RequestDispatcher.FORWARD_SERVLET_PATH;
import static javax.servlet.RequestDispatcher.INCLUDE_CONTEXT_PATH;
import static javax.servlet.RequestDispatcher.INCLUDE_PATH_INFO;
import static javax.servlet.RequestDispatcher.INCLUDE_QUERY_STRING;
import static javax.servlet.RequestDispatcher.INCLUDE_REQUEST_URI;
import static javax.servlet.RequestDispatcher.INCLUDE_SERVLET_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.service.http.NamespaceException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class RequestDispatchTest extends BaseIntegrationTest
{
    /**
     * Tests that we can forward content from other servlets using the {@link RequestDispatcher} service.
     */
    @Test
    public void testDispatchForwardToAbsoluteURIOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet forward = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object includeContextPath = req.getAttribute(FORWARD_CONTEXT_PATH);
                if (includeContextPath != null)
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/forward", req.getServletPath());
                    assertEquals(null, req.getPathInfo());
                    assertEquals("/forward", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("", includeContextPath);
                    assertEquals("/test", req.getAttribute(FORWARD_SERVLET_PATH));
                    assertEquals("/foo", req.getAttribute(FORWARD_PATH_INFO));
                    assertEquals("/test/foo", req.getAttribute(FORWARD_REQUEST_URI));
                    assertEquals("bar=qux&quu", req.getAttribute(FORWARD_QUERY_STRING));
                }
                else
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/forward", req.getServletPath());
                    assertEquals("/bar", req.getPathInfo());
                    assertEquals("/forward/bar", req.getRequestURI());
                    assertEquals("quu=qux", req.getQueryString());
                }

                resp.getWriter().print("FORWARD\n");
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                assertEquals("", req.getContextPath());
                assertEquals("/test", req.getServletPath());
                assertEquals("/foo", req.getPathInfo());
                assertEquals("/test/foo", req.getRequestURI());
                assertEquals("bar=qux&quu", req.getQueryString());

                resp.getWriter().print("NOT_SEND\n");
                req.getRequestDispatcher("/forward").forward(req, resp);
                resp.getWriter().print("NOT_SEND\n");
            }
        };

        register("/forward", forward);
        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("FORWARD\n", createURL("/test/foo?bar=qux&quu"));
        assertContent("FORWARD\n", createURL("/forward/bar?quu=qux"));

        unregister(forward);
        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can forward content from other servlets using the {@link RequestDispatcher} service.
     */
    @Test
    public void testDispatchForwardToRelativeURIOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object contextPathAttr = req.getAttribute(FORWARD_CONTEXT_PATH);
                if (contextPathAttr != null)
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/test", req.getServletPath());
                    assertEquals("/forward", req.getPathInfo());
                    assertEquals("/test/forward", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("", contextPathAttr);
                    assertEquals("/test", req.getAttribute(FORWARD_SERVLET_PATH));
                    assertEquals("/foo", req.getAttribute(FORWARD_PATH_INFO));
                    assertEquals("/test/foo", req.getAttribute(FORWARD_REQUEST_URI));
                    assertEquals("bar=qux&quu", req.getAttribute(FORWARD_QUERY_STRING));

                    resp.getWriter().print("FORWARD\n");
                }
                else
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/test", req.getServletPath());
                    assertEquals("/foo", req.getPathInfo());
                    assertEquals("/test/foo", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    resp.getWriter().print("NOT_SEND\n");

                    // ServletContext#getRequestDispatcher only takes absolute paths...
                    RequestDispatcher disp = req.getServletContext().getRequestDispatcher("forward");
                    assertNull("ServletContext returned RequestDispatcher for relative path?!", disp);
                    // Causes a request to ourselves being made (/test/forward)...
                    disp = req.getRequestDispatcher("forward");
                    assertNotNull("ServletRequest returned NO RequestDispatcher for relative path?!", disp);

                    disp.forward(req, resp);
                    resp.getWriter().print("NOT_SEND\n");
                }
            }
        };

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("FORWARD\n", createURL("/test/foo?bar=qux&quu"));

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can include content from other servlets using the {@link RequestDispatcher} service.
     */
    @Test
    public void testDispatchIncludeAbsoluteURIOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet include = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object includeContextPath = req.getAttribute(INCLUDE_CONTEXT_PATH);
                if (includeContextPath != null)
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/test", req.getServletPath());
                    assertEquals("/foo", req.getPathInfo());
                    assertEquals("/test/foo", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("", includeContextPath);
                    assertEquals("/include", req.getAttribute(INCLUDE_SERVLET_PATH));
                    assertEquals(null, req.getAttribute(INCLUDE_PATH_INFO));
                    assertEquals("/include", req.getAttribute(INCLUDE_REQUEST_URI));
                    assertEquals(null, req.getAttribute(INCLUDE_QUERY_STRING));
                }
                else
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/include", req.getServletPath());
                    assertEquals("/bar", req.getPathInfo());
                    assertEquals("/include/bar", req.getRequestURI());
                    assertEquals("quu=qux", req.getQueryString());
                }

                resp.getWriter().print("INCLUDE\n");
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                assertEquals("", req.getContextPath());
                assertEquals("/test", req.getServletPath());
                assertEquals("/foo", req.getPathInfo());
                assertEquals("/test/foo", req.getRequestURI());
                assertEquals("bar=qux&quu", req.getQueryString());

                resp.getWriter().print("BEFORE\n");
                req.getRequestDispatcher("/include").include(req, resp);
                resp.getWriter().print("AFTER\n");
            }
        };

        register("/include", include);
        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("BEFORE\nINCLUDE\nAFTER\n", createURL("/test/foo?bar=qux&quu"));
        assertContent("INCLUDE\n", createURL("/include/bar?quu=qux"));

        unregister(include);
        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can include content from other servlets using the {@link RequestDispatcher} service.
     */
    @Test
    public void testDispatchIncludeRelativeURIOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object contextPathAttr = req.getAttribute(INCLUDE_CONTEXT_PATH);
                if (contextPathAttr != null)
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/foo", req.getPathInfo());
                    assertEquals("/test", req.getServletPath());
                    assertEquals("/test/foo", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("", contextPathAttr);
                    assertEquals("/test", req.getAttribute(INCLUDE_SERVLET_PATH));
                    //                    assertEquals("/include", req.getAttribute(INCLUDE_PATH_INFO));
                    //                    assertEquals("/test/include", req.getAttribute(INCLUDE_REQUEST_URI));
                    assertEquals(null, req.getAttribute(INCLUDE_QUERY_STRING));

                    resp.getWriter().print("INCLUDE\n");
                }
                else
                {
                    assertEquals("", req.getContextPath());
                    assertEquals("/test", req.getServletPath());
                    //                    assertEquals("/foo", req.getPathInfo());
                    //                    assertEquals("/test/foo", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    resp.getWriter().print("BEFORE\n");

                    // ServletContext#getRequestDispatcher only takes absolute paths...
                    RequestDispatcher disp = req.getServletContext().getRequestDispatcher("include");
                    assertNull("ServletContext returned RequestDispatcher for relative path?!", disp);
                    // Causes a request to ourselves being made (/test/forward)...
                    disp = req.getRequestDispatcher("include");
                    assertNotNull("ServletRequest returned NO RequestDispatcher for relative path?!", disp);

                    disp.include(req, resp);
                    resp.getWriter().print("AFTER\n");
                }
            }
        };

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("BEFORE\nINCLUDE\nAFTER\n", createURL("/test/foo?bar=qux&quu"));

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can forward content from other servlets using the {@link RequestDispatcher} service.
     */
    @Test
    public void testDispatchOnNonRootContextPathOk() throws Exception
    {
        // Configure HTTP on a different context path...
        configureHttpService(createDictionary("org.apache.felix.http.context_path", "/context", "org.osgi.service.http.port", "8080"));

        try
        {
            // Include two tests in one as to keep tests a little easier to read...
            doTestForwardAbsoluteURI();
            doTestIncludeAbsoluteURI();
        }
        finally
        {
            configureHttpService(null);
        }
    }

    private void doTestForwardAbsoluteURI() throws ServletException, NamespaceException, InterruptedException, IOException
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet forward = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object includeContextPath = req.getAttribute(FORWARD_CONTEXT_PATH);
                if (includeContextPath != null)
                {
                    assertEquals("/context", req.getContextPath());
                    assertEquals("/forward", req.getServletPath());
                    assertEquals(null, req.getPathInfo());
                    assertEquals("/context/forward", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("/context", includeContextPath);
                    assertEquals("/test", req.getAttribute(FORWARD_SERVLET_PATH));
                    assertEquals("/foo", req.getAttribute(FORWARD_PATH_INFO));
                    assertEquals("/context/test/foo", req.getAttribute(FORWARD_REQUEST_URI));
                    assertEquals("bar=qux&quu", req.getAttribute(FORWARD_QUERY_STRING));
                }
                else
                {
                    assertEquals("/context", req.getContextPath());
                    assertEquals("/forward", req.getServletPath());
                    assertEquals("/bar", req.getPathInfo());
                    assertEquals("/context/forward/bar", req.getRequestURI());
                    assertEquals("quu=qux", req.getQueryString());
                }

                resp.getWriter().print("FORWARD\n");
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                assertEquals("/context", req.getContextPath());
                assertEquals("/test", req.getServletPath());
                assertEquals("/foo", req.getPathInfo());
                assertEquals("/context/test/foo", req.getRequestURI());
                assertEquals("bar=qux&quu", req.getQueryString());

                resp.getWriter().print("NOT_SEND\n");
                req.getRequestDispatcher("/forward").forward(req, resp);
                resp.getWriter().print("NOT_SEND\n");
            }
        };

        register("/forward", forward);
        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("FORWARD\n", createURL("/context/test/foo?bar=qux&quu"));
        assertContent("FORWARD\n", createURL("/context/forward/bar?quu=qux"));

        unregister(forward);
        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can include content from other servlets using the {@link RequestDispatcher} service.
     */
    private void doTestIncludeAbsoluteURI() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        TestServlet include = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                Object includeContextPath = req.getAttribute(INCLUDE_CONTEXT_PATH);
                if (includeContextPath != null)
                {
                    assertEquals("/context", req.getContextPath());
                    assertEquals("/test", req.getServletPath());
                    assertEquals("/foo", req.getPathInfo());
                    assertEquals("/context/test/foo", req.getRequestURI());
                    assertEquals("bar=qux&quu", req.getQueryString());

                    assertEquals("/context", includeContextPath);
                    assertEquals("/include", req.getAttribute(INCLUDE_SERVLET_PATH));
                    assertEquals(null, req.getAttribute(INCLUDE_PATH_INFO));
                    assertEquals("/context/include", req.getAttribute(INCLUDE_REQUEST_URI));
                    assertEquals(null, req.getAttribute(INCLUDE_QUERY_STRING));
                }
                else
                {
                    assertEquals("/context", req.getContextPath());
                    assertEquals("/include", req.getServletPath());
                    assertEquals("/bar", req.getPathInfo());
                    assertEquals("/context/include/bar", req.getRequestURI());
                    assertEquals("quu=qux", req.getQueryString());
                }

                resp.getWriter().print("INCLUDE\n");
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                assertEquals("/context", req.getContextPath());
                assertEquals("/test", req.getServletPath());
                assertEquals("/foo", req.getPathInfo());
                assertEquals("/context/test/foo", req.getRequestURI());
                assertEquals("bar=qux&quu", req.getQueryString());

                resp.getWriter().print("BEFORE\n");
                req.getRequestDispatcher("/include").include(req, resp);
                resp.getWriter().print("AFTER\n");
            }
        };

        register("/include", include);
        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("BEFORE\nINCLUDE\nAFTER\n", createURL("/context/test/foo?bar=qux&quu"));
        assertContent("INCLUDE\n", createURL("/context/include/bar?quu=qux"));

        unregister(include);
        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }
}
