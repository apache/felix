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
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.apache.felix.cm.impl.helper.BaseTracker;
import org.apache.felix.cm.impl.helper.ConfigurationMap;
import org.apache.felix.cm.impl.helper.ManagedServiceFactoryTracker;
import org.apache.felix.cm.impl.helper.ManagedServiceTracker;
import org.apache.felix.cm.impl.helper.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>ConfigurationManager</code> is the central class in this
 * implementation of the Configuration Admin Service Specification. As such it
 * has the following tasks:
 * <ul>
 * <li>It is a <code>BundleActivator</code> which is called when the bundle
 * is started and stopped.
 * <li>It is a <code>BundleListener</code> which gets informed when the
 * states of bundles change. Mostly this is needed to unbind any bound
 * configuration in case a bundle is uninstalled.
 * <li>It is a <code>ServiceListener</code> which gets informed when
 * <code>ManagedService</code> and <code>ManagedServiceFactory</code>
 * services are registered and unregistered. This is used to provide
 * configuration to these services. As a service listener it also listens for
 * {@link PersistenceManager} instances being registered to support different
 * configuration persistence layers.
 * <li>A {@link ConfigurationAdminFactory} instance is registered as the
 * <code>ConfigurationAdmin</code> service.
 * <li>A {@link FilePersistenceManager} instance is registered as a default
 * {@link PersistenceManager}.
 * <li>Last but not least this instance manages all tasks laid out in the
 * specification such as maintaining configuration, taking care of configuration
 * events, etc.
 * </ul>
 * <p>
 * The default {@link FilePersistenceManager} is configured with a configuration
 * location taken from the <code>felix.cm.dir</code> framework property. If
 * this property is not set the <code>config</code> directory in the current
 * working directory as specified in the <code>user.dir</code> system property
 * is used.
 */
public class ConfigurationManager implements BundleActivator, BundleListener
{

    /**
     * The name of the bundle context property defining the location for the
     * configuration files (value is "felix.cm.dir").
     *
     * @see #start(BundleContext)
     */
    public static final String CM_CONFIG_DIR = "felix.cm.dir";

    /**
     * The name of the bundle context property defining the maximum log level
     * (value is "felix.cm.loglevel"). The log level setting is only used if
     * there is no OSGi LogService available. Otherwise this setting is ignored.
     * <p>
     * This value of this property is expected to be an integer number
     * corresponding to the log level values of the OSGi LogService. That is 1
     * for errors, 2 for warnings, 3 for informational messages and 4 for debug
     * messages. The default value is 2, such that only warnings and errors are
     * logged in the absence of a LogService.
     */
    public static final String CM_LOG_LEVEL = "felix.cm.loglevel";

    // The name of the LogService (not using the class, which might be missing)
    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    private static final int CM_LOG_LEVEL_DEFAULT = 2;

    // random number generator to create configuration PIDs for factory
    // configurations
    private static Random numberGenerator;

    // the BundleContext of the Configuration Admin Service bundle
    BundleContext bundleContext;

    // the service registration of the default file persistence manager
    private volatile ServiceRegistration filepmRegistration;

    // the service registration of the configuration admin
    private volatile ServiceRegistration configurationAdminRegistration;

    // the ServiceTracker to emit log services (see log(int, String, Throwable))
    private ServiceTracker logTracker;

    // the ConfigurationEvent listeners
    private ServiceTracker configurationListenerTracker;

    // the synchronous ConfigurationEvent listeners
    private ServiceTracker syncConfigurationListenerTracker;

    // service tracker for managed services
    private ManagedServiceTracker managedServiceTracker;

    // service tracker for managed service factories
    private ManagedServiceFactoryTracker managedServiceFactoryTracker;

    // PersistenceManager services
    private ServiceTracker persistenceManagerTracker;

    // the thread used to schedule tasks required to run asynchronously
    private UpdateThread updateThread;

    // the thread used to schedule events to be dispatched asynchronously
    private UpdateThread eventThread;

    /**
     * The actual list of {@link PersistenceManager persistence managers} to use
     * when looking for configuration data. This list is built from the
     * {@link #persistenceManagerMap}, which is ordered according to the
     * {@link RankingComparator}.
     */
    private CachingPersistenceManagerProxy[] persistenceManagers;

    // the persistenceManagerTracker.getTrackingCount when the
    // persistenceManagers were last got
    private int pmtCount;

    // the cache of Factory instances mapped by their factory PID
    private final HashMap<String, Factory> factories = new HashMap<String, Factory>();

    // the cache of Configuration instances mapped by their PID
    // have this always set to prevent NPE on bundle shutdown
    private final HashMap<String, ConfigurationImpl> configurations = new HashMap<String, ConfigurationImpl>();

    /**
     * The map of dynamic configuration bindings. This maps the
     * PID of the dynamically bound configuration or factory to its bundle
     * location.
     * <p>
     * On bundle startup this map is loaded from persistence and validated
     * against the locations of installed bundles: Entries pointing to bundle
     * locations not currently installed are removed.
     * <p>
     * The map is written to persistence on each change.
     */
    private DynamicBindings dynamicBindings;

    // the maximum log level when no LogService is available
    private int logLevel = CM_LOG_LEVEL_DEFAULT;

    // flag indicating whether BundleChange events should be consumed (FELIX-979)
    private volatile boolean handleBundleEvents;

    // flag indicating whether the manager is considered alive
    private volatile boolean isActive;

    public void start( BundleContext bundleContext )
    {
        // track the log service using a ServiceTracker
        logTracker = new ServiceTracker( bundleContext, LOG_SERVICE_NAME , null );
        logTracker.open();

        // assign the log level
        String logLevelProp = bundleContext.getProperty( CM_LOG_LEVEL );
        if ( logLevelProp == null )
        {
            logLevel = CM_LOG_LEVEL_DEFAULT;
        }
        else
        {
            try
            {
                logLevel = Integer.parseInt( logLevelProp );
            }
            catch ( NumberFormatException nfe )
            {
                logLevel = CM_LOG_LEVEL_DEFAULT;
            }
        }

        // set up some fields
        this.bundleContext = bundleContext;

        // configurationlistener support
        configurationListenerTracker = new ServiceTracker( bundleContext, ConfigurationListener.class.getName(), null );
        configurationListenerTracker.open();
        syncConfigurationListenerTracker = new ServiceTracker( bundleContext,
            SynchronousConfigurationListener.class.getName(), null );
        syncConfigurationListenerTracker.open();

        // initialize the asynchonous updater thread
        ThreadGroup tg = new ThreadGroup( "Configuration Admin Service" );
        tg.setDaemon( true );
        this.updateThread = new UpdateThread( this, tg, "CM Configuration Updater" );
        this.eventThread = new UpdateThread( this, tg, "CM Event Dispatcher" );

        // set up the location (might throw IllegalArgumentException)
        try
        {
            FilePersistenceManager fpm = new FilePersistenceManager( bundleContext, bundleContext
                .getProperty( CM_CONFIG_DIR ) );
            Hashtable props = new Hashtable();
            props.put( Constants.SERVICE_PID, fpm.getClass().getName() );
            props.put( Constants.SERVICE_DESCRIPTION, "Platform Filesystem Persistence Manager" );
            props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
            props.put( Constants.SERVICE_RANKING, new Integer( Integer.MIN_VALUE ) );
            filepmRegistration = bundleContext.registerService( PersistenceManager.class.getName(), fpm, props );

            // setup dynamic configuration bindings
            dynamicBindings = new DynamicBindings( bundleContext, fpm );
        }
        catch ( IOException ioe )
        {
            log( LogService.LOG_ERROR, "Failure setting up dynamic configuration bindings", ioe );
        }
        catch ( IllegalArgumentException iae )
        {
            log( LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae );
        }

        // register as bundle and service listener
        handleBundleEvents = true;
        bundleContext.addBundleListener( this );

        // get all persistence managers to begin with
        pmtCount = 1; // make sure to get the persistence managers at least once
        persistenceManagerTracker = new ServiceTracker( bundleContext, PersistenceManager.class.getName(), null );
        persistenceManagerTracker.open();

        // consider alive now (before clients use Configuration Admin
        // service registered in the next step)
        isActive = true;

        // create and register configuration admin - start after PM tracker ...
        ConfigurationAdminFactory caf = new ConfigurationAdminFactory( this );
        Hashtable props = new Hashtable();
        props.put( Constants.SERVICE_PID, "org.apache.felix.cm.ConfigurationAdmin" );
        props.put( Constants.SERVICE_DESCRIPTION, "Configuration Admin Service Specification 1.5 Implementation" );
        props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
        configurationAdminRegistration = bundleContext.registerService( ConfigurationAdmin.class.getName(), caf, props );

        // start handling ManagedService[Factory] services
        managedServiceTracker = new ManagedServiceTracker(this);
        managedServiceFactoryTracker = new ManagedServiceFactoryTracker(this);

        // start processing the event queues only after registering the service
        // see FELIX-2813 for details
        this.updateThread.start();
        this.eventThread.start();
    }


