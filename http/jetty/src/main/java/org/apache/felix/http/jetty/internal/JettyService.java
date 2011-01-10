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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.*;
import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.HttpServiceController;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import java.util.Properties;
import java.util.Dictionary;
import java.util.Hashtable;

public final class JettyService
    implements Runnable
{
    /** PID for configuration of the HTTP service. */
    private static final String PID = "org.apache.felix.http";

    private final JettyConfig config;
    private final BundleContext context;
    private boolean running;
    private Thread thread;
    private ServiceRegistration configServiceReg;
    private Server server;
    private DispatcherServlet dispatcher;
    private EventDispatcher eventDispatcher;
    private final HttpServiceController controller;

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
        JettyLogger.init();

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
        }
    }

    private void initializeJetty()
        throws Exception
    {
        if (this.config.isUseHttp() || this.config.isUseHttps())
        {
            StringBuffer message = new StringBuffer("Started jetty ").append(Server.getVersion()).append(" at port(s)");
            HashUserRealm realm = new HashUserRealm("OSGi HTTP Service Realm");
            this.server = new Server();
            this.server.addUserRealm(realm);

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

            Context context = new Context(this.server, "/", Context.SESSIONS);
            context.addEventListener(eventDispatcher);
            context.getSessionHandler().addEventListener(eventDispatcher);
            context.addServlet(new ServletHolder(this.dispatcher), "/*");

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
        connector.setMaxIdleTime(60000);
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
        connector.setMaxIdleTime(60000);

        this.server.addConnector(connector);
    }

    public void run()
    {
        this.running = true;
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        while (this.running) {
            startJetty();

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // we will definitely be interrupted
                }
            }

            stopJetty();
        }
    }
}
