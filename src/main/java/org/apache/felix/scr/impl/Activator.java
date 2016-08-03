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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.impl.config.ScrConfigurationImpl;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.inject.ClassUtils;
import org.apache.felix.scr.impl.runtime.ServiceComponentRuntimeImpl;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 *
 */
public class Activator extends AbstractExtender implements SimpleLogger
{
    //  name of the LogService class (this is a string to not create a reference to the class)
    static final String LOGSERVICE_CLASS = "org.osgi.service.log.LogService";

    // Our configuration from bundle context properties and Config Admin
    private ScrConfigurationImpl m_configuration;

    private BundleContext m_context;

    //Either this bundle's context or the framework bundle context, depending on the globalExtender setting.
    private BundleContext m_globalContext;

    // this bundle
    private Bundle m_bundle;

    // the log service to log messages to
    private volatile ServiceTracker<LogService, LogService> m_logService;

    // map of BundleComponentActivator instances per Bundle indexed by Bundle id
    private Map<Long, BundleComponentActivator> m_componentBundles;

    // registry of managed component
    private ComponentRegistry m_componentRegistry;

    //  thread acting upon configurations
    private ComponentActorThread m_componentActor;

    private ServiceRegistration<?> m_runtime_reg;

    private ScrCommand m_scrCommand;

    public Activator()
    {
        m_configuration = new ScrConfigurationImpl( this );
        setSynchronous( true );
    }

    /**
     * Registers this instance as a (synchronous) bundle listener and loads the
     * components of already registered bundles.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    @Override
    public void start(BundleContext context) throws Exception
    {
        m_context = context;
        m_bundle = context.getBundle();
        // require the log service
        m_logService = new ServiceTracker<LogService, LogService>( m_context, LOGSERVICE_CLASS, null );
        m_logService.open();
        // set bundle context for PackageAdmin tracker
        ClassUtils.setBundleContext( context );
        // get the configuration
        m_configuration.start( m_context ); //this will call restart, which calls super.start.
    }

    public void restart(boolean globalExtender)
    {
        BundleContext context = m_globalContext;
        if ( globalExtender )
        {
            m_globalContext = m_context.getBundle( Constants.SYSTEM_BUNDLE_LOCATION ).getBundleContext();
        }
        else
        {
            m_globalContext = m_context;
        }
        if ( ClassUtils.m_packageAdmin != null )
        {
            log( LogService.LOG_INFO, m_bundle,
                "Stopping to restart with new globalExtender setting: " + globalExtender, null );
            //this really is a restart, not the initial start
            // the initial start where m_globalContext is null should skip this as m_packageAdmin should not yet be set.
            try
            {
                super.stop( context );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_bundle, "Exception stopping during restart", e );
            }
        }
        try
        {
            log( LogService.LOG_INFO, m_bundle, "Starting with globalExtender setting: " + globalExtender, null );
            super.start( m_globalContext );
        }
        catch ( Exception e )
        {
            log( LogService.LOG_ERROR, m_bundle, "Exception starting during restart", e );
        }

    }

    @Override
    protected void doStart() throws Exception
    {

        // prepare component registry
        m_componentBundles = new HashMap<Long, BundleComponentActivator>();
        m_componentRegistry = new ComponentRegistry( this );

        final ServiceComponentRuntime runtime = new ServiceComponentRuntimeImpl( m_globalContext, m_componentRegistry );
        m_runtime_reg = m_context.registerService( ServiceComponentRuntime.class, runtime, null );

        // log SCR startup
        log( LogService.LOG_INFO, m_bundle, " Version = {0}",
            new Object[] { m_bundle.getHeaders().get( Constants.BUNDLE_VERSION ) }, null );

        // create and start the component actor
        m_componentActor = new ComponentActorThread( this );
        Thread t = new Thread( m_componentActor, "SCR Component Actor" );
        t.setDaemon( true );
        t.start();

        super.doStart();

        m_scrCommand = ScrCommand.register( m_context, runtime, m_configuration );
        m_configuration.setScrCommand( m_scrCommand );
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        super.stop( context );
        m_configuration.stop();
        m_configuration = null;
    }

    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     */
    @Override
    public void doStop() throws Exception
    {
        // stop tracking
        super.doStop();

        if ( m_scrCommand != null )
        {
            m_scrCommand.unregister();
            m_scrCommand = null;
        }
        if ( m_runtime_reg != null )
        {
            m_runtime_reg.unregister();
            m_runtime_reg = null;
        }
        // dispose component registry
        if ( m_componentRegistry != null )
        {
            m_componentRegistry = null;
        }

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
        ClassUtils.close();
    }

