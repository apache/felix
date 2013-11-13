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
package org.apache.felix.scr.impl;


import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 *
 */
public class Activator extends AbstractExtender
{
    //  name of the LogService class (this is a string to not create a reference to the class)
    static final String LOGSERVICE_CLASS = "org.osgi.service.log.LogService";

    // name of the PackageAdmin class (this is a string to not create a reference to the class)
    static final String PACKAGEADMIN_CLASS = "org.osgi.service.packageadmin.PackageAdmin";

    // Our configuration from bundle context properties and Config Admin
    private static ScrConfiguration m_configuration = new ScrConfiguration();

    // this bundle's context
    private static BundleContext m_context;

    // the log service to log messages to
    private static volatile ServiceTracker m_logService;

    // the package admin service (see BindMethod.getParameterClass)
    private static volatile ServiceTracker m_packageAdmin;

    // map of BundleComponentActivator instances per Bundle indexed by Bundle id
    private Map<Long, BundleComponentActivator> m_componentBundles;

    // registry of managed component
    private ComponentRegistry m_componentRegistry;

    //  thread acting upon configurations
    private ComponentActorThread m_componentActor;

    public Activator() {
        setSynchronous(true);
    }

    /**
     * Registers this instance as a (synchronous) bundle listener and loads the
     * components of already registered bundles.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        m_context = context;
        super.start(context);
    }

    protected void doStart() throws Exception {
        // require the log service
        m_logService = new ServiceTracker( m_context, LOGSERVICE_CLASS, null );
        m_logService.open();

        // prepare component registry
        m_componentBundles = new HashMap<Long, BundleComponentActivator>();
        m_componentRegistry = new ComponentRegistry( m_context );

        // get the configuration
        m_configuration.start( m_context );

        // log SCR startup
        log( LogService.LOG_INFO, m_context.getBundle(), " Version = {0}",
            new Object[] {m_context.getBundle().getHeaders().get( Constants.BUNDLE_VERSION )}, null );

        // create and start the component actor
        m_componentActor = new ComponentActorThread();
        Thread t = new Thread(m_componentActor, "SCR Component Actor");
        t.setDaemon( true );
        t.start();

        super.doStart();

        // register the Gogo and old Shell commands
        ScrCommand scrCommand = ScrCommand.register(m_context, m_componentRegistry, m_configuration);
        m_configuration.setScrCommand( scrCommand );
    }


    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     */
    public void doStop() throws Exception
    {
        // stop tracking
        super.doStop();

        // dispose component registry
        m_componentRegistry.dispose();

        // terminate the actor thread
        if ( m_componentActor != null )
        {
            m_componentActor.terminate();
            m_componentActor = null;
        }

        // close the LogService tracker now
        if ( m_logService != null )
        {
            m_logService.close();
            m_logService = null;
        }

        // close the PackageAdmin tracker now
        if ( m_packageAdmin != null )
        {
            m_packageAdmin.close();
            m_packageAdmin = null;
        }

        // remove the reference to the component context
        m_context = null;
    }


    //---------- Component Management -----------------------------------------


    @Override
    protected Extension doCreateExtension(final Bundle bundle) throws Exception
    {
        return new ScrExtension(bundle);
    }

    protected class ScrExtension implements Extension {

        private final Bundle bundle;
        private final CountDownLatch started;

        public ScrExtension(Bundle bundle) {
            this.bundle = bundle;
            this.started = new CountDownLatch(1);
        }

        public void start() {
            try {
                loadComponents( ScrExtension.this.bundle );
            } finally {
                started.countDown();
            }
        }

