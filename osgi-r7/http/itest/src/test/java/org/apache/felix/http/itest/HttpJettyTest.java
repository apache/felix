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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PAYMENT_REQUIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpJettyTest extends BaseIntegrationTest
{

    /**
     * Tests the starting of Jetty.
     */
    @Test
    public void test00_StartJettyOk() throws Exception
    {
        assertTrue(getHttpJettyBundle().getState() == Bundle.ACTIVE);

        assertResponseCode(SC_NOT_FOUND, createURL("/"));
    }

    /**
     * Tests the starting of Jetty.
     */
    @Test
    public void test00_StopJettyOk() throws Exception
    {
        Bundle bundle = getHttpJettyBundle();

        assertTrue(bundle.getState() == Bundle.ACTIVE);

        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch);

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_OK, createURL("/test"));

        bundle.stop();

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        try
        {
            createURL("/test").openStream();
            fail("Could connect to stopped Jetty instance?!");
        }
        catch (ConnectException e)
        {
            // Ok; expected...
        }

        bundle.start();

        Thread.sleep(500); // Allow Jetty to start (still done asynchronously)...

        assertResponseCode(SC_NOT_FOUND, createURL("/test"));
    }

    @Test
    public void testCorrectPathInfoInHttpContextOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        HttpContext context = new HttpContext()
        {
            @Override
            public String getMimeType(String name)
            {
                return null;
            }

            @Override
            public URL getResource(String name)
            {
                return null;
            }

            @Override
            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                try
                {
                    assertEquals("", request.getContextPath());
                    assertEquals("/foo", request.getServletPath());
                    assertEquals("/bar", request.getPathInfo());
                    assertEquals("/foo/bar", request.getRequestURI());
                    assertEquals("qux=quu", request.getQueryString());
                    return true;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return false;
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch);

        register("/foo", servlet, context);

        URL testURL = createURL("/foo/bar?qux=quu");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_OK, testURL);

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, testURL);
    }

    /**
     * Tests that we can register servlets and filters together.
     */
    @Test
    public void testHandleMultipleRegistrationsOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(3);
        CountDownLatch destroyLatch = new CountDownLatch(3);

        TestServlet servlet1 = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            final AtomicLong m_count = new AtomicLong();

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                resp.setStatus(SC_OK);
                resp.getWriter().printf("1.%d", m_count.incrementAndGet());
                resp.flushBuffer();
            }
        };

        TestServlet servlet2 = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            final AtomicLong m_count = new AtomicLong();

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                resp.setStatus(SC_OK);
                resp.getWriter().printf("2.%d", m_count.incrementAndGet());
                resp.flushBuffer();
            }
        };

        TestFilter filter = new TestFilter(initLatch, destroyLatch)
        {
            @Override
            protected void filter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException
            {
                String param = req.getParameter("param");
                if ("forbidden".equals(param))
                {
                    resp.reset();
                    resp.sendError(SC_FORBIDDEN);
                    resp.flushBuffer();
                }
                else
                {
                    chain.doFilter(req, resp);
                }
            }
        };

        register("/test/1", servlet1);
        register("/test/2", servlet2);
        register("/test/.*", filter);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("1.1", createURL("/test/1"));
        assertContent("2.1", createURL("/test/2"));
        assertContent("2.2", createURL("/test/2"));
        assertContent("1.2", createURL("/test/1"));
        assertContent("2.3", createURL("/test/2"));

        assertResponseCode(SC_FORBIDDEN, createURL("/test/2?param=forbidden"));
        assertResponseCode(SC_NOT_FOUND, createURL("/test?param=not_recognized"));

        assertContent("2.4", createURL("/test/2"));
        assertContent("1.3", createURL("/test/1"));

        assertResponseCode(SC_NOT_FOUND, createURL("/test?param=forbidden"));

        unregister(servlet1);
        unregister(servlet2);
        unregister(filter);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Test case for FELIX-3988, handling security constraints in {@link Filter}s.
     */
    @Test
    public void testHandleSecurityInFilterOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(2);
        CountDownLatch destroyLatch = new CountDownLatch(2);

        HttpContext context = new HttpContext()
        {
            @Override
            public String getMimeType(String name)
            {
                return null;
            }

            @Override
            public URL getResource(String name)
            {
                return null;
            }

            @Override
            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                if (request.getParameter("setStatus") != null)
                {
                    response.setStatus(SC_PAYMENT_REQUIRED);
                }
                else if (request.getParameter("sendError") != null)
                {
                    response.sendError(SC_PAYMENT_REQUIRED);
                }
                else if (request.getParameter("commit") != null)
                {
                    if (!response.isCommitted())
                    {
                        response.getWriter().append("Not allowed!");
                        response.flushBuffer();
                    }
                }
                return false;
            }
        };

        TestFilter filter = new TestFilter(initLatch, destroyLatch);
        TestServlet servlet = new TestServlet(initLatch, destroyLatch);

        register("/foo", servlet, context);
        register("/.*", filter, context);

        URL url1 = createURL("/foo");
        URL url2 = createURL("/foo?sendError=true");
        URL url3 = createURL("/foo?setStatus=true");
        URL url4 = createURL("/foo?commit=true");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_FORBIDDEN, url1);
        assertResponseCode(SC_PAYMENT_REQUIRED, url2);
        assertResponseCode(SC_PAYMENT_REQUIRED, url3);
        assertContent(SC_OK, "Not allowed!", url4);

        unregister(filter);
        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, url1);
    }

    /**
     * Tests that we can register a filter with Jetty and that its lifecycle is correctly controlled.
     */
    @Test
    public void testRegisterFilterLifecycleOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestFilter filter = new TestFilter(initLatch, destroyLatch);

        register("/test", filter);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        unregister(filter);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that we can register a servlet with Jetty and that its lifecycle is correctly controlled.
     */
    @Test
    public void testRegisterServletLifecycleOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch);

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Tests that initialization parameters are properly passed.
     */
    @Test
    public void testInitParametersOk() throws Exception
    {
        final CountDownLatch initLatch = new CountDownLatch(1);

        Servlet servlet = new HttpServlet()
        {
            @Override
            public void init(ServletConfig config) throws ServletException
            {
                String value1 = config.getInitParameter("key1");
                String value2 = config.getInitParameter("key2");
                if ("value1".equals(value1) && "value2".equals(value2))
                {
                    initLatch.countDown();
                }
            }
        };

        Dictionary params = new Hashtable();
        params.put("key1", "value1");
        params.put("key2", "value2");

        getHttpService().registerServlet("/initTest", servlet, params, null);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        unregister(servlet);
    }

    @Test
    public void testUseServletContextOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        HttpContext context = new HttpContext()
        {
            @Override
            public String getMimeType(String name)
            {
                return null;
            }

            @Override
            public URL getResource(String name)
            {
                try
                {
                    File f = new File("src/test/resources/resource/" + name);
                    if (f.exists())
                    {
                        return f.toURI().toURL();
                    }
                }
                catch (MalformedURLException e)
                {
                    fail();
                }
                return null;
            }

            @Override
            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                return true;
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void init(ServletConfig config) throws ServletException
            {
                ServletContext context = config.getServletContext();
                try
                {
                    assertEquals("", context.getContextPath());
                    assertNotNull(context.getResource("test.html"));
                    assertNotNull(context.getRealPath("test.html"));
                }
                catch (MalformedURLException e)
                {
                    fail();
                }

                super.init(config);
            }
        };

        register("/foo", servlet, context);

        URL testURL = createURL("/foo");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_OK, testURL);

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, testURL);
    }
}