    //---------- Component Management -----------------------------------------

    private volatile boolean gogoMissing = true;

    @Override
    protected Extension doCreateExtension(final Bundle bundle) throws Exception
    {
        if ( gogoMissing )
        {
            try
            {
                bundle.loadClass( "org.apache.felix.service.command.Descriptor" );
                gogoMissing = false;
            }
            catch ( Throwable e )
            {
            }
        }
        return new ScrExtension( bundle );
    }

    protected class ScrExtension implements Extension
    {

        private final Bundle bundle;
        private final Lock stateLock = new ReentrantLock();

        public ScrExtension(Bundle bundle)
        {
            this.bundle = bundle;
        }

        public void start()
        {
            boolean acquired = false;
            try
            {
                try
                {
                    acquired = stateLock.tryLock( m_configuration.stopTimeout(), TimeUnit.MILLISECONDS );

                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    log( LogService.LOG_WARNING, m_bundle,
                        "The wait for bundle {0}/{1} being destroyed before starting has been interrupted.",
                        new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, e );
                }
                loadComponents( ScrExtension.this.bundle );
            }
            finally
            {
                if ( acquired )
                {
                    stateLock.unlock();
                }
            }
        }

        public void destroy()
        {
            boolean acquired = false;
            try
            {
                try
                {
                    acquired = stateLock.tryLock( m_configuration.stopTimeout(), TimeUnit.MILLISECONDS );

                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    log( LogService.LOG_WARNING, m_bundle,
                        "The wait for bundle {0}/{1} being started before destruction has been interrupted.",
                        new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, e );
                }
                disposeComponents( bundle );
            }
            finally
            {
                if ( acquired )
                {
                    stateLock.unlock();
                }
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
    private void loadComponents(Bundle bundle)
    {
        if ( bundle.getHeaders().get( ComponentConstants.SERVICE_COMPONENT ) == null )
        {
            // no components in the bundle, abandon
            return;
        }

        // there should be components, load them with a bundle context
        BundleContext context = bundle.getBundleContext();
        if ( context == null )
        {
            log( LogService.LOG_DEBUG, m_bundle, "Cannot get BundleContext of bundle {0}/{1}",
                new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, null );
            return;
        }

        //Examine bundle for extender requirement; if present check if bundle is wired to us.
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        List<BundleWire> extenderWires = wiring.getRequiredWires( ExtenderNamespace.EXTENDER_NAMESPACE );
        try
        {
            for ( BundleWire wire : extenderWires )
            {
                if ( ComponentConstants.COMPONENT_CAPABILITY_NAME.equals(
                    wire.getCapability().getAttributes().get( ExtenderNamespace.EXTENDER_NAMESPACE ) ) )
                {
                    if ( !m_bundle.adapt( BundleRevision.class ).equals( wire.getProvider() ) )
                    {
                        log( LogService.LOG_DEBUG, m_bundle, "Bundle {0}/{1} wired to a different extender: {2}",
                            new Object[] { bundle.getSymbolicName(), bundle.getBundleId(),
                                    wire.getProvider().getSymbolicName() },
                            null );
                        return;
                    }
                    break;
                }
            }
        }
        catch ( NoSuchMethodError e )
        {
            log( LogService.LOG_DEBUG, m_bundle, "Cannot determine bundle wiring on pre R6 framework", null, null );
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
            log( LogService.LOG_DEBUG, m_bundle, "Components for bundle {0}/{1} already loaded. Nothing to do.",
                new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, null );
            return;
        }

        try
        {
            BundleComponentActivator ga = new BundleComponentActivator( this, m_componentRegistry, m_componentActor,
                context, m_configuration );
            ga.initialEnable();

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
                log( LogService.LOG_DEBUG, m_bundle,
                    "Bundle {0}/{1} has been stopped while trying to activate its components. Trying again when the bundles gets started again.",
                    new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, e );
            }
            else
            {
                log( LogService.LOG_ERROR, m_bundle, "Error while loading components of bundle {0}/{1}",
                    new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, e );
            }
        }
    }

    /**
     * Unloads components of the given bundle. If no components have been loaded
     * for the bundle, this method has no effect.
     */
    private void disposeComponents(Bundle bundle)
    {
        final BundleComponentActivator ga;
        synchronized ( m_componentBundles )
        {
            ga = m_componentBundles.remove( bundle.getBundleId() );
        }

        if ( ga != null )
        {
            try
            {
                int reason = isStopping()? ComponentConstants.DEACTIVATION_REASON_DISPOSED
                    : ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED;
                ga.dispose( reason );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_bundle, "Error while disposing components of bundle {0}/{1}",
                    new Object[] { bundle.getSymbolicName(), bundle.getBundleId() }, e );
            }
        }
    }

