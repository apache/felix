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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class ErrorPageTest extends BaseIntegrationTest
{
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private CountDownLatch initLatch;
    private CountDownLatch destroyLatch;

    public void setupLatches(int count)
    {
        initLatch = new CountDownLatch(count);
        destroyLatch = new CountDownLatch(count);
    }

    public void setupErrorServlet(final Integer errorCode,
        final Class<? extends RuntimeException> exceptionType,
        String context) throws Exception
    {
        Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
        servletProps.put(HTTP_WHITEBOARD_SERVLET_NAME, "servlet");
        servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, asList("/test"));
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
                if (errorCode != null)
                {
                    resp.sendError(errorCode);
                }

                if (exceptionType != null)
                {
                    RuntimeException exception;
                    try
                    {
                        exception = exceptionType.newInstance();
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                    throw exception;
                }
            }
        };

        registrations.add(m_context.registerService(Servlet.class.getName(), servletWithErrorCode, servletProps));
    }

    public void setupErrorPage(final Integer errorCode,
        final Class<? extends Throwable> exceptionType,
        final String name,
        String context) throws Exception
    {
        TestServlet errorPage = new TestServlet(initLatch, destroyLatch)
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

        List<String> errors = new ArrayList<String>();
        if (errorCode != null)
        {
            errors.add(errorCode.toString());
        }
        if (exceptionType != null)
        {
            errors.add(exceptionType.getName());
        }

        Dictionary<String, Object> errorPageProps = new Hashtable<String, Object>();
        errorPageProps.put(HTTP_WHITEBOARD_SERVLET_NAME, name);
        errorPageProps.put(HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, errors);
        if (context != null)
        {
            errorPageProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + context + ")");
        }

        registrations.add(m_context.registerService(Servlet.class.getName(), errorPage, errorPageProps));
    }

    private void registerContext(String name, String path) throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_PATH, path);

        ServletContextHelper servletContextHelper = new ServletContextHelper(m_context.getBundle()){
            // test
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
    public void errorPageForErrorCodeIsSent() throws Exception
    {
        setupLatches(2);
        setupErrorServlet(501, null, null);
        setupErrorPage(501, null, "Error page", null);
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(501, "Error page", createURL("/test"));
    }

    @Test
    public void errorPageForExceptionIsSent() throws Exception
    {
        setupLatches(2);
        setupErrorServlet(null, NullPointerException.class, null);
        setupErrorPage(null, NullPointerException.class, "Error page", null);
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(500, "Error page", createURL("/test"));
    }

    @Test
    public void errorPageForParentExceptionIsSent() throws Exception
    {
        setupLatches(2);
        setupErrorServlet(null, NullPointerException.class, null);
        setupErrorPage(null, RuntimeException.class, "Error page", null);
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(500, "Error page", createURL("/test"));
    }

    @Test
    public void errorPageForExceptionIsPreferedOverErrorCode() throws Exception
    {
        setupLatches(3);
        setupErrorServlet(null, NullPointerException.class, null);
        setupErrorPage(500, null, "Error page 2", null);
        setupErrorPage(null, NullPointerException.class, "Error page 1", null);
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(500, "Error page 1", createURL("/test"));
    }

    @Test
    public void errorPageIsHandledPerContext() throws Exception
    {
        registerContext("context1", "/one");
        registerContext("context2", "/two");

        setupLatches(3);
        setupErrorServlet(501, null, "context1");
        setupErrorPage(501, null, "Error page 1", "context2");
        setupErrorPage(501, null, "Error page 2", "context1");
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(501, "Error page 2", createURL("/one/test"));
    }

    @Test
    public void errorPageIsShadowedByHigherRankingPage() throws Exception
    {
        registerContext("context1", "/one");
        registerContext("context2", "/two");

        // Shadowed error page is not initialized
        setupLatches(2);
        setupErrorServlet(501, null, "context1");
        setupErrorPage(501, null, "Error page 1", "context1");
        setupErrorPage(501, null, "Error page 2", "context1");
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent(501, "Error page 1", createURL("/one/test"));
    }
}
