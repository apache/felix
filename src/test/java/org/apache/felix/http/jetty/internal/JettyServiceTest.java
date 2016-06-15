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
package org.apache.felix.http.jetty.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.HttpServiceController;
import org.apache.felix.http.jetty.internal.JettyService.Deployment;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;

public class JettyServiceTest
{

    private static final String OSGI_BUNDLECONTEXT = "osgi-bundlecontext";

    private JettyService jettyService;

    private BundleContext mockBundleContext;

    private DispatcherServlet dispatcherServlet;

    private EventDispatcher mockEventDispatcher;

    private HttpServiceController httpServiceController;

    private Bundle mockBundle;

    @Before
    public void setUp() throws Exception
    {
        //Setup Mocks
        mockBundleContext = mock(BundleContext.class);
        mockEventDispatcher = mock(EventDispatcher.class);
        mockBundle = mock(Bundle.class);

        //Setup Behaviors
        when(mockBundleContext.getBundle()).thenReturn(mockBundle);
        final org.osgi.framework.Filter f = mock(org.osgi.framework.Filter.class);
        when(f.toString()).thenReturn("(prop=*)");
        when(mockBundleContext.createFilter(anyString())).thenReturn(f);
        when(mockBundle.getSymbolicName()).thenReturn("main");
        when(mockBundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(mockBundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        final ServiceReference ref = mock(ServiceReference.class);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        final ServiceRegistration reg = mock(ServiceRegistration.class);
        when(reg.getReference()).thenReturn(ref);
        when(mockBundleContext.registerService((Class<ServletContextHelper>)Matchers.isNotNull(),
                (ServiceFactory<ServletContextHelper>)Matchers.any(ServiceFactory.class),
                Matchers.any(Dictionary.class))).thenReturn(reg);
        when(mockBundleContext.registerService(Matchers.<String[]>any(),
                Matchers.any(ServiceFactory.class),
                Matchers.any(Dictionary.class))).thenReturn(reg);
        when(mockBundleContext.registerService((Class<HttpServiceRuntime>)Matchers.isNotNull(),
                Matchers.any(HttpServiceRuntime.class),
                Matchers.any(Dictionary.class))).thenReturn(reg);

        httpServiceController = new HttpServiceController(mockBundleContext);
        dispatcherServlet = new DispatcherServlet(httpServiceController);
        jettyService = new JettyService(mockBundleContext, dispatcherServlet, mockEventDispatcher, httpServiceController);

        jettyService.start();
    }

    @After
    public void tearDown() throws Exception {
        jettyService.stop();
    }

    /**
     *
     * Tests to ensure the osgi-bundlecontext is available for init methods.
     *
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test public void testInitBundleContextDeployIT() throws Exception
    {
        //Setup mocks
        Deployment mockDeployment = mock(Deployment.class);
        Bundle mockBundle = mock(Bundle.class);
        BundleContext mockBundleContext = mock(BundleContext.class);

        //Setup behaviors
        when(mockDeployment.getBundle()).thenReturn(mockBundle);
        final org.osgi.framework.Filter f = mock(org.osgi.framework.Filter.class);
        when(f.toString()).thenReturn("(prop=*)");
        when(mockBundleContext.createFilter(anyString())).thenReturn(f);
        when(mockBundle.getBundleContext()).thenReturn(mockBundleContext);
        when(mockBundle.getSymbolicName()).thenReturn("test");
        when(mockBundle.getVersion()).thenReturn(new Version("0.0.1"));

        Dictionary<String, String> headerProperties = new Hashtable<String, String>();
        headerProperties.put("Web-ContextPath", "test");
        when(mockBundle.getHeaders()).thenReturn(headerProperties);
        when(mockDeployment.getContextPath()).thenReturn("test");
        when(mockBundle.getEntry("/")).thenReturn(new URL("http://www.apache.com"));
        when(mockBundle.getState()).thenReturn(Bundle.ACTIVE);

        EnumSet<DispatcherType> dispatcherSet = EnumSet.allOf(DispatcherType.class);
        dispatcherSet.add(DispatcherType.REQUEST);

        WebAppBundleContext webAppBundleContext = new WebAppBundleContext("/", mockBundle, this.getClass().getClassLoader());

        final CountDownLatch testLatch = new CountDownLatch(2);

        //Add a Filter to test whether the osgi-bundlecontext is available at init
        webAppBundleContext.addServlet(new ServletHolder(new Servlet()
        {
            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException
            {
                // Do Nothing
            }

            @Override
            public void init(ServletConfig config) throws ServletException
            {
                ServletContext context = config.getServletContext();

                assertNotNull(context.getAttribute(OSGI_BUNDLECONTEXT));

                testLatch.countDown();
            }

            @Override
            public String getServletInfo()
            {
                return null;
            }

            @Override
            public ServletConfig getServletConfig()
            {
                return null;
            }

            @Override
            public void destroy()
            {
                // Do Nothing
            }
        }), "/test1");

        webAppBundleContext.addFilter(new FilterHolder(new Filter()
        {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException
            {
                ServletContext context = filterConfig.getServletContext();

                assertNotNull(context.getAttribute(OSGI_BUNDLECONTEXT));

                testLatch.countDown();
            }

            @Override
            public void doFilter(ServletRequest arg0, ServletResponse response, FilterChain chain) throws IOException, ServletException
            {
                // Do Nothing
            }

            @Override
            public void destroy()
            {
                // Do Nothing

            }
        }), "/test2", dispatcherSet);

        jettyService.deploy(mockDeployment, webAppBundleContext);

        //Pause since service is multi-threaded.
        //Fail if takes too long.
        if (!testLatch.await(10, TimeUnit.SECONDS))
        {
            fail("Test Was not asserted");
        }
    }
}