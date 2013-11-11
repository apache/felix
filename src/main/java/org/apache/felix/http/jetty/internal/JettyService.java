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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.HttpServiceController;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.servlet.ServletContext;

public final class JettyService extends AbstractLifeCycle.AbstractLifeCycleListener implements BundleTrackerCustomizer, ServiceTrackerCustomizer
{
    /** PID for configuration of the HTTP service. */
    private static final String PID = "org.apache.felix.http";

    /** Endpoint service registration property from RFC 189 */
    private static final String REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    private static final String HEADER_WEB_CONTEXT_PATH = "Web-ContextPath";
    private static final String HEADER_ACTIVATION_POLICY = "Bundle-ActivationPolicy";
    private static final String WEB_SYMBOLIC_NAME = "osgi.web.symbolicname";
    private static final String WEB_VERSION = "osgi.web.version";
    private static final String WEB_CONTEXT_PATH = "osgi.web.contextpath";
    private static final String OSGI_BUNDLE_CONTEXT = "osgi-bundlecontext";

    private final JettyConfig config;
    private final BundleContext context;
    private ServiceRegistration configServiceReg;
    private ExecutorService executor;
    private Server server;
    private ContextHandlerCollection parent;
    private DispatcherServlet dispatcher;
    private EventDispatcher eventDispatcher;
    private final HttpServiceController controller;
    private MBeanServerTracker mbeanServerTracker;
    private BundleTracker bundleTracker;
    private ServiceTracker serviceTracker;
    private EventAdmin eventAdmin;
    private Map<String, Deployment> deployments = new LinkedHashMap<String, Deployment>();

    public JettyService(BundleContext context, DispatcherServlet dispatcher, EventDispatcher eventDispatcher, HttpServiceController controller)
    {
        this.context = context;
        this.config = new JettyConfig(this.context);
        this.dispatcher = dispatcher;
        this.eventDispatcher = eventDispatcher;
        this.controller = controller;
    }