        public void destroy() {
            try {
                this.started.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log( LogService.LOG_WARNING, m_context.getBundle(), "The wait for bundle {0}/{1} being started before destruction has been interrupted.",
                        new Object[] {bundle.getSymbolicName(), bundle.getBundleId()}, e );
            }
            disposeComponents( this.bundle );
        }
    }

    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link Bundle#getBundleContext()} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    private void loadComponents( Bundle bundle )
    {
        if ( bundle.getHeaders().get( "Service-Component" ) == null )
        {
            // no components in the bundle, abandon
            return;
        }

        // there should be components, load them with a bundle context
        BundleContext context = bundle.getBundleContext();
        if ( context == null )
        {
            log( LogService.LOG_ERROR, m_context.getBundle(), "Cannot get BundleContext of bundle {0}/{1}",
                new Object[] {bundle.getSymbolicName(), bundle.getBundleId()}, null );
            return;
        }

        // FELIX-1666 method is called for the LAZY_ACTIVATION event and
        // the started event. Both events cause this method to be called;
        // so we have to make sure to not load components twice
        // FELIX-2231 Mark bundle loaded early to prevent concurrent loading
        // if LAZY_ACTIVATION and STARTED event are fired at the same time
        final boolean loaded;
        final Long bundleId = bundle.getBundleId();
        synchronized ( m_componentBundles )
        {
            if ( m_componentBundles.containsKey( bundleId ) )
            {
                loaded = true;
            }
            else
            {
                m_componentBundles.put( bundleId, null );
                loaded = false;
            }
        }

        // terminate if already loaded (or currently being loaded)
        if ( loaded )
        {
            log( LogService.LOG_DEBUG, m_context.getBundle(), "Components for bundle {0}/{1} already loaded. Nothing to do.",
                new Object[] {bundle.getSymbolicName(), bundle.getBundleId()}, null );
            return;
        }

        try
        {
            BundleComponentActivator ga = new BundleComponentActivator( m_componentRegistry, m_componentActor, context,
                m_configuration );

            // replace bundle activator in the map
            synchronized ( m_componentBundles )
            {
                m_componentBundles.put( bundleId, ga );
            }
        }
        catch ( Exception e )
        {
            // remove the bundle id from the bundles map to ensure it is
            // not marked as being loaded
            synchronized ( m_componentBundles )
            {
                m_componentBundles.remove( bundleId );
            }

            if ( e instanceof IllegalStateException && bundle.getState() != Bundle.ACTIVE )
            {
                log(
                    LogService.LOG_DEBUG,
                    m_context.getBundle(),
                    "Bundle {0}/{1} has been stopped while trying to activate its components. Trying again when the bundles gets started again.",
                new Object[] {bundle.getSymbolicName(), bundle.getBundleId()},
                    e );
            }
            else
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while loading components of bundle {0}/{1}",
                new Object[] {bundle.getSymbolicName(), bundle.getBundleId()}, e );
            }
        }
    }


    /**
     * Unloads components of the given bundle. If no components have been loaded
     * for the bundle, this method has no effect.
     */
    private void disposeComponents( Bundle bundle )
    {
        final Object ga;
        synchronized ( m_componentBundles )
        {
            ga = m_componentBundles.remove( bundle.getBundleId() );
        }

        if ( ga != null )
        {
            try
            {
                int reason = isStopping()
                        ? ComponentConstants.DEACTIVATION_REASON_DISPOSED
                        : ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED;
                ( ( BundleComponentActivator ) ga ).dispose( reason );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while disposing components of bundle {0}/{1}",
                    new Object[] {bundle.getSymbolicName(), bundle.getBundleId()}, e );
            }
        }
    }

    @Override
    protected void debug(Bundle bundle, String msg) {
        log( LogService.LOG_DEBUG, bundle, msg, null );
    }

    @Override
    protected void warn(Bundle bundle, String msg, Throwable t) {
        log( LogService.LOG_WARNING, bundle, msg, t );
    }

    @Override
    protected void error(String msg, Throwable t) {
        log( LogService.LOG_DEBUG, m_context.getBundle(), msg, t );
    }

    public static void log( int level, Bundle bundle, String pattern, Object[] arguments, Throwable ex )
    {
        if ( isLogEnabled( level ) )
        {
            final String message = MessageFormat.format( pattern, arguments );
            log( level, bundle, message, ex );
        }
    }

    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public static boolean isLogEnabled( int level )
    {
        return m_configuration.getLogLevel() >= level;
    }

    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param message The message to log
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     *      or <code>null</code> to not log a stack trace.
     */
    public static void log( int level, Bundle bundle, String message, Throwable ex )
    {
        if ( isLogEnabled( level ) )
        {
            ServiceTracker t = m_logService;
            Object logger = ( t != null ) ? t.getService() : null;
            if ( logger == null )
            {
                // output depending on level
                PrintStream out = ( level == LogService.LOG_ERROR ) ? System.err : System.out;

                // level as a string
                StringBuffer buf = new StringBuffer();
                switch ( level )
                {
                    case ( LogService.LOG_DEBUG     ):
                        buf.append( "DEBUG: " );
                        break;
                    case ( LogService.LOG_INFO     ):
                        buf.append( "INFO : " );
                        break;
                    case ( LogService.LOG_WARNING     ):
                        buf.append( "WARN : " );
                        break;
                    case ( LogService.LOG_ERROR     ):
                        buf.append( "ERROR: " );
                        break;
                    default:
                        buf.append( "UNK  : " );
                        break;
                }

                // bundle information
                if ( bundle != null )
                {
                    buf.append( bundle.getSymbolicName() );
                    buf.append( " (" );
                    buf.append( bundle.getBundleId() );
                    buf.append( "): " );
                }

                // the message
                buf.append( message );

                // keep the message and the stacktrace together
                synchronized ( out)
                {
                    out.println( buf );
                    if ( ex != null )
                    {
                        ex.printStackTrace( out );
                    }
                }
            }
            else
            {
                ( ( LogService ) logger ).log( level, message, ex );
            }
        }
    }


    public static Object getPackageAdmin()
    {
        if ( m_packageAdmin == null )
        {
            synchronized ( Activator.class )
            {
                if ( m_packageAdmin == null )
                {
                    m_packageAdmin = new ServiceTracker( m_context, PACKAGEADMIN_CLASS, null );
                    m_packageAdmin.open();
                }
            }
        }

        return m_packageAdmin.getService();
    }
}