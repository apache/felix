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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
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

        unregister("/foo");

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, testURL);
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

        unregister("/test");

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

        unregister("/initTest");
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

        unregister("/foo");

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, testURL);
    }
}
