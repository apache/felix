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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.api.ExtHttpService;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base class for integration tests.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class BaseIntegrationTest
{
    protected static class TestFilter implements Filter
    {
        private final CountDownLatch m_initLatch;
        private final CountDownLatch m_destroyLatch;

        public TestFilter()
        {
            this(null, null);
        }

        public TestFilter(CountDownLatch initLatch, CountDownLatch destroyLatch)
        {
            m_initLatch = initLatch;
            m_destroyLatch = destroyLatch;
        }

        @Override
        public void destroy()
        {
            if (m_destroyLatch != null)
            {
                m_destroyLatch.countDown();
            }
        }

        @Override
        public final void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
        {
            filter((HttpServletRequest) req, (HttpServletResponse) resp, chain);
        }

        @Override
        public void init(FilterConfig config) throws ServletException
        {
            if (m_initLatch != null)
            {
                m_initLatch.countDown();
            }
        }

        protected void filter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException
        {
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected static class TestServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        private final CountDownLatch m_initLatch;
        private final CountDownLatch m_destroyLatch;

        public TestServlet()
        {
            this(null, null);
        }

        public TestServlet(CountDownLatch initLatch, CountDownLatch destroyLatch)
        {
            m_initLatch = initLatch;
            m_destroyLatch = destroyLatch;
        }

        @Override
        public void destroy()
        {
            super.destroy();
            if (m_destroyLatch != null)
            {
                m_destroyLatch.countDown();
            }
        }

        @Override
        public void init() throws ServletException
        {
            super.init();
            if (m_initLatch != null)
            {
                m_initLatch.countDown();
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected static final int DEFAULT_TIMEOUT = 10000;

    private static final String ORG_APACHE_FELIX_HTTP_JETTY = "org.apache.felix.http.jetty";

    protected static void assertContent(int expectedRC, String expected, URL url) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        int rc = conn.getResponseCode();
        assertEquals("Unexpected response code,", expectedRC, rc);

        if (rc >= 200 && rc < 500)
        {
            InputStream is = null;
            try
            {
                is = conn.getInputStream();
                assertEquals(expected, slurpAsString(is));
            }
            finally
            {
                close(is);
                conn.disconnect();
            }
        }
        else
        {
            InputStream is = null;
            try
            {
                is = conn.getErrorStream();
                assertEquals(expected, slurpAsString(is));
            }
            finally
            {
                close(is);
                conn.disconnect();
            }
        }
    }

    protected static void assertContent(String expected, URL url) throws IOException
    {
        assertContent(200, expected, url);
    }

    protected static void assertResponseCode(int expected, URL url) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try
        {
            assertEquals(expected, conn.getResponseCode());
        }
        finally
        {
            conn.disconnect();
        }
    }

    protected static void close(Closeable resource)
    {
        if (resource != null)
        {
            try
            {
                resource.close();
            }
            catch (IOException e)
            {
                // Ignore...
            }
        }
    }

    protected static Dictionary<String, ?> createDictionary(Object... entries)
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        for (int i = 0; i < entries.length; i += 2)
        {
            String key = (String) entries[i];
            Object value = entries[i + 1];
            props.put(key, value);
        }
        return props;
    }

    protected static URL createURL(String path)
    {
        if (path == null)
        {
            path = "";
        }
        while (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        int port = Integer.getInteger("org.osgi.service.http.port", 8080);
        try
        {
            return new URL(String.format("http://localhost:%d/%s", port, path));
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected static String slurpAsString(InputStream is) throws IOException
    {
        // See <weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html>
        Scanner scanner = new Scanner(is, "UTF-8");
        try
        {
            scanner.useDelimiter("\\A");

            return scanner.hasNext() ? scanner.next() : null;
        }
        finally
        {
            try
            {
                scanner.close();
            }
            catch (Exception e)
            {
                // Ignore...
            }
        }
    }

    @Inject
    protected volatile BundleContext m_context;

    @Configuration
    public Option[] config()
    {
        final String localRepo = System.getProperty("maven.repo.local", "");

        return options(
            when( localRepo.length() > 0 ).useOptions(
                    systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
            ),
//            CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787"),

            mavenBundle("org.slf4j", "slf4j-api", "1.7.5"),
            mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.5"),
            mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.5"),

            mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.0"),
            mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"),

            mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", System.getProperty("http.servlet.api.version")).startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", ORG_APACHE_FELIX_HTTP_JETTY, System.getProperty("http.jetty.version")).startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.http.whiteboard", "3.0.0").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.8.8"),

            mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.3.2").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.3.4").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.mockito", "mockito-all", "1.10.19").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.objenesis", "objenesis", "2.1").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("com.googlecode.json-simple", "json-simple", "1.1.1").startLevel(START_LEVEL_SYSTEM_BUNDLES),

            junitBundles(),
            frameworkStartLevel(START_LEVEL_TEST_BUNDLE));
    }

    private final Map<String, ServiceTracker<?, ?>> trackers = new HashMap<String, ServiceTracker<?, ?>>();

    @Before
    public void setUp() throws Exception
    {
        assertNotNull("No bundle context?!", m_context);
    }

    @After
    public void tearDown() throws Exception
    {
        synchronized ( trackers )
        {
            for(final Map.Entry<String, ServiceTracker<?, ?>> entry : trackers.entrySet())
            {
                entry.getValue().close();
            }
            trackers.clear();
        }
        Bundle bundle = getHttpJettyBundle();
        // Restart the HTTP-service to clean all registrations...
        if (bundle.getState() == Bundle.ACTIVE)
        {
            bundle.stop();
            bundle.start();
        }
    }

    /**
     * Waits for a service to become available in certain time interval.
     * @param serviceName
     * @return
     * @throws Exception
     */
    protected <T> T awaitService(String serviceName) throws Exception
    {
        ServiceTracker<T, T> tracker = null;
        tracker = getTracker(serviceName);
        return tracker.waitForService(DEFAULT_TIMEOUT);
    }

    /**
     * Return an array of {@code ServiceReference}s for all services for the
     * given serviceName
     * @param serviceName
     * @return Array of {@code ServiceReference}s or {@code null} if no services
     *         are being tracked.
     */
    protected <T> ServiceReference<T>[] getServiceReferences(String serviceName)
    {
        ServiceTracker<T, T> tracker = getTracker(serviceName);
        return tracker.getServiceReferences();
    }

    private <T> ServiceTracker<T, T> getTracker(String serviceName)
    {
        synchronized ( this.trackers )
        {
            ServiceTracker<?, ?> tracker = trackers.get(serviceName);
            if ( tracker == null )
            {
                tracker = new ServiceTracker<T, T>(m_context, serviceName, null);
                trackers.put(serviceName, tracker);
                tracker.open();
            }
            return (ServiceTracker<T, T>) tracker;
        }
    }

    protected void configureHttpService(Dictionary<String, ?> props) throws Exception
    {
        final String pid = "org.apache.felix.http";

        final Collection<ServiceReference<ManagedService>> serviceRefs = m_context.getServiceReferences(ManagedService.class, String.format("(%s=%s)", Constants.SERVICE_PID, pid));
        assertNotNull("Unable to obtain managed configuration for " + pid, serviceRefs);
        assertFalse("Unable to obtain managed configuration for " + pid, serviceRefs.isEmpty());

        for (final ServiceReference<ManagedService> serviceRef : serviceRefs)
        {
            ManagedService service = m_context.getService(serviceRef);
            try
            {
                service.updated(props);
            }
            catch (ConfigurationException ex)
            {
                fail("Invalid configuration provisioned: " + ex.getMessage());
            }
            finally
            {
                m_context.ungetService(serviceRef);
            }
        }
    }

    /**
     * @param bsn
     * @return
     */
    protected Bundle findBundle(String bsn)
    {
        for (Bundle bundle : m_context.getBundles())
        {
            if (bsn.equals(bundle.getSymbolicName()))
            {
                return bundle;
            }
        }
        return null;
    }

    protected ExtHttpService getExtHttpService()
    {
        return getService(ExtHttpService.class.getName());
    }

    protected Bundle getHttpJettyBundle()
    {
        Bundle b = findBundle(ORG_APACHE_FELIX_HTTP_JETTY);
        assertNotNull("Filestore bundle not found?!", b);
        return b;
    }

    protected HttpService getHttpService()
    {
        return getService(HttpService.class.getName());
    }

    /**
     * Obtains a service without waiting for it to become available.
     * @param serviceName
     * @return
     */
    protected <T> T getService(final String serviceName)
    {
        ServiceTracker<?, ?> tracker = null;
        synchronized ( this.trackers )
        {
            tracker = trackers.get(serviceName);
            if ( tracker == null )
            {
                tracker = new ServiceTracker(m_context, serviceName, null);
                trackers.put(serviceName, tracker);
                tracker.open();
            }
        }
        return (T) tracker.getService();
    }

    protected void register(String pattern, Filter filter) throws ServletException, NamespaceException
    {
        register(pattern, filter, null);
    }

    protected void register(String pattern, Filter servlet, HttpContext context) throws ServletException, NamespaceException
    {
        getExtHttpService().registerFilter(servlet, pattern, null, 0, context);
    }

    protected void register(String alias, Servlet servlet) throws ServletException, NamespaceException
    {
        register(alias, servlet, null);
    }

    protected void register(String alias, Servlet servlet, HttpContext context) throws ServletException, NamespaceException
    {
        getHttpService().registerServlet(alias, servlet, null, context);
    }

    protected void register(String alias, String name) throws ServletException, NamespaceException
    {
        register(alias, name, null);
    }

    protected void register(String alias, String name, HttpContext context) throws ServletException, NamespaceException
    {
        getExtHttpService().registerResources(alias, name, context);
    }

    protected void unregister(Filter filter) throws ServletException, NamespaceException
    {
        getExtHttpService().unregisterFilter(filter);
    }

    protected void unregister(Servlet servlet) throws ServletException, NamespaceException
    {
        getExtHttpService().unregisterServlet(servlet);
    }

    protected void unregister(String alias) throws ServletException, NamespaceException
    {
        getExtHttpService().unregister(alias);
    }
}
