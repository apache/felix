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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.impl.helper.ConfigAdminTracker;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.manager.ExtendedServiceEvent;
import org.apache.felix.scr.impl.manager.ExtendedServiceListener;
import org.apache.felix.scr.impl.manager.RegionConfigurationSupport;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The BundleComponentActivator is helper class to load and unload Components of
 * a single bundle. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 */
public class BundleComponentActivator implements ComponentActivator
{

    // global component registration
    private final ComponentRegistry m_componentRegistry;

    // The bundle owning the registered component
    private final Bundle m_bundle;

    // The bundle context owning the registered component
    private final BundleContext m_context;

    // This is a list of component holders that belong to a particular bundle
    private final List<ComponentHolder<?>> m_holders = new ArrayList<ComponentHolder<?>>();

    // The Configuration Admin tracker providing configuration for components
    private final ServiceTracker<LogService, LogService> m_logService;

    // thread acting upon configurations
    private final ComponentActorThread m_componentActor;

    // true as long as the dispose method is not called
    private final AtomicBoolean m_active = new AtomicBoolean( true );
    private final CountDownLatch m_closeLatch = new CountDownLatch( 1 );

    // the configuration
    private final ScrConfiguration m_configuration;

    private final ConfigAdminTracker configAdminTracker;

    private final Map<String, ListenerInfo> listenerMap = new HashMap<String, ListenerInfo>();

    private final SimpleLogger m_logger;

    private static class ListenerInfo implements ServiceListener
    {
        private Map<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> filterMap = new HashMap<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>>();

        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference<?> ref = event.getServiceReference();
            ExtendedServiceEvent extEvent = null;
            ExtendedServiceEvent endMatchEvent = null;
            Map<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> filterMap;
            synchronized ( this )
            {
                filterMap = this.filterMap;
            }
            for ( Map.Entry<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> entry : filterMap.entrySet() )
            {
                Filter filter = entry.getKey();
                if ( filter == null || filter.match( ref ) )
                {
                    if ( extEvent == null )
                    {
                        extEvent = new ExtendedServiceEvent( event );
                    }
                    for ( ExtendedServiceListener<ExtendedServiceEvent> forwardTo : entry.getValue() )
                    {
                        forwardTo.serviceChanged( extEvent );
                    }
                }
                else if ( event.getType() == ServiceEvent.MODIFIED )
                {
                    if ( endMatchEvent == null )
                    {
                        endMatchEvent = new ExtendedServiceEvent( ServiceEvent.MODIFIED_ENDMATCH, ref );
                    }
                    for ( ExtendedServiceListener<ExtendedServiceEvent> forwardTo : entry.getValue() )
                    {
                        forwardTo.serviceChanged( endMatchEvent );
                    }
                }
            }
            if ( extEvent != null )
            {
                extEvent.activateManagers();
            }
            if ( endMatchEvent != null )
            {
                endMatchEvent.activateManagers();
            }
        }

        public synchronized void add(Filter filter, ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            filterMap = new HashMap<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>>( filterMap );
            List<ExtendedServiceListener<ExtendedServiceEvent>> listeners = filterMap.get( filter );
            if ( listeners == null )
            {
                listeners = Collections.<ExtendedServiceListener<ExtendedServiceEvent>> singletonList( listener );
            }
            else
            {
                listeners = new ArrayList<ExtendedServiceListener<ExtendedServiceEvent>>( listeners );
                listeners.add( listener );
            }
            filterMap.put( filter, listeners );
        }

