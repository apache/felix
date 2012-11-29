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
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 *
 */
public class Activator implements BundleActivator, SynchronousBundleListener
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
    private static ServiceTracker m_logService;

    // the package admin service (see BindMethod.getParameterClass)
    private static ServiceTracker m_packageAdmin;

    // map of BundleComponentActivator instances per Bundle indexed by Bundle id
    private Map<Long, BundleComponentActivator> m_componentBundles;

    // registry of managed component
    private ComponentRegistry m_componentRegistry;

    //  thread acting upon configurations
    private ComponentActorThread m_componentActor;

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

        // require the log service
        m_logService = new ServiceTracker( context, LOGSERVICE_CLASS, null );
        m_logService.open();

        // prepare component registry
        m_componentBundles = new HashMap<Long, BundleComponentActivator>();
        m_componentRegistry = new ComponentRegistry( context );

        // get the configuration
        m_configuration.start( context );

        // log SCR startup
        log( LogService.LOG_INFO, context.getBundle(), " Version = "
            + context.getBundle().getHeaders().get( Constants.BUNDLE_VERSION ), null );

        // create and start the component actor
        m_componentActor = new ComponentActorThread();
        Thread t = new Thread(m_componentActor, "SCR Component Actor");
        t.setDaemon( true );
        t.start();

        // register for bundle updates
        context.addBundleListener( this );

        // 112.8.2 load all components of active bundles
        loadAllComponents( context );

        // register the Gogo and old Shell commands
        ScrCommand.register(context, m_componentRegistry, m_configuration);
    }


    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void stop( BundleContext context ) throws Exception
    {
        // unregister as bundle listener
        context.removeBundleListener( this );

        // 112.8.2 dispose off all active components
        disposeAllComponents();

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


    // ---------- BundleListener Interface -------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *      change.
     */
    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.LAZY_ACTIVATION || event.getType() == BundleEvent.STARTED )
        {
            // FELIX-1666 LAZY_ACTIVATION event is sent if the bundle has lazy
            // activation policy and is waiting for class loader access to
            // actually load it; STARTED event is sent if bundle has regular
            // activation policy or if the lazily activated bundle finally is
            // really started. In both cases just try to load the components
            loadComponents( event.getBundle() );
        }
        else if ( event.getType() == BundleEvent.STOPPING )
        {
            disposeComponents( event.getBundle() );
        }
    }


    //---------- Component Management -----------------------------------------

    // Loads the components of all bundles currently active.
    private void loadAllComponents( BundleContext context )
    {
        Bundle[] bundles = context.getBundles();
        for ( Bundle bundle : bundles )
        {
            if ( ComponentRegistry.isBundleActive( bundle ) )
            {
                loadComponents( bundle );
            }
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
            log( LogService.LOG_ERROR, m_context.getBundle(), "Cannot get BundleContext of bundle "
                + bundle.getSymbolicName() + "/" + bundle.getBundleId(), null );
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
            log( LogService.LOG_DEBUG, m_context.getBundle(), "Components for bundle  " + bundle.getSymbolicName()
                + "/" + bundle.getBundleId() + " already loaded. Nothing to do.", null );
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
                    "Bundle "
                        + bundle.getSymbolicName()
                        + "/"
                        + bundle.getBundleId()
                        + " has been stopped while trying to activate its components. Trying again when the bundles gets startet again.",
                    e );
            }
            else
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while loading components of bundle "
                    + bundle.getSymbolicName() + "/" + bundle.getBundleId(), e );
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

        if ( ga instanceof BundleComponentActivator )
        {
            try
            {
                ( ( BundleComponentActivator ) ga ).dispose( ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while disposing components of bundle "
                    + bundle.getSymbolicName() + "/" + bundle.getBundleId(), e );
            }
        }
    }


    // Unloads all components registered with the SCR
    private void disposeAllComponents()
    {
        final Object[] activators;
        synchronized ( m_componentBundles )
        {
            activators = m_componentBundles.values().toArray();
            m_componentBundles.clear();
        }

        for ( Object activator : activators )
        {
            if ( activator instanceof BundleComponentActivator )
            {
                final BundleComponentActivator ga = ( BundleComponentActivator ) activator;
                try
                {
                    final Bundle bundle = ga.getBundleContext().getBundle();
                    try
                    {
                        ga.dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
                    }
                    catch ( Exception e )
                    {
                        log( LogService.LOG_ERROR, m_context.getBundle(), "Error while disposing components of bundle "
                                + bundle.getSymbolicName() + "/" + bundle.getBundleId(), e );
                    }
                }
                catch ( IllegalStateException e )
                {
                    //bundle context was already shut down in another thread, bundle is not available.
                }
            }
        }
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
        if ( m_configuration.getLogLevel() >= level )
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