    @Override
    protected void debug(Bundle bundle, String msg)
    {
        final String message = MessageFormat.format( msg + " bundle: {0}/{1}", bundle.getSymbolicName(),
            bundle.getBundleId() );
        log( LogService.LOG_DEBUG, bundle, message, null );
    }

    @Override
    protected void warn(Bundle bundle, String msg, Throwable t)
    {
        final String message = MessageFormat.format( msg + " bundle: {0}/{1}", bundle.getSymbolicName(),
            bundle.getBundleId() );
        log( LogService.LOG_WARNING, bundle, message, t );
    }

    @Override
    protected void error(String msg, Throwable t)
    {
        log( LogService.LOG_DEBUG, m_bundle, msg, t );
    }

    //    @Override
    public void log(int level, String message, Throwable ex)
    {
        log( level, null, message, ex );
    }

    //    @Override
    public void log(int level, String pattern, Object[] arguments, Throwable ex)
    {
        if ( isLogEnabled( level ) )
        {
            final String message = MessageFormat.format( pattern, arguments );
            log( level, null, message, ex );
        }
    }

    public void log(int level, Bundle bundle, String pattern, Object[] arguments, Throwable ex)
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
    public boolean isLogEnabled(int level)
    {
        return m_configuration == null || m_configuration.getLogLevel() >= level;
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
    public void log(int level, Bundle bundle, String message, Throwable ex)
    {
        if ( isLogEnabled( level ) )
        {
            ServiceTracker<LogService, LogService> t = m_logService;
            LogService logger = ( t != null )? t.getService(): null;
            if ( logger == null )
            {
                // output depending on level
                PrintStream out = ( level == LogService.LOG_ERROR )? System.err: System.out;

                // level as a string
                StringBuffer buf = new StringBuffer();
                switch (level)
                {
                    case ( LogService.LOG_DEBUG ):
                        buf.append( "DEBUG: " );
                        break;
                    case ( LogService.LOG_INFO ):
                        buf.append( "INFO : " );
                        break;
                    case ( LogService.LOG_WARNING ):
                        buf.append( "WARN : " );
                        break;
                    case ( LogService.LOG_ERROR ):
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
                synchronized ( out )
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
                logger.log( level, message, ex );
            }
        }
    }

}