    public void start() throws Exception
    {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, PID);
        this.configServiceReg = this.context.registerService("org.osgi.service.cm.ManagedService", new JettyManagedService(this), props);

        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            public Thread newThread(Runnable runnable)
            {
                Thread t = new Thread(runnable);
                t.setName("Jetty HTTP Service");
                return t;
            }
        });
        this.executor.submit(new JettyOperation()
        {
            @Override
            protected void doExecute() throws Exception
            {
                startJetty();
            }
        });

        this.serviceTracker = new ServiceTracker(this.context, EventAdmin.class.getName(), this);
        this.serviceTracker.open();

        this.bundleTracker = new BundleTracker(this.context, Bundle.ACTIVE | Bundle.STARTING, this);
        this.bundleTracker.open();
    }

    public void stop() throws Exception
    {
        if (this.executor != null && !this.executor.isShutdown())
        {
            this.executor.submit(new JettyOperation()
            {
                @Override
                protected void doExecute() throws Exception
                {
                    stopJetty();
                }
            });
            this.executor.shutdown();
        }
        if (this.configServiceReg != null)
        {
            this.configServiceReg.unregister();
        }
        if (this.bundleTracker != null)
        {
            this.bundleTracker.close();
        }
        if (this.serviceTracker != null)
        {
            this.serviceTracker.close();
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

        if (this.executor != null && !this.executor.isShutdown())
        {
            this.executor.submit(new JettyOperation()
            {
                @Override
                protected void doExecute() throws Exception
                {
                    stopJetty();
                    startJetty();
                }
            });
        }
    }

    private void startJetty()
    {
        try
        {
            initializeJetty();
        }
        catch (Exception e)
        {
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

    private void initializeJetty() throws Exception
    {
        if (this.config.isUseHttp() || this.config.isUseHttps())
        {
            StringBuffer message = new StringBuffer("Started jetty ").append(getJettyVersion()).append(" at port(s)");
            HashLoginService realm = new HashLoginService("OSGi HTTP Service Realm");
            this.server = new Server();
            this.server.addLifeCycleListener(this);

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

            this.parent = new ContextHandlerCollection();

            ServletContextHandler context = new ServletContextHandler(this.parent, this.config.getContextPath(), ServletContextHandler.SESSIONS);

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

            this.server.setHandler(this.parent);
            this.server.start();
            SystemLogger.info(message.toString());
        }
        else
        {
            SystemLogger.info("Jetty not started (HTTP and HTTPS disabled)");
        }

        publishServiceProperties();
    }

    private String getJettyVersion()
    {
        // FELIX-4311: report the real version of Jetty...
        Dictionary headers = this.context.getBundle().getHeaders();
        String version = (String) headers.get("X-Jetty-Version");
        if (version == null)
        {
            version = Server.getVersion();
        }
        return version;
    }

    private void initializeHttp() throws Exception
    {
        Connector connector = this.config.isUseHttpNio() ? new SelectChannelConnector() : new SocketConnector();
        connector.setPort(this.config.getHttpPort());
        configureConnector(connector);
        this.server.addConnector(connector);
    }

    @SuppressWarnings("deprecation")
    private void initializeHttps() throws Exception
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

    private String getEndpoint(final Connector listener, final InetAddress ia)
    {
        if (ia.isLoopbackAddress())
        {
            return null;
        }

        String address = ia.getHostAddress().trim().toLowerCase();
        if (ia instanceof Inet6Address)
        {
            // skip link-local
            if (address.startsWith("fe80:0:0:0:"))
            {
                return null;
            }
            address = "[" + address + "]";
        }
        else if (!(ia instanceof Inet4Address))
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
        if (listener instanceof SslConnector)
        {
            sb.append('s');
            defaultPort = 443;
        }
        sb.append("://");
        sb.append(hostname);
        if (listener.getPort() != defaultPort)
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
        if (connectors != null)
        {
            for (int i = 0; i < connectors.length; i++)
            {
                final Connector connector = connectors[i];

                if (connector.getHost() == null)
                {
                    try
                    {
                        final List<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
                        final List<NetworkInterface> loopBackInterfaces = new ArrayList<NetworkInterface>();
                        final Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                        while (nis.hasMoreElements())
                        {
                            final NetworkInterface ni = nis.nextElement();
                            if (ni.isLoopback())
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
                    if (endpoint != null)
                    {
                        endpoints.add(endpoint);
                    }
                }
            }
        }
        props.put(REG_PROPERTY_ENDPOINTS, endpoints.toArray(new String[endpoints.size()]));
    }

    private Deployment startWebAppBundle(Bundle bundle, String contextPath)
    {
        postEvent(WebEvent.DEPLOYING(bundle, this.context.getBundle()));

        // check existing deployments
        Deployment deployment = this.deployments.get(contextPath);
        if (deployment != null)
        {
            SystemLogger.warning(String.format("Web application bundle %s has context path %s which is already registered", bundle.getSymbolicName(), contextPath), null);
            postEvent(WebEvent.FAILED(bundle, this.context.getBundle(), null, contextPath, deployment.getBundle().getBundleId()));
            return null;
        }

        // check context path belonging to Http Service implementation
        if (contextPath.equals("/"))
        {
            SystemLogger.warning(String.format("Web application bundle %s has context path %s which is reserved", bundle.getSymbolicName(), contextPath), null);
            postEvent(WebEvent.FAILED(bundle, this.context.getBundle(), null, contextPath, this.context.getBundle().getBundleId()));
            return null;
        }

        // check against excluded paths
        for (String path : this.config.getPathExclusions())
        {
            if (contextPath.startsWith(path))
            {
                SystemLogger.warning(String.format("Web application bundle %s has context path %s which clashes with excluded path prefix %s", bundle.getSymbolicName(), contextPath, path), null);
                postEvent(WebEvent.FAILED(bundle, this.context.getBundle(), null, path, null));
                return null;
            }
        }

        deployment = new Deployment(contextPath, bundle);
        this.deployments.put(contextPath, deployment);

        WebAppBundleContext context = new WebAppBundleContext(contextPath, bundle, this.getClass().getClassLoader());
        deploy(deployment, context);
        return deployment;
    }

    public void deploy(final Deployment deployment, final WebAppBundleContext context)
    {
        if (this.executor != null && !this.executor.isShutdown())
        {
            this.executor.submit(new JettyOperation()
            {
                @Override
                protected void doExecute()
                {
                    final Bundle webAppBundle = deployment.getBundle();
                    final Bundle extenderBundle = JettyService.this.context.getBundle();

                    try
                    {
                        JettyService.this.parent.addHandler(context);
                        context.start();

                        Dictionary<String, Object> props = new Hashtable<String, Object>();
                        props.put(WEB_SYMBOLIC_NAME, webAppBundle.getSymbolicName());
                        props.put(WEB_VERSION, webAppBundle.getVersion());
                        props.put(WEB_CONTEXT_PATH, deployment.getContextPath());
                        deployment.setRegistration(webAppBundle.getBundleContext().registerService(ServletContext.class.getName(), context.getServletContext(), props));

                        context.getServletContext().setAttribute(OSGI_BUNDLE_CONTEXT, webAppBundle.getBundleContext());

                        postEvent(WebEvent.DEPLOYED(webAppBundle, extenderBundle));
                    }
                    catch (Exception e)
                    {
                        SystemLogger.error(String.format("Deploying web application bundle %s failed.", webAppBundle.getSymbolicName()), e);
                        postEvent(WebEvent.FAILED(webAppBundle, extenderBundle, e, null, null));
                        deployment.setContext(null);
                    }
                }
            });
            deployment.setContext(context);
        }
    }

    public void undeploy(final Deployment deployment, final WebAppBundleContext context)
    {
        if (this.executor != null && !this.executor.isShutdown())
        {
            this.executor.submit(new JettyOperation()
            {
                @Override
                protected void doExecute()
                {
                    final Bundle webAppBundle = deployment.getBundle();
                    final Bundle extenderBundle = JettyService.this.context.getBundle();

                    try
                    {
                        postEvent(WebEvent.UNDEPLOYING(webAppBundle, extenderBundle));

                        context.getServletContext().removeAttribute(OSGI_BUNDLE_CONTEXT);

                        ServiceRegistration registration = deployment.getRegistration();
                        if (registration != null)
                        {
                            registration.unregister();
                        }
                        deployment.setRegistration(null);
                        context.stop();
                    }
                    catch (Exception e)
                    {
                        SystemLogger.error(String.format("Undeploying web application bundle %s failed.", webAppBundle.getSymbolicName()), e);
                    }
                    finally
                    {
                        postEvent(WebEvent.UNDEPLOYED(webAppBundle, extenderBundle));
                    }
                }
            });
        }
        deployment.setContext(null);
    }

    public Object addingBundle(Bundle bundle, BundleEvent event)
    {
        return detectWebAppBundle(bundle);
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object)
    {
        detectWebAppBundle(bundle);
    }

    private Object detectWebAppBundle(Bundle bundle)
    {
        if (bundle.getState() == Bundle.ACTIVE || (bundle.getState() == Bundle.STARTING && "Lazy".equals(bundle.getHeaders().get(HEADER_ACTIVATION_POLICY))))
        {

            String contextPath = (String) bundle.getHeaders().get(HEADER_WEB_CONTEXT_PATH);
            if (contextPath != null)
            {
                return startWebAppBundle(bundle, contextPath);
            }
        }
        return null;
    }

    public void removedBundle(Bundle bundle, BundleEvent event, Object object)
    {
        String contextPath = (String) bundle.getHeaders().get(HEADER_WEB_CONTEXT_PATH);
        if (contextPath == null)
        {
            return;
        }

        Deployment deployment = this.deployments.remove(contextPath);
        if (deployment != null && deployment.getContext() != null)
        {
            // remove registration, since bundle is already stopping
            deployment.setRegistration(null);
            undeploy(deployment, deployment.getContext());
        }
    }

    public Object addingService(ServiceReference reference)
    {
        Object service = this.context.getService(reference);
        modifiedService(reference, service);
        return service;
    }

    public void modifiedService(ServiceReference reference, Object service)
    {
        this.eventAdmin = (EventAdmin) service;
    }

    public void removedService(ServiceReference reference, Object service)
    {
        this.context.ungetService(reference);
        this.eventAdmin = null;
    }

    private void postEvent(Event event)
    {
        if (this.eventAdmin != null)
        {
            this.eventAdmin.postEvent(event);
        }
    }

    public void lifeCycleStarted(LifeCycle event)
    {
        for (Deployment deployment : this.deployments.values())
        {
            if (deployment.getContext() == null)
            {
                postEvent(WebEvent.DEPLOYING(deployment.getBundle(), this.context.getBundle()));
                WebAppBundleContext context = new WebAppBundleContext(deployment.getContextPath(), deployment.getBundle(), this.getClass().getClassLoader());
                deploy(deployment, context);
            }
        }
    }

    public void lifeCycleStopping(LifeCycle event)
    {
        for (Deployment deployment : this.deployments.values())
        {
            if (deployment.getContext() != null)
            {
                undeploy(deployment, deployment.getContext());
            }
        }
    }

    /**
     * A deployment represents a web application bundle that may or may not be deployed.
     */
    static class Deployment
    {
        private String contextPath;
        private Bundle bundle;
        private WebAppBundleContext context;
        private ServiceRegistration registration;

        public Deployment(String contextPath, Bundle bundle)
        {
            this.contextPath = contextPath;
            this.bundle = bundle;
        }

        public Bundle getBundle()
        {
            return this.bundle;
        }

        public String getContextPath()
        {
            return this.contextPath;
        }

        public WebAppBundleContext getContext()
        {
            return this.context;
        }

        public void setContext(WebAppBundleContext context)
        {
            this.context = context;
        }

        public ServiceRegistration getRegistration()
        {
            return this.registration;
        }

        public void setRegistration(ServiceRegistration registration)
        {
            this.registration = registration;
        }
    }

    /**
     * A Jetty operation is executed with the context class loader set to this class's
     * class loader.
     */
    abstract static class JettyOperation implements Callable<Void>
    {
        public Void call() throws Exception
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            try
            {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                doExecute();
                return null;
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        protected abstract void doExecute() throws Exception;
    }
}
