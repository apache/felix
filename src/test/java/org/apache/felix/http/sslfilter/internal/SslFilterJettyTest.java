/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.sslfilter.internal;

import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_LOCATION;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_PROTO;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_SSL;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTP;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SslFilterJettyTest
{
    private InetSocketAddress serverAddress;

    private Server server;
    private ServletContextHandler context;
    private boolean originalFollowRedirects;

    @Before
    public void setupServer() throws Exception
    {
        this.serverAddress = new InetSocketAddress("localhost", 8080);

        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.context.setContextPath("/");
        this.context.addFilter(new FilterHolder(new SslFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));

        this.server = new Server(this.serverAddress);
        this.server.setHandler(this.context);

        this.originalFollowRedirects = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
    }

    @After
    public void tearDown() throws Exception
    {
        HttpURLConnection.setFollowRedirects(this.originalFollowRedirects);

        if (this.server != null)
        {
            this.server.stop();
        }
    }

    @Test
    public void testSslFilterWithRelativeRedirectURL() throws Exception
    {
        String servletPath = "/test";
        String redirectPath = "/foo";

        this.context.addServlet(new ServletHolder(new RedirectServlet(redirectPath)), servletPath);
        this.server.start();

        HttpURLConnection conn = openConnection(createURL(servletPath));

        assertEquals(302, conn.getResponseCode());
        String location = conn.getHeaderField(HDR_LOCATION);
        assertTrue(location, location.startsWith(HTTPS));
    }

    @Test
    public void testSslFilterWithAbsoluteRedirectURL() throws Exception
    {
        String servletPath = "/test";
        String redirectPath = String.format("http://%s:%d/foo", this.serverAddress.getHostName(), this.serverAddress.getPort());

        this.context.addServlet(new ServletHolder(new RedirectServlet(redirectPath)), servletPath);
        this.server.start();

        HttpURLConnection conn = openConnection(createURL(servletPath));

        assertEquals(302, conn.getResponseCode());

        String location = conn.getHeaderField(HDR_LOCATION);
        assertTrue(location, location.startsWith(HTTP));
    }

    @Test
    public void testSslFilterWithAbsoluteRedirectURLWithoutScheme() throws Exception
    {
        String servletPath = "/test";
        String redirectPath = String.format("//%s:%d/foo", this.serverAddress.getHostName(), this.serverAddress.getPort());

        this.context.addServlet(new ServletHolder(new RedirectServlet(redirectPath)), servletPath);
        this.server.start();

        HttpURLConnection conn = openConnection(createURL(servletPath));

        assertEquals(302, conn.getResponseCode());

        String location = conn.getHeaderField(HDR_LOCATION);
        assertTrue(location, location.startsWith(HTTPS));
    }

    @Test
    public void testSslFilterWithAbsoluteRedirectURLWithHttpsScheme() throws Exception
    {
        String servletPath = "/test";
        String redirectPath = String.format("https://%s:%d/foo", this.serverAddress.getHostName(), this.serverAddress.getPort());

        this.context.addServlet(new ServletHolder(new RedirectServlet(redirectPath)), servletPath);
        this.server.start();

        HttpURLConnection conn = openConnection(createURL(servletPath));

        assertEquals(302, conn.getResponseCode());

        String location = conn.getHeaderField(HDR_LOCATION);
        assertTrue(location, location.startsWith(HTTPS));
    }

    private HttpURLConnection openConnection(URL url) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty(HDR_X_FORWARDED_PROTO, HTTPS);
        conn.setRequestProperty(HDR_X_FORWARDED_SSL, "on");
        conn.connect();
        return conn;
    }

    private URL createURL(String path) throws MalformedURLException
    {
        return new URL(HTTP, this.serverAddress.getHostName(), this.serverAddress.getPort(), path);
    }

    private static class RedirectServlet extends HttpServlet
    {
        private final String redirectPath;

        private RedirectServlet(String redirectPath)
        {
            this.redirectPath = redirectPath;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.sendRedirect(redirectPath);
            assertEquals(HTTPS, req.getScheme());
        }
    }
}