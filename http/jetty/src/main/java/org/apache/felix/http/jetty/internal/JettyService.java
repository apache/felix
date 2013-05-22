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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.HttpServiceController;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public final class JettyService
    implements Runnable
{
    /** PID for configuration of the HTTP service. */
    private static final String PID = "org.apache.felix.http";

    /** Endpoint service registration property from RFC 189 */
    private static final String REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    private final JettyConfig config;
    private final BundleContext context;
    private boolean running;
    private Thread thread;
    private ServiceRegistration configServiceReg;
    private Server server;
    private DispatcherServlet dispatcher;
    private EventDispatcher eventDispatcher;
    private final HttpServiceController controller;
    private MBeanServerTracker mbeanServerTracker;

    public JettyService(BundleContext context, DispatcherServlet dispatcher, EventDispatcher eventDispatcher,
        HttpServiceController controller)
    {
        this.context = context;
        this.config = new JettyConfig(this.context);
        this.dispatcher = dispatcher;
        this.eventDispatcher = eventDispatcher;
        this.controller = controller;
    }

    public void start()
        throws Exception
    {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, PID);
        this.configServiceReg = this.context.registerService("org.osgi.service.cm.ManagedService",
            new JettyManagedService(this), props);

        this.thread = new Thread(this, "Jetty HTTP Service");
        this.thread.start();
    }

    public void stop()
        throws Exception
    {
        if (this.configServiceReg != null) {
            this.configServiceReg.unregister();
        }

        this.running = false;
        this.thread.interrupt();

        try {
            this.thread.join(3000);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    private void publishServiceProperties()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        this.config.setServiceProperties(props);
        this.addEndpointProperties(props, null);
        this.controller.setProperties(props);
    }

    public void updated(Dictionary props)
    {
        this.config.update(props);

        if (this.running && (this.thread != null)) {
            this.thread.interrupt();
        }
    }

    private void startJetty()
    {
        try {
            initializeJetty();
        } catch (Exception e) {
            SystemLogger.error("Exception while initializing Jetty.", e);
        }
    }

    private void stopJetty()
    {
        if (this.server != null)
        {
            try
            {
                this.server.stop();
                this.server = null;
            }
            catch (Exception e)
            {
                SystemLogger.error("Exception while stopping Jetty.", e);
            }

            if (this.mbeanServerTracker != null)
            {
                this.mbeanServerTracker.close();
                this.mbeanServerTracker = null;
            }
        }
    }

    private void initializeJetty()
        throws Exception
    {
        if (this.config.isUseHttp() || this.config.isUseHttps())
        {
            StringBuffer message = new StringBuffer("Started jetty ").append(Server.getVersion()).append(" at port(s)");
            HashLoginService realm = new HashLoginService("OSGi HTTP Service Realm");
            this.server = new Server();

            // HTTP/1.1 requires Date header if possible (it is)
            this.server.setSendDateHeader(true);

            this.server.addBean(realm);

            if (this.config.isUseHttp())
            {
                initializeHttp();
                message.append(" HTTP:").append(this.config.getHttpPort());
            }

            if (this.config.isUseHttps())
            {
                initializeHttps();
                message.append(" HTTPS:").append(this.config.getHttpsPort());
            }

            ServletContextHandler context = new ServletContextHandler(this.server, this.config.getContextPath(), ServletContextHandler.SESSIONS);

            message.append(" on context path ").append(this.config.getContextPath());
            configureSessionManager(context);
            context.addEventListener(eventDispatcher);
            context.getSessionHandler().addEventListener(eventDispatcher);
            context.addServlet(new ServletHolder(this.dispatcher), "/*");

            if (this.config.isRegisterMBeans())
            {
                this.mbeanServerTracker = new MBeanServerTracker(this.context, this.server);
                this.mbeanServerTracker.open();
                context.addBean(new StatisticsHandler());
            }

            this.server.start();
            SystemLogger.info(message.toString());
        }
        else
        {
            SystemLogger.info("Jetty not started (HTTP and HTTPS disabled)");
        }

        publishServiceProperties();
    }

    private void initializeHttp()
        throws Exception
    {
        Connector connector = this.config.isUseHttpNio()
                ? new SelectChannelConnector()
                : new SocketConnector();
        connector.setPort(this.config.getHttpPort());
        configureConnector(connector);
        this.server.addConnector(connector);
    }

    private void initializeHttps()
        throws Exception
    {
        // this massive code duplication is caused by the SslSelectChannelConnector
        // and the SslSocketConnector not have a common API to setup security
        // stuff
        Connector connector;
        if (this.config.isUseHttpsNio())
        {
            SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();

            if (this.config.getKeystore() != null)
            {
                sslConnector.setKeystore(this.config.getKeystore());
            }

            if (this.config.getPassword() != null)
            {
                System.setProperty(SslSelectChannelConnector.PASSWORD_PROPERTY, this.config.getPassword());
                sslConnector.setPassword(this.config.getPassword());
            }

            if (this.config.getKeyPassword() != null)
            {
                System.setProperty(SslSelectChannelConnector.KEYPASSWORD_PROPERTY, this.config.getKeyPassword());
                sslConnector.setKeyPassword(this.config.getKeyPassword());
            }

            if (this.config.getTruststore() != null)
            {
                sslConnector.setTruststore(this.config.getTruststore());
            }

            if (this.config.getTrustPassword() != null)
            {
                sslConnector.setTrustPassword(this.config.getTrustPassword());
            }

            if ("wants".equals(this.config.getClientcert()))
            {
                sslConnector.setWantClientAuth(true);
            }
            else if ("needs".equals(this.config.getClientcert()))
            {
                sslConnector.setNeedClientAuth(true);
            }

            connector = sslConnector;
        }
        else
        {
            SslSocketConnector sslConnector = new SslSocketConnector();

            if (this.config.getKeystore() != null)
            {
                sslConnector.setKeystore(this.config.getKeystore());
            }

            if (this.config.getPassword() != null)
            {
                System.setProperty(SslSelectChannelConnector.PASSWORD_PROPERTY, this.config.getPassword());
                sslConnector.setPassword(this.config.getPassword());
            }

            if (this.config.getKeyPassword() != null)
            {
                System.setProperty(SslSelectChannelConnector.KEYPASSWORD_PROPERTY, this.config.getKeyPassword());
                sslConnector.setKeyPassword(this.config.getKeyPassword());
            }

            if (this.config.getTruststore() != null)
            {
                sslConnector.setTruststore(this.config.getTruststore());
            }

            if (this.config.getTrustPassword() != null)
            {
                sslConnector.setTrustPassword(this.config.getTrustPassword());
            }

            if ("wants".equals(this.config.getClientcert()))
            {
                sslConnector.setWantClientAuth(true);
            }
            else if ("needs".equals(this.config.getClientcert()))
            {
                sslConnector.setNeedClientAuth(true);
            }

            connector = sslConnector;
        }

        connector.setPort(this.config.getHttpsPort());
        configureConnector(connector);

        this.server.addConnector(connector);
    }

    private void configureConnector(final Connector connector)
    {
        connector.setMaxIdleTime(this.config.getHttpTimeout());
        connector.setRequestBufferSize(this.config.getRequestBufferSize());
        connector.setResponseBufferSize(this.config.getResponseBufferSize());
        connector.setHost(this.config.getHost());
        connector.setStatsOn(this.config.isRegisterMBeans());

        // connector.setLowResourceMaxIdleTime(ms);
        // connector.setRequestBufferSize(requestBufferSize);
        // connector.setResponseBufferSize(responseBufferSize);
    }

    private void configureSessionManager(final ServletContextHandler context)
    {
        final SessionManager manager = context.getSessionHandler().getSessionManager();

        manager.setMaxInactiveInterval(this.config.getSessionTimeout() * 60);

        manager.setSessionCookie(this.config.getProperty(SessionManager.__SessionCookieProperty, SessionManager.__DefaultSessionCookie));
        manager.setSessionIdPathParameterName(this.config.getProperty(SessionManager.__SessionIdPathParameterNameProperty, SessionManager.__DefaultSessionIdPathParameterName));
        manager.setSessionDomain(this.config.getProperty(SessionManager.__SessionDomainProperty, SessionManager.__DefaultSessionDomain));
        manager.setSessionPath(this.config.getProperty(SessionManager.__SessionPathProperty, context.getContextPath()));
        manager.setMaxCookieAge(this.config.getIntProperty(SessionManager.__MaxAgeProperty, -1));
    }

    public void run()
    {
        this.running = true;
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        while (this.running) {
            startJetty();

            synchronized (this)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    // we will definitely be interrupted
                }
            }

            stopJetty();
        }
    }


    private String getEndpoint(final Connector listener, final InetAddress ia)
    {
        if (ia.isLoopbackAddress())
        {
            return null;
        }

        String address = ia.getHostAddress().trim().toLowerCase();
        if ( ia instanceof Inet6Address )
        {
            // skip link-local
            if ( address.startsWith("fe80:0:0:0:") )
            {
                return null;
            }
            address = "[" + address + "]";
        }
        else if ( ! ( ia instanceof Inet4Address ) )
        {
            return null;
        }

        return getEndpoint(listener, address);
    }

    private String getEndpoint(final Connector listener, final String hostname)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("http");
        int defaultPort = 80;
        if ( listener instanceof SslConnector )
        {
            sb.append('s');
            defaultPort = 443;
        }
        sb.append("://");
        sb.append(hostname);
        if ( listener.getPort() != defaultPort )
        {
            sb.append(':');
            sb.append(String.valueOf(listener.getPort()));
        }
        sb.append(config.getContextPath());

        return sb.toString();
    }

    private List<String> getEndpoints(final Connector connector, final List<NetworkInterface> interfaces)
    {
        final List<String> endpoints = new ArrayList<String>();
        for (final NetworkInterface ni : interfaces)
        {
            final Enumeration<InetAddress> ias = ni.getInetAddresses();
            while (ias.hasMoreElements())
            {
                final InetAddress ia = ias.nextElement();
                final String endpoint = this.getEndpoint(connector, ia);
                if (endpoint != null)
                {
                    endpoints.add(endpoint);
                }
            }
        }
        return endpoints;
    }

    private void addEndpointProperties(final Hashtable<String, Object> props, Object container)
    {
        final List<String> endpoints = new ArrayList<String>();

        final Connector[] connectors = this.server.getConnectors();
        if ( connectors != null )
        {
            for(int i=0 ; i < connectors.length; i++)
            {
                final Connector connector = connectors[i];

                if ( connector.getHost() == null )
                {
                    try
                    {
                        final List<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
                        final List<NetworkInterface> loopBackInterfaces = new ArrayList<NetworkInterface>();
                        final Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                        while ( nis.hasMoreElements() )
                        {
                            final NetworkInterface ni = nis.nextElement();
                            if ( ni.isLoopback() )
                            {
                                loopBackInterfaces.add(ni);
                            }
                            else
                            {
                                interfaces.add(ni);
                            }
                        }

                        // only add loop back endpoints to the endpoint property if no other endpoint is available.
                        if (!interfaces.isEmpty())
                        {
                            endpoints.addAll(getEndpoints(connector, interfaces));
                        }
                        else
                        {
                            endpoints.addAll(getEndpoints(connector, loopBackInterfaces));
                        }
                    }
                    catch (final SocketException se)
                    {
                        // we ignore this
                    }
                }
                else
                {
                    final String endpoint = this.getEndpoint(connector, connector.getHost());
                    if ( endpoint != null )
                    {
                        endpoints.add(endpoint);
                    }
                }
            }
        }
        props.put(REG_PROPERTY_ENDPOINTS, endpoints.toArray(new String[endpoints.size()]));
    }
}