    public void stop( BundleContext bundleContext )
    {

        // stop handling bundle events immediately
        handleBundleEvents = false;

        // stop handling ManagedService[Factory] services
        managedServiceFactoryTracker.close();
        managedServiceTracker.close();

        // stop queue processing before unregistering the service
        // see FELIX-2813 for details
        if ( updateThread != null )
        {
            updateThread.terminate();
        }
        if ( eventThread != null )
        {
            eventThread.terminate();
        }

        // immediately unregister the Configuration Admin before cleaning up
        // clearing the field before actually unregistering the service
        // prevents IllegalStateException in getServiceReference() if
        // the field is not null but the service already unregistered
        final ServiceRegistration caReg = configurationAdminRegistration;
        configurationAdminRegistration = null;
        if ( caReg != null )
        {
            caReg.unregister();
        }

        // consider inactive after unregistering such that during
        // unregistration the manager is still alive and can react
        isActive = false;

        // don't care for PersistenceManagers any more
        persistenceManagerTracker.close();

        // shutdown the file persistence manager
        final ServiceRegistration filePmReg = filepmRegistration;
        filepmRegistration = null;
        if ( filePmReg != null )
        {
            filePmReg.unregister();
        }

        // stop listening for events
        bundleContext.removeBundleListener( this );

        if ( configurationListenerTracker != null )
        {
            configurationListenerTracker.close();
        }

        if ( syncConfigurationListenerTracker != null )
        {
            syncConfigurationListenerTracker.close();
        }

        if ( logTracker != null )
        {
            logTracker.close();
        }

        // just ensure the configuration cache is empty
        synchronized ( configurations )
        {
            configurations.clear();
        }

        // just ensure the factory cache is empty
        synchronized ( factories )
        {
            factories.clear();
        }

        this.bundleContext = null;
    }


    /**
     * Returns <code>true</code> if this manager is considered active.
     */
    boolean isActive()
    {
        return isActive;
    }

    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    // ---------- Configuration caching support --------------------------------

    ConfigurationImpl getCachedConfiguration( String pid )
    {
        synchronized ( configurations )
        {
            return configurations.get( pid );
        }
    }


    ConfigurationImpl[] getCachedConfigurations()
    {
        synchronized ( configurations )
        {
            return configurations.values().toArray(
                new ConfigurationImpl[configurations.size()] );
        }
    }


    ConfigurationImpl cacheConfiguration( ConfigurationImpl configuration )
    {
        synchronized ( configurations )
        {
            final String pid = configuration.getPidString();
            final Object existing = configurations.get( pid );
            if ( existing != null )
            {
                return ( ConfigurationImpl ) existing;
            }

            configurations.put( pid, configuration );
            return configuration;
        }
    }


    void removeConfiguration( ConfigurationImpl configuration )
    {
        synchronized ( configurations )
        {
            configurations.remove( configuration.getPidString() );
        }
    }


    Factory getCachedFactory( String factoryPid )
    {
        synchronized ( factories )
        {
            return factories.get( factoryPid );
        }
    }


    Factory[] getCachedFactories()
    {
        synchronized ( factories )
        {
            return factories.values().toArray( new Factory[factories.size()] );
        }
    }


    void cacheFactory( Factory factory )
    {
        synchronized ( factories )
        {
            factories.put( factory.getFactoryPidString(), factory );
        }
    }


    // ---------- ConfigurationAdminImpl support

