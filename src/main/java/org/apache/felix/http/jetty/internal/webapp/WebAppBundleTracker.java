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
package org.apache.felix.http.jetty.internal.webapp;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.jetty.internal.JettyConfig;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class WebAppBundleTracker extends AbstractLifeCycle.AbstractLifeCycleListener
{
    private static final String HEADER_WEB_CONTEXT_PATH = "Web-ContextPath";

    private static final String HEADER_ACTIVATION_POLICY = "Bundle-ActivationPolicy";

    private static final String POLICY_LAZY = "Lazy";

    private static final String OSGI_BUNDLE_CONTEXT = "osgi-bundlecontext";

    private static final String WEB_SYMBOLIC_NAME = "osgi.web.symbolicname";
    private static final String WEB_VERSION = "osgi.web.version";
    private static final String WEB_CONTEXT_PATH = "osgi.web.contextpath";

    private final Map<String, Deployment> deployments = new LinkedHashMap<>();

    private final BundleContext context;

    private final ExecutorService executor;

    private final JettyConfig config;

    private volatile BundleTracker<Deployment> bundleTracker;

    private volatile ServiceTracker<Object, Object> eventAdmintTracker;

    private volatile Object eventAdmin;

    private volatile ContextHandlerCollection parent;

    public WebAppBundleTracker(final BundleContext bundleContext, final JettyConfig config) {
        this.context = bundleContext;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setName("Jetty HTTP Service");
                return t;
            }
        });
    }

    public void start(final ContextHandlerCollection parent) throws Exception
    {
        this.parent = parent;
        // we use the class name as a String to make the dependency on event admin
        // optional
        this.eventAdmintTracker = new ServiceTracker<>(this.context, "org.osgi.service.event.EventAdmin",
                new ServiceTrackerCustomizer<Object, Object>() {
                    @Override
                    public Object addingService(final ServiceReference<Object> reference) {
                        final Object service = context.getService(reference);
                        eventAdmin = service;
                        return service;
                    }

                    @Override
                    public void modifiedService(final ServiceReference<Object> reference, final Object service) {
                        // nothing to do
                    }

                    @Override
                    public void removedService(final ServiceReference<Object> reference, final Object service) {
                        eventAdmin = null;
                        context.ungetService(reference);
                    }
                });
        this.eventAdmintTracker.open();

        this.bundleTracker = new BundleTracker<>(this.context, Bundle.ACTIVE | Bundle.STARTING,
                new BundleTrackerCustomizer<Deployment>() {

            @Override
            public Deployment addingBundle(Bundle bundle, BundleEvent event)
            {
                return detectWebAppBundle(bundle);
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, Deployment object)
            {
                detectWebAppBundle(bundle);
            }

            private Deployment detectWebAppBundle(Bundle bundle)
            {
                        if (bundle.getState() == Bundle.ACTIVE || (bundle.getState() == Bundle.STARTING
                                && POLICY_LAZY.equals(bundle.getHeaders().get(HEADER_ACTIVATION_POLICY))))
                {

                    String contextPath = bundle.getHeaders().get(HEADER_WEB_CONTEXT_PATH);
                    if (contextPath != null)
                    {
                        return startWebAppBundle(bundle, contextPath);
                    }
                }
                return null;
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Deployment object)
            {
                String contextPath = bundle.getHeaders().get(HEADER_WEB_CONTEXT_PATH);
                if (contextPath == null)
                {
                    return;
                }

                Deployment deployment = deployments.remove(contextPath);
                if (deployment != null && deployment.getContext() != null)
                {
                    // remove registration, since bundle is already stopping
                    deployment.setRegistration(null);
                    undeploy(deployment, deployment.getContext());
                }
            }

                });
        this.bundleTracker.open();
    }

    public void stop() throws Exception
    {
        if (this.bundleTracker != null)
        {
            this.bundleTracker.close();
            this.bundleTracker = null;
        }
        if (this.eventAdmintTracker != null) {
            this.eventAdmintTracker.close();
            this.eventAdmintTracker = null;
        }
        if (isExecutorServiceAvailable()) {
            this.executor.shutdown();
            // FELIX-4423: make sure to await the termination of the executor...
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        this.parent = null;
    }

    private Deployment startWebAppBundle(Bundle bundle, String contextPath)
    {
        postEvent(WebEvent.TOPIC_DEPLOYING, bundle, this.context.getBundle(), null, null, null);

        // check existing deployments
        Deployment deployment = this.deployments.get(contextPath);
        if (deployment != null)
        {
            SystemLogger.warning(String.format("Web application bundle %s has context path %s which is already registered", bundle.getSymbolicName(), contextPath), null);
            postEvent(WebEvent.TOPIC_FAILED, bundle, this.context.getBundle(), null, contextPath, deployment.getBundle().getBundleId());
            return null;
        }

        // check context path belonging to Http Service implementation
        if (contextPath.equals("/"))
        {
            SystemLogger.warning(String.format("Web application bundle %s has context path %s which is reserved", bundle.getSymbolicName(), contextPath), null);
            postEvent(WebEvent.TOPIC_FAILED, bundle, this.context.getBundle(), null, contextPath, this.context.getBundle().getBundleId());
            return null;
        }

        // check against excluded paths
        for (String path : this.config.getPathExclusions())
        {
            if (contextPath.startsWith(path))
            {
                SystemLogger.warning(String.format("Web application bundle %s has context path %s which clashes with excluded path prefix %s", bundle.getSymbolicName(), contextPath, path), null);
                postEvent(WebEvent.TOPIC_FAILED, bundle, this.context.getBundle(), null, path, null);
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
        if (!isExecutorServiceAvailable())
        {
            // Shutting down...?
            return;
        }

        this.executor.submit(new JettyOperation()
        {
            @Override
            protected void doExecute()
            {
                final Bundle webAppBundle = deployment.getBundle();
                final Bundle extenderBundle = WebAppBundleTracker.this.context.getBundle();

                try
                {
                    context.getServletContext().setAttribute(OSGI_BUNDLE_CONTEXT, webAppBundle.getBundleContext());

                    WebAppBundleTracker.this.parent.addHandler(context);
                    context.start();

                    Dictionary<String, Object> props = new Hashtable<>();
                    props.put(WEB_SYMBOLIC_NAME, webAppBundle.getSymbolicName());
                    props.put(WEB_VERSION, webAppBundle.getVersion());
                    props.put(WEB_CONTEXT_PATH, deployment.getContextPath());
                    deployment.setRegistration(webAppBundle.getBundleContext().registerService(ServletContext.class, context.getServletContext(), props));

                    postEvent(WebEvent.TOPIC_DEPLOYED, webAppBundle, extenderBundle, null, null, null);
                }
                catch (Exception e)
                {
                    SystemLogger.error(String.format("Deploying web application bundle %s failed.", webAppBundle.getSymbolicName()), e);
                    postEvent(WebEvent.TOPIC_FAILED, webAppBundle, extenderBundle, e, null, null);
                    deployment.setContext(null);
                }
            }
        });
        deployment.setContext(context);
    }

    public void undeploy(final Deployment deployment, final WebAppBundleContext context)
    {
        if (!isExecutorServiceAvailable())
        {
            // Already stopped...?
            return;
        }

        this.executor.submit(new JettyOperation()
        {
            @Override
            protected void doExecute()
            {
                final Bundle webAppBundle = deployment.getBundle();
                final Bundle extenderBundle = WebAppBundleTracker.this.context.getBundle();

                try
                {
                    postEvent(WebEvent.TOPIC_UNDEPLOYING, webAppBundle, extenderBundle, null, null, null);

                    context.getServletContext().removeAttribute(OSGI_BUNDLE_CONTEXT);

                    ServiceRegistration<ServletContext> registration = deployment.getRegistration();
                    if (registration != null)
                    {
                        registration.unregister();
                    }
                    deployment.setRegistration(null);
                    deployment.setContext(null);
                    context.stop();
                }
                catch (Exception e)
                {
                    SystemLogger.error(String.format("Undeploying web application bundle %s failed.", webAppBundle.getSymbolicName()), e);
                }
                finally
                {
                    postEvent(WebEvent.TOPIC_UNDEPLOYED, webAppBundle, extenderBundle, null, null, null);
                }
            }
        });
    }

    private void postEvent(final String topic,
            final Bundle webAppBundle,
            final Bundle extenderBundle,
            final Throwable exception,
            final String collision,
            final Long collisionBundles)
    {
        final Object ea = this.eventAdmin;
        if (ea != null)
        {
            WebEvent.postEvent(ea, topic, webAppBundle, extenderBundle, exception, collision, collisionBundles);
        }
    }

    @Override
    public void lifeCycleStarted(final LifeCycle event)
    {
        for (Deployment deployment : this.deployments.values())
        {
            if (deployment.getContext() == null)
            {
                postEvent(WebEvent.TOPIC_DEPLOYING, deployment.getBundle(), this.context.getBundle(), null, null, null);
                WebAppBundleContext context = new WebAppBundleContext(deployment.getContextPath(), deployment.getBundle(), this.getClass().getClassLoader());
                deploy(deployment, context);
            }
        }
    }

    @Override
    public void lifeCycleStopping(final LifeCycle event)
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
    public static class Deployment
    {
        private String contextPath;
        private Bundle bundle;
        private WebAppBundleContext context;
        private ServiceRegistration<ServletContext> registration;

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

        public ServiceRegistration<ServletContext> getRegistration()
        {
            return this.registration;
        }

        public void setRegistration(ServiceRegistration<ServletContext> registration)
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
        @Override
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

    /**
     * @return <code>true</code> if there is a valid executor service available,
     *         <code>false</code> otherwise.
     */
    private boolean isExecutorServiceAvailable() {
        return this.executor != null && !this.executor.isShutdown();
    }
}