        public synchronized boolean remove(Filter filter, ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            List<ExtendedServiceListener<ExtendedServiceEvent>> listeners = filterMap.get( filter );
            if ( listeners != null )
            {
                filterMap = new HashMap<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>>( filterMap );
                listeners = new ArrayList<ExtendedServiceListener<ExtendedServiceEvent>>( listeners );
                listeners.remove( listener );
                if ( listeners.isEmpty() )
                {
                    filterMap.remove( filter );
                }
                else
                {
                    filterMap.put( filter, listeners );
                }
            }
            return filterMap.isEmpty();
        }
    }

    public void addServiceListener(String classNameFilter, Filter eventFilter,
        ExtendedServiceListener<ExtendedServiceEvent> listener)
    {
        ListenerInfo listenerInfo;
        synchronized ( listenerMap )
        {
            log( LogService.LOG_DEBUG, "classNameFilter: " + classNameFilter + " event filter: " + eventFilter, null,
                null, null );
            listenerInfo = listenerMap.get( classNameFilter );
            if ( listenerInfo == null )
            {
                listenerInfo = new ListenerInfo();
                listenerMap.put( classNameFilter, listenerInfo );
                try
                {
                    m_context.addServiceListener( listenerInfo, classNameFilter );
                }
                catch ( InvalidSyntaxException e )
                {
                    throw (IllegalArgumentException) new IllegalArgumentException(
                        "invalid class name filter" ).initCause( e );
                }
            }
        }
        listenerInfo.add( eventFilter, listener );
    }

    public void removeServiceListener(String className, Filter filter,
        ExtendedServiceListener<ExtendedServiceEvent> listener)
    {
        synchronized ( listenerMap )
        {
            ListenerInfo listenerInfo = listenerMap.get( className );
            if ( listenerInfo != null )
            {
                if ( listenerInfo.remove( filter, listener ) )
                {
                    listenerMap.remove( className );
                    m_context.removeServiceListener( listenerInfo );
                }
            }
        }
    }

    /**
     * Called upon starting of the bundle. This method invokes initialize() which
     * parses the metadata and creates the holders
     *
     * @param componentRegistry The <code>ComponentRegistry</code> used to
     *      register components with to ensure uniqueness of component names
     *      and to ensure configuration updates.
     * @param   context  The bundle context owning the components
     *
     * @throws ComponentException if any error occurrs initializing this class
     */
    public BundleComponentActivator(SimpleLogger logger, ComponentRegistry componentRegistry, ComponentActorThread componentActor, BundleContext context, ScrConfiguration configuration) throws ComponentException
    {
        // keep the parameters for later
        m_logger = logger;
        m_componentRegistry = componentRegistry;
        m_componentActor = componentActor;
        m_context = context;
        m_bundle = context.getBundle();

        // have the LogService handy (if available)
        m_logService = new ServiceTracker<LogService, LogService>( context, Activator.LOGSERVICE_CLASS, null );
        m_logService.open();
        m_configuration = configuration;

        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] active",
            new Object[] { m_bundle.getBundleId() }, null, null, null );

        // Get the Metadata-Location value from the manifest
        String descriptorLocations = (String) m_bundle.getHeaders().get( "Service-Component" );
        if ( descriptorLocations == null )
        {
            throw new ComponentException( "Service-Component entry not found in the manifest" );
        }

        initialize( descriptorLocations );
        ConfigAdminTracker tracker = null;
        for ( ComponentHolder<?> holder : m_holders )
        {
            if ( !holder.getComponentMetadata().isConfigurationIgnored() )
            {
                tracker = new ConfigAdminTracker( this );
                break;
            }
        }
        configAdminTracker = tracker;
    }

    /**
     * Gets the MetaData location, parses the meta data and requests the processing
     * of binder instances
     *
     * @param descriptorLocations A comma separated list of locations of
     *      component descriptors. This must not be <code>null</code>.
     *
     * @throws IllegalStateException If the bundle has already been uninstalled.
     */
    protected void initialize(String descriptorLocations)
    {
        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] descriptor locations {1}",
            new Object[] { m_bundle.getBundleId(), descriptorLocations }, null, null, null );

        // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle
        StringTokenizer st = new StringTokenizer( descriptorLocations, ", " );

        while ( st.hasMoreTokens() )
        {
            String descriptorLocation = st.nextToken();

            URL[] descriptorURLs = findDescriptors( m_bundle, descriptorLocation );
            if ( descriptorURLs.length == 0 )
            {
                // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
                // fragments, SCR must log an error message with the Log Service, if present, and continue.
                log( LogService.LOG_ERROR, "Component descriptor entry ''{0}'' not found",
                    new Object[] { descriptorLocation }, null, null, null );
                continue;
            }

            // load from the descriptors
            for ( URL descriptorURL : descriptorURLs )
            {
                loadDescriptor( descriptorURL );
            }
        }
    }

    /**
     * Called outside the constructor so that the m_managers field is completely initialized.
     * A component might possibly start a thread to enable other components, which could access m_managers
     */
    void initialEnable()
    {
        //enable all the enabled components
        for ( ComponentHolder<?> componentHolder : m_holders )
        {
            log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] May enable component holder {1}",
                new Object[] { m_bundle.getBundleId(), componentHolder.getComponentMetadata().getName() }, null, null,
                null );

            if ( componentHolder.getComponentMetadata().isEnabled() )
            {
                log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] Enabling component holder {1}",
                    new Object[] { m_bundle.getBundleId(), componentHolder.getComponentMetadata().getName() }, null,
                    null, null );

                try
                {
                    componentHolder.enableComponents( false );
                }
                catch ( Throwable t )
                {
                    // caught on unhandled RuntimeException or Error
                    // (e.g. ClassDefNotFoundError)

                    // make sure the component is properly disabled, just in case
                    try
                    {
                        componentHolder.disableComponents( false );
                    }
                    catch ( Throwable ignore )
                    {
                    }

                    log( LogService.LOG_ERROR,
                        "BundleComponentActivator : Bundle [{0}] Unexpected failure enabling component holder {1}",
                        new Object[] { m_bundle.getBundleId(), componentHolder.getComponentMetadata().getName() }, null,
                        null, t );
                }
            }
            else
            {
                log( LogService.LOG_DEBUG,
                    "BundleComponentActivator : Bundle [{0}] Will not enable component holder {1}",
                    new Object[] { m_bundle.getBundleId(), componentHolder.getComponentMetadata().getName() }, null,
                    null, null );
            }
        }
    }

    /**
     * Finds component descriptors based on descriptor location.
     *
     * @param bundle bundle to search for descriptor files
     * @param descriptorLocation descriptor location
     * @return array of descriptors or empty array if none found
     */
    static URL[] findDescriptors(final Bundle bundle, final String descriptorLocation)
    {
        if ( bundle == null || descriptorLocation == null || descriptorLocation.trim().length() == 0 )
        {
            return new URL[0];
        }

        // split pattern and path
        final int lios = descriptorLocation.lastIndexOf( "/" );
        final String path;
        final String filePattern;
        if ( lios > 0 )
        {
            path = descriptorLocation.substring( 0, lios );
            filePattern = descriptorLocation.substring( lios + 1 );
        }
        else
        {
            path = "/";
            filePattern = descriptorLocation;
        }

        // find the entries
        final Enumeration<URL> entries = bundle.findEntries( path, filePattern, false );
        if ( entries == null || !entries.hasMoreElements() )
        {
            return new URL[0];
        }

        // create the result list
        List<URL> urls = new ArrayList<URL>();
        while ( entries.hasMoreElements() )
        {
            urls.add( entries.nextElement() );
        }
        return urls.toArray( new URL[urls.size()] );
    }

    private void loadDescriptor(final URL descriptorURL)
    {
        // simple path for log messages
        final String descriptorLocation = descriptorURL.getPath();

        InputStream stream = null;
        try
        {
            stream = descriptorURL.openStream();

            BufferedReader in = new BufferedReader( new InputStreamReader( stream, "UTF-8" ) );
            XmlHandler handler = new XmlHandler( m_bundle, this, getConfiguration().isFactoryEnabled(),
                getConfiguration().keepInstances() );
            KXml2SAXParser parser;

            parser = new KXml2SAXParser( in );

            parser.parseXML( handler );

            // 112.4.2 Component descriptors may contain a single, root component element
            // or one or more component elements embedded in a larger document
            for ( Object o : handler.getComponentMetadataList() )
            {
                ComponentMetadata metadata = (ComponentMetadata) o;
                ComponentRegistryKey key = null;
                try
                {
                    // check and reserve the component name (if not null)
                    if ( metadata.getName() != null )
                    {
                        key = m_componentRegistry.checkComponentName( m_bundle, metadata.getName() );
                    }

                    // validate the component metadata
                    metadata.validate( this );

                    // Request creation of the component manager
                    ComponentHolder<?> holder = m_componentRegistry.createComponentHolder( this, metadata );

                    // register the component after validation
                    m_componentRegistry.registerComponentHolder( key, holder );
                    m_holders.add( holder );

                    log( LogService.LOG_DEBUG,
                        "BundleComponentActivator : Bundle [{0}] ComponentHolder created for {1}",
                        new Object[] { m_bundle.getBundleId(), metadata.getName() }, null, null, null );

                }
                catch ( Throwable t )
                {
                    // There is a problem with this particular component, we'll log the error
                    // and proceed to the next one
                    log( LogService.LOG_ERROR, "Cannot register Component", metadata, null, t );

                    // make sure the name is not reserved any more
                    if ( key != null )
                    {
                        m_componentRegistry.unregisterComponentHolder( key );
                    }
                }
            }
        }
        catch ( IOException ex )
        {
            // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
            // fragments, SCR must log an error message with the Log Service, if present, and continue.

            log( LogService.LOG_ERROR, "Problem reading descriptor entry ''{0}''", new Object[] { descriptorLocation },
                null, null, ex );
        }
        catch ( Exception ex )
        {
            log( LogService.LOG_ERROR, "General problem with descriptor entry ''{0}''",
                new Object[] { descriptorLocation }, null, null, ex );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
    }

    /**
    * Dispose of this component activator instance and all the component
    * managers.
    */
    void dispose(int reason)
    {
        if ( m_active.compareAndSet( true, false ) )
        {
            log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] will destroy {1} instances",
                new Object[] { m_bundle.getBundleId(), m_holders.size() }, null, null, null );

            for ( ComponentHolder<?> holder : m_holders )
            {
                try
                {
                    holder.disposeComponents( reason );
                }
                catch ( Exception e )
                {
                    log( LogService.LOG_ERROR, "BundleComponentActivator : Exception invalidating",
                        holder.getComponentMetadata(), null, e );
                }
                finally
                {
                    m_componentRegistry.unregisterComponentHolder( m_bundle, holder.getComponentMetadata().getName() );
                }

            }
            if ( configAdminTracker != null )
            {
                configAdminTracker.dispose();
            }

            log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] STOPPED",
                new Object[] { m_bundle.getBundleId() }, null, null, null );

            m_logService.close();
            m_closeLatch.countDown();
        }
        else
        {
            try
            {
                m_closeLatch.await( m_configuration.lockTimeout(), TimeUnit.MILLISECONDS );
            }
            catch ( InterruptedException e )
            {
                //ignore interruption during concurrent shutdown.
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * Returns <true> if this instance is active, that is if components
     * may be activated for this component. The active flag is set early
     * in the constructor indicating the activator is basically active
     * (not fully setup, though) and reset early in the process of
     * {@link #dispose(int) disposing} this instance.
     */
    public boolean isActive()
    {
        return m_active.get();
    }

    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    public BundleContext getBundleContext()
    {
        return m_context;
    }

    public ScrConfiguration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * Implements the <code>ComponentContext.enableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * enabling them.  The enable method will schedule activation.
     * <p>
     *
     * @param name The name of the component to enable or <code>null</code> to
     *      enable all components.
     */
    public void enableComponent(final String name)
    {
        final List<ComponentHolder<?>> holder = getSelectedComponents( name );
        for ( ComponentHolder<?> aHolder : holder )
        {
            try
            {
                log( LogService.LOG_DEBUG, "Enabling Component", aHolder.getComponentMetadata(), null, null );
                aHolder.enableComponents( true );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot enable component", aHolder.getComponentMetadata(), null, t );
            }
        }
    }

    /**
     * Implements the <code>ComponentContext.disableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * disabling them.  The disable method will schedule deactivation
     * <p>
     *
     * @param name The name of the component to disable or <code>null</code> to
     *      disable all components.
     */
    public void disableComponent(final String name)
    {
        final List<ComponentHolder<?>> holder = getSelectedComponents( name );
        for ( ComponentHolder<?> aHolder : holder )
        {
            try
            {
                log( LogService.LOG_DEBUG, "Disabling Component", aHolder.getComponentMetadata(), null, null );
                aHolder.disableComponents( true );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot disable component", aHolder.getComponentMetadata(), null, t );
            }
        }
    }

    /**
     * Returns an array of {@link ComponentHolder} instances which match the
     * <code>name</code>. If the <code>name</code> is <code>null</code> an
     * array of all currently known component managers is returned. Otherwise
     * an array containing a single component manager matching the name is
     * returned if one is registered. Finally, if no component manager with the
     * given name is registered, <code>null</code> is returned.
     *
     * @param name The name of the component manager to return or
     *      <code>null</code> to return an array of all component managers.
     *
     * @return An array containing one or more component managers according
     *      to the <code>name</code> parameter or <code>null</code> if no
     *      component manager with the given name is currently registered.
     */
    private List<ComponentHolder<?>> getSelectedComponents(String name)
    {
        // if all components are selected
        if ( name == null )
        {
            return m_holders;
        }

        ComponentHolder<?> componentHolder = m_componentRegistry.getComponentHolder( m_bundle, name );
        if ( componentHolder != null )
        {
            return Collections.<ComponentHolder<?>> singletonList( componentHolder );
        }

        // if the component is not known
        return Collections.emptyList();
    }

    //---------- Component ID support

    public long registerComponentId(AbstractComponentManager<?> componentManager)
    {
        return m_componentRegistry.registerComponentId( componentManager );
    }

    public void unregisterComponentId(AbstractComponentManager<?> componentManager)
    {
        m_componentRegistry.unregisterComponentId( componentManager.getId() );
    }

    //---------- Asynchronous Component Handling ------------------------------

    /**
     * Schedules the given <code>task</code> for asynchrounous execution or
     * synchronously runs the task if the thread is not running. If this instance
     * is {@link #isActive() not active}, the task is not executed.
     *
     * @param task The component task to execute
     */
    public void schedule(Runnable task)
    {
        if ( isActive() )
        {
            ComponentActorThread cat = m_componentActor;
            if ( cat != null )
            {
                cat.schedule( task );
            }
            else
            {
                log( LogService.LOG_DEBUG, "Component Actor Thread not running, calling synchronously", null, null,
                    null );
                try
                {
                    synchronized ( this )
                    {
                        task.run();
                    }
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_WARNING, "Unexpected problem executing task", null, null, t );
                }
            }
        }
        else
        {
            log( LogService.LOG_WARNING, "BundleComponentActivator is not active; not scheduling {0}",
                new Object[] { task }, null, null, null );
        }
    }

    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public boolean isLogEnabled(int level)
    {
        return m_configuration.getLogLevel() >= level;
    }

    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param pattern The <code>java.text.MessageFormat</code> message format
     *      string for preparing the message
     * @param arguments The format arguments for the <code>pattern</code>
    *      string.
     * @param componentId
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     */
    public void log(int level, String pattern, Object[] arguments, ComponentMetadata metadata, Long componentId,
        Throwable ex)
    {
        if ( isLogEnabled( level ) )
        {
            final String message = MessageFormat.format( pattern, arguments );
            log( level, message, metadata, componentId, ex );
        }
    }

    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param message The message to log
     * @param componentId
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     */
    public void log(int level, String message, ComponentMetadata metadata, Long componentId, Throwable ex)
    {
        if ( isLogEnabled( level ) )
        {
            // prepend the metadata name to the message
            if ( metadata != null )
            {
                if ( componentId != null )
                {
                    message = "[" + metadata.getName() + "(" + componentId + ")] " + message;
                }
                else
                {
                    message = "[" + metadata.getName() + "] " + message;
                }
            }

            ServiceTracker<LogService, LogService> logService = m_logService;
            if ( logService != null )
            {
                LogService logger = logService.getService();
                if ( logger == null )
                {
                    m_logger.log( level, message, ex );
                }
                else
                {
                    logger.log( level, message, ex );
                }
            }
            else
            {
                // BCA has been disposed off, bundle context is probably invalid. Try to log something.
                m_logger.log( level, message, ex );
            }
        }
    }

    public <T> boolean enterCreate(ServiceReference<T> serviceReference)
    {
        return m_componentRegistry.enterCreate( serviceReference );
    }

    public <T> void leaveCreate(ServiceReference<T> serviceReference)
    {
        m_componentRegistry.leaveCreate( serviceReference );
    }

    public <T> void missingServicePresent(ServiceReference<T> serviceReference)
    {
        m_componentRegistry.missingServicePresent( serviceReference, m_componentActor );
    }

    public <S, T> void registerMissingDependency(DependencyManager<S, T> dependencyManager,
        ServiceReference<T> serviceReference, int trackingCount)
    {
        m_componentRegistry.registerMissingDependency( dependencyManager, serviceReference, trackingCount );
    }

    public RegionConfigurationSupport setRegionConfigurationSupport(ServiceReference<ConfigurationAdmin> reference)
    {
        RegionConfigurationSupport rcs = m_componentRegistry.registerRegionConfigurationSupport( reference );
        for ( ComponentHolder<?> holder : m_holders )
        {
            rcs.configureComponentHolder( holder );
        }
        return rcs;
    }

    public void unsetRegionConfigurationSupport(RegionConfigurationSupport rcs)
    {
        m_componentRegistry.unregisterRegionConfigurationSupport( rcs );
        // TODO anything needed?
    }
}