    void setDynamicBundleLocation( final String pid, final String location )
    {
        if ( dynamicBindings != null )
        {
            try
            {
                dynamicBindings.putLocation( pid, location );
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Failed storing dynamic configuration binding for {0} to {1}", new Object[]
                    { pid, location, ioe } );
            }
        }
    }


    String getDynamicBundleLocation( final String pid )
    {
        if ( dynamicBindings != null )
        {
            return dynamicBindings.getLocation( pid );
        }

        return null;
    }


    ConfigurationImpl createFactoryConfiguration( String factoryPid, String location ) throws IOException
    {
        return cacheConfiguration( createConfiguration( createPid( factoryPid ), factoryPid, location ) );
    }

    /**
     * Returns a targeted configuration for the given service PID and
     * the reference target service.
     * <p>
     * A configuration returned has already been checked for visibility
     * by the bundle registering the referenced service. Additionally,
     * the configuration is also dynamically bound if needed.
     *
     * @param rawPid The raw service PID to get targeted configuration for.
     * @param target The target <code>ServiceReference</code> to get
     *      configuration for.
     * @return The best matching targeted configuration or <code>null</code>
     *      if there is no configuration at all.
     * @throwss IOException if an error occurrs reading configurations
     *      from persistence.
     */
    ConfigurationImpl getTargetedConfiguration( final String rawPid, final ServiceReference target ) throws IOException
    {
        final Bundle serviceBundle = target.getBundle();
        if ( serviceBundle != null )
        {
            // list of targeted PIDs to check
            // (StringBuffer for pre-1.5 API compatibility)
            final StringBuffer targetedPid = new StringBuffer( rawPid );
            int i = 3;
            String[] names = new String[4];
            names[i--] = targetedPid.toString();
            targetedPid.append( '|' ).append( serviceBundle.getSymbolicName() );
            names[i--] = targetedPid.toString();
            targetedPid.append( '|' ).append( TargetedPID.getBundleVersion( serviceBundle ) );
            names[i--] = targetedPid.toString();
            targetedPid.append( '|' ).append( serviceBundle.getLocation() );
            names[i--] = targetedPid.toString();

            for ( String candidate : names )
            {
                ConfigurationImpl config = getConfiguration( candidate );
                if ( config != null && !config.isDeleted() )
                {
                    // check visibility to use and dynamically bind
                    if ( canReceive( serviceBundle, config.getBundleLocation() ) )
                    {
                        config.tryBindLocation( serviceBundle.getLocation() );
                        return config;
                    }

                    // CM 1.4 / 104.13.2.2 / 104.5.3
                    // act as if there is no configuration
                    log(
                        LogService.LOG_DEBUG,
                        "Cannot use configuration {0} for {1}: No visibility to configuration bound to {2}; calling with null",
                        new Object[]
                            { config.getPid(), toString( target ), config.getBundleLocation() } );
                }
            }
        }
        else
        {
            log( LogService.LOG_INFO,
                "Service for PID {0} seems to already have been unregistered, not updating with configuration",
                new Object[]
                    { rawPid } );
        }

        // service already unregistered, nothing to do really
        return null;
    }


    /**
     * Returns the {@link ConfigurationImpl} with the given PID if
     * available in the internal cache or from any persistence manager.
     * Otherwise <code>null</code> is returned.
     *
     * @param pid The PID for which to return the configuration
     * @return The configuration or <code>null</code> if non exists
     * @throws IOException If an error occurrs reading from a persistence
     *      manager.
     */
    ConfigurationImpl getConfiguration( String pid ) throws IOException
    {
        ConfigurationImpl config = getCachedConfiguration( pid );
        if ( config != null )
        {
            log( LogService.LOG_DEBUG, "Found cached configuration {0} bound to {1}", new Object[]
                { pid, config.getBundleLocation() } );

            config.ensureFactoryConfigPersisted();

            return config;
        }

        PersistenceManager[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            if ( pmList[i].exists( pid ) )
            {
                Dictionary props = pmList[i].load( pid );
                config = new ConfigurationImpl( this, pmList[i], props );
                log( LogService.LOG_DEBUG, "Found existing configuration {0} bound to {1}", new Object[]
                    { pid, config.getBundleLocation() } );
                return cacheConfiguration( config );
            }
        }

        // neither the cache nor any persistence manager has configuration
        return null;
    }


    /**
     * Creates a regular (non-factory) configuration for the given PID
     * setting the bundle location accordingly.
     * <p>
     * This method assumes the configuration to not exist yet and will
     * create it without further checking.
     *
     * @param pid The PID of the new configuration
     * @param bundleLocation The location to set on the new configuration.
     *      This may be <code>null</code> to not bind the configuration
     *      yet.
     * @return The new configuration persisted in the first persistence
     *      manager.
     * @throws IOException If an error occurrs writing the configuration
     *      to the persistence.
     */
    ConfigurationImpl createConfiguration( String pid, String bundleLocation ) throws IOException
    {
        // check for existing (cached or persistent) configuration
        ConfigurationImpl config = getConfiguration( pid );
        if ( config != null )
        {
            return config;
        }

        // else create new configuration also setting the bundle location
        // and cache the new configuration
        config = createConfiguration( pid, null, bundleLocation );
        return cacheConfiguration( config );
    }


    ConfigurationImpl[] listConfigurations( ConfigurationAdminImpl configurationAdmin, String filterString )
        throws IOException, InvalidSyntaxException
    {
        SimpleFilter filter = null;
        if ( filterString != null )
        {
            filter = SimpleFilter.parse( filterString );
        }

        log( LogService.LOG_DEBUG, "Listing configurations matching {0}", new Object[]
            { filterString } );

        List configList = new ArrayList();

        CachingPersistenceManagerProxy[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            Enumeration configs = pmList[i].getDictionaries( filter );
            while ( configs.hasMoreElements() )
            {
                final Dictionary config = ( Dictionary ) configs.nextElement();

                // ignore non-Configuration dictionaries
                final String pid = ( String ) config.get( Constants.SERVICE_PID );
                if ( pid == null )
                {
                    continue;
                }

                // CM 1.4 / 104.13.2.3 Permission required
                if ( !configurationAdmin.hasPermission( this,
                    ( String ) config.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) ) )
                {
                    log(
                        LogService.LOG_DEBUG,
                        "Omitting configuration {0}: No permission for bundle {1} on configuration bound to {2}",
                        new Object[]
                            { pid, configurationAdmin.getBundle().getLocation(),
                                config.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) } );
                    continue;
                }

                // ensure the service.pid and returned a cached config if available
                ConfigurationImpl cfg = getCachedConfiguration( pid );
                if ( cfg == null )
                {
                    cfg = new ConfigurationImpl( this, pmList[i], config );
                }

                // FELIX-611: Ignore configuration objects without props
                if ( !cfg.isNew() )
                {
                    log( LogService.LOG_DEBUG, "Adding configuration {0}", new Object[]
                        { pid } );
                    configList.add( cfg );
                }
                else
                {
                    log( LogService.LOG_DEBUG, "Omitting configuration {0}: Is new", new Object[]
                        { pid } );
                }
            }
        }

        if ( configList.size() == 0 )
        {
            return null;
        }
        return ( ConfigurationImpl[] ) configList.toArray( new ConfigurationImpl[configList
            .size()] );
    }


    void deleted( ConfigurationImpl config )
    {
        // remove the configuration from the cache
        removeConfiguration( config );
        fireConfigurationEvent( ConfigurationEvent.CM_DELETED, config.getPidString(), config.getFactoryPidString() );
        updateThread.schedule( new DeleteConfiguration( config ) );
        log( LogService.LOG_DEBUG, "DeleteConfiguration({0}) scheduled", new Object[]
            { config.getPid() } );
    }


    void updated( ConfigurationImpl config, boolean fireEvent )
    {
        if ( fireEvent )
        {
            fireConfigurationEvent( ConfigurationEvent.CM_UPDATED, config.getPidString(), config.getFactoryPidString() );
        }
        updateThread.schedule( new UpdateConfiguration( config ) );
        log( LogService.LOG_DEBUG, "UpdateConfiguration({0}) scheduled", new Object[]
            { config.getPid() } );
    }


    void locationChanged( ConfigurationImpl config, String oldLocation )
    {
        fireConfigurationEvent( ConfigurationEvent.CM_LOCATION_CHANGED, config.getPidString(), config.getFactoryPidString() );
        if ( oldLocation != null && !config.isNew() )
        {
            updateThread.schedule( new LocationChanged( config, oldLocation ) );
            log( LogService.LOG_DEBUG, "LocationChanged({0}, {1}=>{2}) scheduled", new Object[]
                { config.getPid(), oldLocation, config.getBundleLocation() } );
        }
        else
        {
            log( LogService.LOG_DEBUG,
                "LocationChanged not scheduled for {0} (old location is null or configuration is new)", new Object[]
                    { config.getPid() } );
        }
    }


    void fireConfigurationEvent( int type, String pid, String factoryPid )
    {
        // prevent event senders
        FireConfigurationEvent asyncSender = new FireConfigurationEvent( this.configurationListenerTracker, type, pid,
            factoryPid );
        FireConfigurationEvent syncSender = new FireConfigurationEvent( this.syncConfigurationListenerTracker, type,
            pid, factoryPid );

        // send synchronous events
        if ( syncSender.hasConfigurationEventListeners() )
        {
            syncSender.run();
        }
        else
        {
            log( LogService.LOG_DEBUG, "No SynchronousConfigurationListeners to send {0} event to.", new Object[]
                { syncSender.getTypeName() } );
        }

        // schedule asynchronous events
        if ( asyncSender.hasConfigurationEventListeners() )
        {
            eventThread.schedule( asyncSender );
        }
        else
        {
            log( LogService.LOG_DEBUG, "No ConfigurationListeners to send {0} event to.", new Object[]
                { asyncSender.getTypeName() } );
        }
    }


    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.UNINSTALLED && handleBundleEvents )
        {
            final String location = event.getBundle().getLocation();

            // we only reset dynamic bindings, which are only present in
            // cached configurations, hence only consider cached configs here
            final ConfigurationImpl[] configs = getCachedConfigurations();
            for ( int i = 0; i < configs.length; i++ )
            {
                final ConfigurationImpl cfg = configs[i];
                if ( location.equals( cfg.getDynamicBundleLocation() ) )
                {
                    cfg.setDynamicBundleLocation( null, true );
                }
            }
        }
    }


    // ---------- internal -----------------------------------------------------

    private CachingPersistenceManagerProxy[] getPersistenceManagers()
    {
        int currentPmtCount = persistenceManagerTracker.getTrackingCount();
        if ( persistenceManagers == null || currentPmtCount > pmtCount )
        {

            List pmList = new ArrayList();
            CachingPersistenceManagerProxy[] pm;

            ServiceReference<?>[] refs = persistenceManagerTracker.getServiceReferences();
            if ( refs == null || refs.length == 0 )
            {
                pm = new CachingPersistenceManagerProxy[0];
            }
            else
            {
                // sort the references according to the cmRanking property
                if ( refs.length > 1 )
                {
                    Arrays.sort( refs, RankingComparator.SRV_RANKING );
                }

                // create the service array from the sorted set of referenecs
                for ( int i = 0; i < refs.length; i++ )
                {
                    Object service = persistenceManagerTracker.getService( refs[i] );
                    if ( service != null )
                    {
                        pmList.add( new CachingPersistenceManagerProxy( ( PersistenceManager ) service ) );
                    }
                }

                pm = ( CachingPersistenceManagerProxy[] ) pmList.toArray( new CachingPersistenceManagerProxy[pmList.size()] );
            }

            pmtCount = currentPmtCount;
            persistenceManagers = pm;
        }

        return persistenceManagers;
    }


    private ServiceReference getServiceReference()
    {
        ServiceRegistration reg = configurationAdminRegistration;
        if (reg != null) {
            return reg.getReference();
        }

        // probably called for firing an event during service registration
        // since we didn't get the service registration yet we use the
        // service registry to get our service reference
        BundleContext context = bundleContext;
        if ( context != null )
        {
            try
            {
                ServiceReference[] refs = context.getServiceReferences( ConfigurationAdmin.class.getName(), null );
                if ( refs != null )
                {
                    for ( int i = 0; i < refs.length; i++ )
                    {
                        if ( refs[i].getBundle().getBundleId() == context.getBundle().getBundleId() )
                        {
                            return refs[i];
                        }
                    }
                }
            }
            catch ( InvalidSyntaxException e )
            {
                // unexpected since there is no filter
            }
        }

        // service references
        return null;
    }


    /**
     * Configures the ManagedService and returns the service.pid
     * service property as a String[], which may be <code>null</code> if
     * the ManagedService does not have such a property.
     */
    /**
     * Configures the ManagedServiceFactory and returns the service.pid
     * service property as a String[], which may be <code>null</code> if
     * the ManagedServiceFactory does not have such a property.
     */
    /**
     * Schedules the configuration of the referenced service with
     * configuration for the given PID.
     *
     * @param pid The list of service PID of the configurations to be
     *      provided to the referenced service.
     * @param sr The <code>ServiceReference</code> to the service
     *      to be configured.
     * @param factory <code>true</code> If the service is considered to
     *      be a <code>ManagedServiceFactory</code>. Otherwise the service
     *      is considered to be a <code>ManagedService</code>.
     */
    public void configure( String[] pid, ServiceReference sr, final boolean factory, final ConfigurationMap<?> configs )
    {
        if ( this.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            this.log( LogService.LOG_DEBUG, "configure(ManagedService {0})", new Object[]
                { toString( sr ) } );
        }

        Runnable r;
        if ( factory )
        {
            r = new ManagedServiceFactoryUpdate( pid, sr, configs );
        }
        else
        {
            r = new ManagedServiceUpdate( pid, sr, configs );
        }
        updateThread.schedule( r );
        log( LogService.LOG_DEBUG, "[{0}] scheduled", new Object[]
            { r } );
    }


    /**
     * Factory method to create a new configuration object. The configuration
     * object returned is not stored in configuration cache and only persisted
     * if the <code>factoryPid</code> parameter is <code>null</code>.
     *
     * @param pid
     *            The PID of the new configuration object. Must not be
     *            <code>null</code>.
     * @param factoryPid
     *            The factory PID of the new configuration. Not
     *            <code>null</code> if the new configuration object belongs to a
     *            factory. The configuration object will not be persisted if
     *            this parameter is not <code>null</code>.
     * @param bundleLocation
     *            The bundle location of the bundle to which the configuration
     *            belongs or <code>null</code> if the configuration is not bound
     *            yet.
     * @return The new configuration object
     * @throws IOException
     *             May be thrown if an error occurrs persisting the new
     *             configuration object.
     */
    ConfigurationImpl createConfiguration( String pid, String factoryPid, String bundleLocation ) throws IOException
    {
        log( LogService.LOG_DEBUG, "createConfiguration({0}, {1}, {2})", new Object[]
            { pid, factoryPid, bundleLocation } );
        return new ConfigurationImpl( this, getPersistenceManagers()[0], pid, factoryPid, bundleLocation );
    }


    /**
     * Returns a list of {@link Factory} instances according to the
     * Configuration Admin 1.5 specification for targeted PIDs (Section
     * 104.3.2)
     *
     * @param rawFactoryPid The raw factory PID without any targetting.
     * @param target The <code>ServiceReference</code> of the service to
     *      be supplied with targeted configuration.
     * @return A list of {@link Factory} instances as listed above. This
     *      list will always at least include an instance for the
     *      <code>rawFactoryPid</code>. Other instances are only included
     *      if existing.
     * @throws IOException If an error occurrs reading any of the
     *      {@link Factory} instances from persistence
     */
    List<Factory> getTargetedFactories( final String rawFactoryPid, final ServiceReference target ) throws IOException
    {
        LinkedList<Factory> factories = new LinkedList<Factory>();

        final Bundle serviceBundle = target.getBundle();
        if ( serviceBundle != null )
        {
            // for pre-1.5 API compatibility
            final StringBuffer targetedPid = new StringBuffer( rawFactoryPid );
            factories.add( getOrCreateFactory( targetedPid.toString() ) );

            targetedPid.append( '|' ).append( serviceBundle.getSymbolicName() );
            Factory f = getFactory( targetedPid.toString() );
            if ( f != null )
            {
                factories.add( 0, f );
            }

            targetedPid.append( '|' ).append( TargetedPID.getBundleVersion( serviceBundle ) );
            f = getFactory( targetedPid.toString() );
            if ( f != null )
            {
                factories.add( 0, f );
            }

            targetedPid.append( '|' ).append( serviceBundle.getLocation() );
            f = getFactory( targetedPid.toString() );
            if ( f != null )
            {
                factories.add( 0, f );
            }
        }

        return factories;
    }


    /**
     * Gets the factory with the exact identifier from the cached or from
     * the persistence managers. If no factory exists already one is
     * created and cached.
     *
     * @param factoryPid The PID of the {@link Factory} to return
     * @return The existing or newly created {@link Factory}
     * @throws IOException If an error occurrs reading the factory from
     *      a {@link PersistenceManager}
     */
    Factory getOrCreateFactory( String factoryPid ) throws IOException
    {
        Factory factory = getFactory( factoryPid );
        if ( factory != null )
        {
            return factory;
        }

        return createFactory( factoryPid );
    }


    /**
     * Gets the factory with the exact identifier from the cached or from
     * the persistence managers. If no factory exists <code>null</code>
     * is returned.
     *
     * @param factoryPid The PID of the {@link Factory} to return
     * @return The existing {@link Factory} or <code>null</code>
     * @throws IOException If an error occurrs reading the factory from
     *      a {@link PersistenceManager}
     */
    Factory getFactory( String factoryPid ) throws IOException
    {
        // check for cached factory
        Factory factory = getCachedFactory( factoryPid );
        if ( factory != null )
        {
            return factory;
        }

        // try to load factory from persistence
        PersistenceManager[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            if ( Factory.exists( pmList[i], factoryPid ) )
            {
                factory = Factory.load( this, pmList[i], factoryPid );
                cacheFactory( factory );
                return factory;
            }
        }

        // no existing factory
        return null;
    }


    /**
     * Creates a new factory with the given <code>factoryPid</code>.
     */
    Factory createFactory( String factoryPid )
    {
        Factory factory = new Factory( this, getPersistenceManagers()[0], factoryPid );
        cacheFactory( factory );
        return factory;
    }


    /**
     * Calls the registered configuration plugins on the given configuration
     * properties from the given configuration object.
     * <p>
     * The plugins to be called are selected as <code>ConfigurationPlugin</code>
     * services registered with a <code>cm.target</code> property set to
     * <code>*</code> or the factory PID of the configuration (for factory
     * configurations) or the PID of the configuration (for non-factory
     * configurations).
     *
     * @param props The configuraiton properties run through the registered
     *          ConfigurationPlugin services. This must not be
     *          <code>null</code>.
     * @param sr The service reference of the managed service (factory) which
     *          is to be updated with configuration
     * @param configPid The PID of the configuration object whose properties
     *          are to be augmented
     * @param factoryPid the factory PID of the configuration object whose
     *          properties are to be augmented. This is non-<code>null</code>
     *          only for a factory configuration.
     */
    public void callPlugins( final Dictionary props, final ServiceReference sr, final String configPid,
        final String factoryPid )
    {
        ServiceReference<?>[] plugins = null;
        try
        {
            final String targetPid = (factoryPid == null) ? configPid : factoryPid;
            String filter = "(|(!(cm.target=*))(cm.target=" + targetPid + "))";
            plugins = bundleContext.getServiceReferences( ConfigurationPlugin.class.getName(), filter );
        }
        catch ( InvalidSyntaxException ise )
        {
            // no filter, no exception ...
        }

        // abort early if there are no plugins
        if ( plugins == null || plugins.length == 0 )
        {
            return;
        }

        // sort the plugins by their service.cmRanking
        if ( plugins.length > 1 )
        {
            Arrays.sort( plugins, RankingComparator.CM_RANKING );
        }

        // call the plugins in order
        for ( int i = 0; i < plugins.length; i++ )
        {
            ServiceReference<?> pluginRef = plugins[i];
            ConfigurationPlugin plugin = ( ConfigurationPlugin ) bundleContext.getService( pluginRef );
            if ( plugin != null )
            {
                // if cmRanking is below 0 or above 1000, ignore modifications from the plugin
                boolean ignore = false;
                Object rankObj = pluginRef.getProperty( ConfigurationPlugin.CM_RANKING );
                if ( rankObj instanceof Integer )
                {
                    final int ranking = ( ( Integer ) rankObj ).intValue();
                    ignore = (ranking < 0 ) || (ranking > 1000);
                }

                try
                {
                    plugin.modifyConfiguration( sr, ignore ? CaseInsensitiveDictionary.unmodifiable(props) : props );
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem calling configuration plugin {0}", new Object[]
                        { toString( pluginRef ), t } );
                }
                finally
                {
                    // ensure ungetting the plugin
                    bundleContext.ungetService( pluginRef );
                }
                ConfigurationImpl.setAutoProperties( props, configPid, factoryPid );
            }
        }
    }


    /**
     * Creates a PID for the given factoryPid
     *
     * @param factoryPid
     * @return
     */
    private static String createPid( String factoryPid )
    {
        Random ng = numberGenerator;
        if ( ng == null )
        {
            // FELIX-2771 Secure Random not available on Mika
            try
            {
                ng = new SecureRandom();
            }
            catch ( Throwable t )
            {
                // fall back to Random
                ng = new Random();
            }
        }

        byte[] randomBytes = new byte[16];
        ng.nextBytes( randomBytes );
        randomBytes[6] &= 0x0f; /* clear version */
        randomBytes[6] |= 0x40; /* set to version 4 */
        randomBytes[8] &= 0x3f; /* clear variant */
        randomBytes[8] |= 0x80; /* set to IETF variant */

        StringBuffer buf = new StringBuffer( factoryPid.length() + 1 + 36 );

        // prefix the new pid with the factory pid
        buf.append( factoryPid ).append( "." );

        // serialize the UUID into the buffer
        for ( int i = 0; i < randomBytes.length; i++ )
        {

            if ( i == 4 || i == 6 || i == 8 || i == 10 )
            {
                buf.append( '-' );
            }

            int val = randomBytes[i] & 0xff;
            buf.append( Integer.toHexString( val >> 4 ) );
            buf.append( Integer.toHexString( val & 0xf ) );
        }

        return buf.toString();
    }


    public boolean isLogEnabled( int level )
    {
        return level <= logLevel;
    }


    public void log( int level, String format, Object[] args )
    {
        if ( isLogEnabled( level ) )
        {
            Throwable throwable = null;
            String message = format;

            if ( args != null && args.length > 0 )
            {
                if ( args[args.length - 1] instanceof Throwable )
                {
                    throwable = ( Throwable ) args[args.length - 1];
                }
                message = MessageFormat.format( format, args );
            }

            log( level, message, throwable );
        }
    }


    public void log( int level, String message, Throwable t )
    {
        // log using the LogService if available
        Object log = logTracker.getService();
        if ( log != null )
        {
            ( ( LogService ) log ).log( getServiceReference(), level, message, t );
            return;
        }

        // Otherwise only log if more serious than the configured level
        if ( isLogEnabled( level ) )
        {
            String code;
            switch ( level )
            {
                case LogService.LOG_INFO:
                    code = "*INFO *";
                    break;

                case LogService.LOG_WARNING:
                    code = "*WARN *";
                    break;

                case LogService.LOG_ERROR:
                    code = "*ERROR*";
                    break;

                case LogService.LOG_DEBUG:
                default:
                    code = "*DEBUG*";
            }

            System.err.println( code + " " + message );
            if ( t != null )
            {
                t.printStackTrace( System.err );
            }
        }
    }


    public static String toString( ServiceReference ref )
    {
        String[] ocs = ( String[] ) ref.getProperty( "objectClass" );
        StringBuffer buf = new StringBuffer( "[" );
        for ( int i = 0; i < ocs.length; i++ )
        {
            buf.append( ocs[i] );
            if ( i < ocs.length - 1 )
                buf.append( ", " );
        }

        buf.append( ", id=" ).append( ref.getProperty( Constants.SERVICE_ID ) );

        Bundle provider = ref.getBundle();
        if ( provider != null )
        {
            buf.append( ", bundle=" ).append( provider.getBundleId() );
            buf.append( '/' ).append( provider.getLocation() );
        }
        else
        {
            buf.append( ", unregistered" );
        }

        buf.append( "]" );
        return buf.toString();
    }


    /**
     * Checks whether the bundle is allowed to receive the configuration
     * with the given location binding.
     * <p>
     * This method implements the logic defined CM 1.4 / 104.4.1:
     * <ul>
     * <li>If the location is <code>null</code> (the configuration is not
     * bound yet), assume the bundle is allowed</li>
     * <li>If the location is a single location (no leading "?"), require
     * the bundle's location to match</li>
     * <li>If the location is a multi-location (leading "?"), assume the
     * bundle is allowed if there is no security manager. If there is a
     * security manager, check whether the bundle has "target" permission
     * on this location.</li>
     * </ul>
     */
    boolean canReceive( final Bundle bundle, final String location )
    {
        if ( location == null )
        {
            log( LogService.LOG_DEBUG, "canReceive=true; bundle={0}; configuration=(unbound)", new Object[]
                { bundle.getLocation() } );
            return true;
        }
        else if ( location.startsWith( "?" ) )
        {
            // multi-location
            if ( System.getSecurityManager() != null )
            {
                final boolean hasPermission = bundle.hasPermission( new ConfigurationPermission( location,
                    ConfigurationPermission.TARGET ) );
                log( LogService.LOG_DEBUG, "canReceive={0}: bundle={1}; configuration={2} (SecurityManager check)",
                    new Object[]
                        { new Boolean( hasPermission ), bundle.getLocation(), location } );
                return hasPermission;
            }

            log( LogService.LOG_DEBUG, "canReceive=true; bundle={0}; configuration={1} (no SecurityManager)",
                new Object[]
                    { bundle.getLocation(), location } );
            return true;
        }
        else
        {
            // single location, must match
            final boolean hasPermission = location.equals( bundle.getLocation() );
            log( LogService.LOG_DEBUG, "canReceive={0}: bundle={1}; configuration={2}", new Object[]
                { new Boolean( hasPermission ), bundle.getLocation(), location } );
            return hasPermission;
        }
    }


    // ---------- inner classes

    /**
     * The <code>ManagedServiceUpdate</code> updates a freshly registered
     * <code>ManagedService</code> with a specific configuration. If a
     * ManagedService is registered with multiple PIDs an instance of this
     * class is used for each registered PID.
     */
    private class ManagedServiceUpdate implements Runnable
    {
        private final String[] pids;

        private final ServiceReference sr;

        private final ConfigurationMap<?> configs;


        ManagedServiceUpdate( String[] pids, ServiceReference sr, ConfigurationMap<?> configs )
        {
            this.pids = pids;
            this.sr = sr;
            this.configs = configs;
        }


        public void run()
        {
            for ( String pid : this.pids )
            {
                try
                {
                    final ConfigurationImpl config = getTargetedConfiguration( pid, this.sr );
                    provide( pid, config );
                }
                catch ( IOException ioe )
                {
                    log( LogService.LOG_ERROR, "Error loading configuration for {0}", new Object[]
                        { pid, ioe } );
                }
                catch ( Exception e )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem providing configuration {0} to service {1}",
                        new Object[]
                            { pid, ConfigurationManager.toString( this.sr ), e } );
                }
            }
        }


        private void provide(final String servicePid, final ConfigurationImpl config)
        {
            // check configuration
            final TargetedPID configPid;
            final Dictionary properties;
            final long revision;
            if ( config != null )
            {
                synchronized ( config )
                {
                    configPid = config.getPid();
                    properties = config.getProperties( true );
                    revision = config.getRevision();
                }
            }
            else
            {
                // 104.5.3 ManagedService.updated must be called with null
                // if no configuration is available
                configPid = new TargetedPID( servicePid );
                properties = null;
                revision = -1;
            }

            log( LogService.LOG_DEBUG, "Updating service {0} with configuration {1}@{2}", new Object[]
                { servicePid, configPid, new Long( revision ) } );

            managedServiceTracker.provideConfiguration( sr, configPid, null, properties, revision, this.configs );
        }

        @Override
        public String toString()
        {
            return "ManagedService Update: pid=" + Arrays.asList( pids );
        }
    }

    /**
     * The <code>ManagedServiceFactoryUpdate</code> updates a freshly
     * registered <code>ManagedServiceFactory</code> with a specific
     * configuration. If a ManagedServiceFactory is registered with
     * multiple PIDs an instance of this class is used for each registered
     * PID.
     */
    private class ManagedServiceFactoryUpdate implements Runnable
    {
        private final String[] factoryPids;

        private final ServiceReference sr;

        private final ConfigurationMap<?> configs;


        ManagedServiceFactoryUpdate( String[] factoryPids, ServiceReference sr, final ConfigurationMap<?> configs )
        {
            this.factoryPids = factoryPids;
            this.sr = sr;
            this.configs = configs;
        }


        public void run()
        {
            for ( String factoryPid : this.factoryPids )
            {

                List<Factory> factories = null;
                try
                {
                    factories = getTargetedFactories( factoryPid, sr );
                    for ( Factory factory : factories )
                    {
                        for ( Iterator pi = factory.getPIDs().iterator(); pi.hasNext(); )
                        {
                            final String pid = ( String ) pi.next();
                            ConfigurationImpl cfg;
                            try
                            {
                                cfg = getConfiguration( pid );
                            }
                            catch ( IOException ioe )
                            {
                                log( LogService.LOG_ERROR, "Error loading configuration for {0}", new Object[]
                                    { pid, ioe } );
                                continue;
                            }

                            // sanity check on the configuration
                            if ( cfg == null )
                            {
                                log( LogService.LOG_ERROR,
                                    "Configuration {0} referred to by factory {1} does not exist", new Object[]
                                        { pid, factoryPid } );
                                factory.removePID( pid );
                                factory.storeSilently();
                                continue;
                            }
                            else if ( cfg.isNew() )
                            {
                                // Configuration has just been created but not yet updated
                                // we currently just ignore it and have the update mechanism
                                // provide the configuration to the ManagedServiceFactory
                                // As of FELIX-612 (not storing new factory configurations)
                                // this should not happen. We keep this for added stability
                                // but raise the logging level to error.
                                log( LogService.LOG_ERROR, "Ignoring new configuration pid={0}", new Object[]
                                    { pid } );
                                continue;
                            }

                            /*
                             * this code would catch targeted factory PIDs;
                             * since this is not expected any way, we can
                             * leave this out
                             */
                            /*
                            else if ( !factoryPid.equals( cfg.getFactoryPid() ) )
                            {
                                log( LogService.LOG_ERROR,
                                    "Configuration {0} referred to by factory {1} seems to belong to factory {2}",
                                    new Object[]
                                        { pid, factoryPid, cfg.getFactoryPid() } );
                                factory.removePID( pid );
                                factory.storeSilently();
                                continue;
                            }
                            */

                            provide( factoryPid, cfg );
                        }
                    }
                }
                catch ( IOException ioe )
                {
                    log( LogService.LOG_ERROR, "Cannot get factory mapping for factory PID {0}", new Object[]
                        { factoryPid, ioe } );
                }
            }
        }


        private void provide(final String factoryPid, final ConfigurationImpl config) {

            final Dictionary rawProperties;
            final long revision;
            synchronized ( config )
            {
                rawProperties = config.getProperties( true );
                revision = config.getRevision();
            }

            log( LogService.LOG_DEBUG, "Updating service {0} with configuration {1}/{2}@{3}", new Object[]
                { factoryPid, config.getFactoryPid(), config.getPid(), new Long( revision ) } );

            // CM 1.4 / 104.13.2.1
            final Bundle serviceBundle = this.sr.getBundle();
            if ( serviceBundle == null )
            {
                log(
                    LogService.LOG_INFO,
                    "ManagedServiceFactory for factory PID {0} seems to already have been unregistered, not updating with factory",
                    new Object[]
                        { factoryPid } );
                return;
            }

            if ( !canReceive( serviceBundle, config.getBundleLocation() ) )
            {
                log( LogService.LOG_ERROR,
                    "Cannot use configuration {0} for {1}: No visibility to configuration bound to {2}",
                    new Object[]
                        { config.getPid(), ConfigurationManager.toString( sr ), config.getBundleLocation() } );

                // no service, really, bail out
                return;
            }

            // 104.4.2 Dynamic Binding
            config.tryBindLocation( serviceBundle.getLocation() );

            // update the service with the configuration (if non-null)
            if ( rawProperties != null )
            {
                log( LogService.LOG_DEBUG, "{0}: Updating configuration pid={1}", new Object[]
                    { ConfigurationManager.toString( sr ), config.getPid() } );
                managedServiceFactoryTracker.provideConfiguration( sr, config.getPid(), config.getFactoryPid(),
                    rawProperties, revision, this.configs );
            }
        }


        @Override
        public String toString()
        {
            return "ManagedServiceFactory Update: factoryPid=" + Arrays.asList( this.factoryPids );
        }
    }

    private abstract class ConfigurationProvider<T> implements Runnable
    {

        protected final ConfigurationImpl config;
        protected final long revision;
        protected final Dictionary<String, ?> properties;
        private BaseTracker<T> helper;


        protected ConfigurationProvider( final ConfigurationImpl config )
        {
            synchronized ( config )
            {
                this.config = config;
                this.revision = config.getRevision();
                this.properties = config.getProperties( true );
            }
        }


        protected TargetedPID getTargetedServicePid()
        {
            final TargetedPID factoryPid = this.config.getFactoryPid();
            if ( factoryPid != null )
            {
                return factoryPid;
            }
            return this.config.getPid();
        }


        protected BaseTracker<T> getHelper()
        {
            if ( this.helper == null )
            {
                this.helper = ( BaseTracker<T> ) ( ( this.config.getFactoryPid() == null ) ? ConfigurationManager.this.managedServiceTracker
                    : ConfigurationManager.this.managedServiceFactoryTracker );
            }
            return this.helper;
        }


        protected boolean provideReplacement( ServiceReference<T> sr )
        {
            if ( this.config.getFactoryPid() == null )
            {
                try
                {
                    final String configPidString = this.getHelper().getServicePid( sr, this.config.getPid() );
                    if (configPidString == null) {
                        return false; // The managed service is not registered anymore in the OSGi service registry.
                    }
                    final ConfigurationImpl rc = getTargetedConfiguration( configPidString, sr );
                    if ( rc != null )
                    {
                        final TargetedPID configPid;
                        final Dictionary properties;
                        final long revision;
                        synchronized ( rc )
                        {
                            configPid = rc.getPid();
                            properties = rc.getProperties( true );
                            revision = rc.getRevision();
                        }

                        this.getHelper().provideConfiguration( sr, configPid, null, properties, -revision, null );

                        return true;
                    }
                }
                catch ( IOException ioe )
                {
                    log( LogService.LOG_ERROR, "Error loading configuration for {0}", new Object[]
                        { this.config.getPid(), ioe } );
                }
                catch ( Exception e )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem providing configuration {0} to service {1}",
                        new Object[]
                            { this.config.getPid(), ConfigurationManager.toString( sr ), e } );
                }
            }

            // factory or no replacement available
            return false;
        }
    }

    /**
     * The <code>UpdateConfiguration</code> is used to update
     * <code>ManagedService[Factory]</code> services with the configuration
     * they are subscribed to. This may cause the configuration to be
     * supplied to multiple services.
     */
    private class UpdateConfiguration extends ConfigurationProvider
    {

        UpdateConfiguration( final ConfigurationImpl config )
        {
            super( config );
        }


        public void run()
        {
            log( LogService.LOG_DEBUG, "Updating configuration {0} to revision #{1}", new Object[]
                { config.getPid(), new Long( revision ) } );

            final List<ServiceReference<?>> srList = this.getHelper().getServices( getTargetedServicePid() );
            if ( !srList.isEmpty() )
            {
                // optionally bind dynamically to the first service
                Bundle bundle = srList.get(0).getBundle();
                if (bundle == null) {
                    log( LogService.LOG_DEBUG,
                        "Service {0} seems to be unregistered concurrently (not providing configuration)",
                        new Object[]
                            { ConfigurationManager.toString( srList.get(0) ) } );
                    return;
                }
                config.tryBindLocation( bundle.getLocation() );

                final String configBundleLocation = config.getBundleLocation();

                // provide configuration to all services from the
                // correct bundle
                for (ServiceReference<?> ref : srList)
                {
                    final Bundle refBundle = ref.getBundle();
                    if ( refBundle == null )
                    {
                        log( LogService.LOG_DEBUG,
                            "Service {0} seems to be unregistered concurrently (not providing configuration)",
                            new Object[]
                                { ConfigurationManager.toString( ref ) } );
                    }
                    else if ( canReceive( refBundle, configBundleLocation ) )
                    {
                        this.getHelper().provideConfiguration( ref, this.config.getPid(), this.config.getFactoryPid(),
                            this.properties, this.revision, null );
                    }
                    else
                    {
                        // CM 1.4 / 104.13.2.2
                        log( LogService.LOG_ERROR,
                            "Cannot use configuration {0} for {1}: No visibility to configuration bound to {2}",
                            new Object[]
                                { config.getPid(), ConfigurationManager.toString( ref ), configBundleLocation } );
                    }

                }
            }
            else if ( isLogEnabled( LogService.LOG_DEBUG ) )
            {
                log( LogService.LOG_DEBUG, "No ManagedService[Factory] registered for updates to configuration {0}",
                    new Object[]
                        { config.getPid() } );
            }
        }


        @Override
        public String toString()
        {
            return "Update: pid=" + config.getPid();
        }
    }


    /**
     * The <code>DeleteConfiguration</code> class is used to inform
     * <code>ManagedService[Factory]</code> services of a configuration
     * being deleted.
     */
    private class DeleteConfiguration extends ConfigurationProvider
    {

        private final String configLocation;


        DeleteConfiguration( ConfigurationImpl config )
        {
            /*
             * NOTE: We keep the configuration because it might be cleared just
             * after calling this method. The pid and factoryPid fields are
             * final and cannot be reset.
             */
            super(config);
            this.configLocation = config.getBundleLocation();
        }


        public void run()
        {
            List<ServiceReference<?>> srList = this.getHelper().getServices( getTargetedServicePid() );
            if ( !srList.isEmpty() )
            {
                for (ServiceReference<?> sr : srList)
                {
                    final Bundle srBundle = sr.getBundle();
                    if ( srBundle == null )
                    {
                        log( LogService.LOG_DEBUG,
                            "Service {0} seems to be unregistered concurrently (not removing configuration)",
                            new Object[]
                                { ConfigurationManager.toString( sr ) } );
                    }
                    else if ( canReceive( srBundle, configLocation ) )
                    {
                        // revoke configuration unless a replacement
                        // configuration can be provided
                        if ( !this.provideReplacement( sr ) )
                        {
                            this.getHelper().removeConfiguration( sr, this.config.getPid(), this.config.getFactoryPid() );
                        }
                    }
                    else
                    {
                        // CM 1.4 / 104.13.2.2
                        log( LogService.LOG_ERROR,
                            "Cannot remove configuration {0} for {1}: No visibility to configuration bound to {2}",
                            new Object[]
                                { config.getPid(), ConfigurationManager.toString( sr ), configLocation } );
                    }
                }
            }

            final TargetedPID factoryPid = config.getFactoryPid();
            if ( factoryPid != null )
            {
                // remove the pid from the factory
                final String pid = config.getPidString();
                try
                {
                    Factory factory = getOrCreateFactory( factoryPid.toString() );
                    factory.removePID( pid );
                    factory.store();
                }
                catch ( IOException ioe )
                {
                    log( LogService.LOG_ERROR, "Failed removing {0} from the factory {1}", new Object[]
                        { pid, factoryPid, ioe } );
                }
            }
        }

        @Override
        public String toString()
        {
            return "Delete: pid=" + config.getPid();
        }
    }

    private class LocationChanged extends ConfigurationProvider
    {
        private final String oldLocation;


        LocationChanged( ConfigurationImpl config, String oldLocation )
        {
            super( config );
            this.oldLocation = oldLocation;
        }


        public void run()
        {
            List<ServiceReference<?>> srList = this.getHelper().getServices( getTargetedServicePid() );
            if ( !srList.isEmpty() )
            {
                for (final ServiceReference<?> sr : srList)
                {
                    final Bundle srBundle = sr.getBundle();
                    if ( srBundle == null )
                    {
                        log( LogService.LOG_DEBUG,
                            "Service {0} seems to be unregistered concurrently (not processing)", new Object[]
                                { ConfigurationManager.toString( sr ) } );
                        continue;
                    }

                    final boolean wasVisible = canReceive( srBundle, oldLocation );
                    final boolean isVisible = canReceive( srBundle, config.getBundleLocation() );

                    // make sure the config is dynamically bound to the first
                    // service if the config has been unbound causing this update
                    if ( isVisible )
                    {
                        config.tryBindLocation( srBundle.getLocation() );
                    }

                    if ( wasVisible && !isVisible )
                    {
                        // revoke configuration unless a replacement
                        // configuration can be provided
                        if ( !this.provideReplacement( sr ) )
                        {
                            this.getHelper().removeConfiguration( sr, this.config.getPid(), this.config.getFactoryPid() );
                            log( LogService.LOG_DEBUG, "Configuration {0} revoked from {1} (no more visibility)",
                                new Object[]
                                    { config.getPid(), ConfigurationManager.toString( sr ) } );
                        }
                    }
                    else if ( !wasVisible && isVisible )
                    {
                        // call updated method
                        this.getHelper().provideConfiguration( sr, this.config.getPid(), this.config.getFactoryPid(),
                            this.properties, this.revision, null );
                        log( LogService.LOG_DEBUG, "Configuration {0} provided to {1} (new visibility)", new Object[]
                            { config.getPid(), ConfigurationManager.toString( sr ) } );
                    }
                    else
                    {
                        // same visibility as before
                        log( LogService.LOG_DEBUG, "Unmodified visibility to configuration {0} for {1}", new Object[]
                            { config.getPid(), ConfigurationManager.toString( sr ) } );
                    }
                }
            }
        }


        @Override
        public String toString()
        {
            return "Location Changed (pid=" + config.getPid() + "): " + oldLocation + " ==> "
                + config.getBundleLocation();
        }
    }

    private class FireConfigurationEvent implements Runnable
    {
        private final int type;

        private final String pid;

        private final String factoryPid;

        private final ServiceReference[] listenerReferences;

        private final ConfigurationListener[] listeners;

        private final Bundle[] listenerProvider;

        private ConfigurationEvent event;

        private FireConfigurationEvent( final ServiceTracker listenerTracker, final int type, final String pid, final String factoryPid)
        {
            this.type = type;
            this.pid = pid;
            this.factoryPid = factoryPid;

            final ServiceReference[] srs = listenerTracker.getServiceReferences();
            if ( srs == null || srs.length == 0 )
            {
                this.listenerReferences = null;
                this.listeners = null;
                this.listenerProvider = null;
            }
            else
            {
                this.listenerReferences = srs;
                this.listeners = new ConfigurationListener[srs.length];
                this.listenerProvider = new Bundle[srs.length];
                for ( int i = 0; i < srs.length; i++ )
                {
                    this.listeners[i] = ( ConfigurationListener ) listenerTracker.getService( srs[i] );
                    this.listenerProvider[i] = srs[i].getBundle();
                }
            }
        }


        boolean hasConfigurationEventListeners()
        {
            return this.listenerReferences != null;
        }


        String getTypeName()
        {
            switch ( type )
            {
                case ConfigurationEvent.CM_DELETED:
                    return "CM_DELETED";
                case ConfigurationEvent.CM_UPDATED:
                    return "CM_UPDATED";
                case ConfigurationEvent.CM_LOCATION_CHANGED:
                    return "CM_LOCATION_CHANGED";
                default:
                    return "<UNKNOWN(" + type + ")>";
            }
        }


        public void run()
        {
            for ( int i = 0; i < listeners.length; i++ )
            {
                sendEvent( i );
            }
        }


        @Override
        public String toString()
        {
            return "Fire ConfigurationEvent: pid=" + pid;
        }


        private ConfigurationEvent getConfigurationEvent()
        {
            if ( event == null )
            {
                this.event = new ConfigurationEvent( getServiceReference(), type, factoryPid, pid );
            }
            return event;
        }


        private void sendEvent( final int serviceIndex )
        {
            if ( (listenerProvider[serviceIndex].getState() & (Bundle.ACTIVE | Bundle.STARTING)) > 0
                    && this.listeners[serviceIndex] != null )
            {
                log( LogService.LOG_DEBUG, "Sending {0} event for {1} to {2}", new Object[]
                    { getTypeName(), pid, ConfigurationManager.toString( listenerReferences[serviceIndex] ) } );

                try
                {
                    listeners[serviceIndex].configurationEvent( getConfigurationEvent() );
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem delivering configuration event to {0}", new Object[]
                        { ConfigurationManager.toString( listenerReferences[serviceIndex] ), t } );
                }
                finally
                {
                    this.listeners[serviceIndex] = null;
                }
            }
        }
    }
}

