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
package org.apache.felix.scr.impl.manager;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
public abstract class AbstractComponentManager<S> implements Component, SimpleLogger
{
    //useful text for deactivation reason numbers
    static final String[] REASONS = {"Unspecified",
        "Component disabled",
        "Reference became unsatisfied",
        "Configuration modified",
        "Configuration deleted",
        "Component disabled",
        "Bundle stopped"};

    // the ID of this component
    private long m_componentId;

    // The state of this instance manager
    // methods accessing this field should be synchronized unless there is a
    // good reason to not be synchronized
    private volatile State m_state;

    // The metadata
    private final ComponentMetadata m_componentMetadata;

    private final ComponentMethods m_componentMethods;

    // The dependency managers that manage every dependency
    private final List<DependencyManager<S, ?>> m_dependencyManagers;

    private volatile boolean m_dependencyManagersInitialized;

    private volatile boolean m_dependenciesCollected;

    private final AtomicInteger trackingCount = new AtomicInteger( );

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator;

    // The ServiceRegistration is now tracked in the RegistrationManager
    
    private final ReentrantLock m_stateLock;

    protected volatile boolean enabled;
    protected volatile CountDownLatch enabledLatch;
    private final Object enabledLatchLock = new Object();

    protected volatile boolean m_internalEnabled;
    
    private volatile boolean disposed;
    
    //service event tracking
    private int floor;

    private volatile int ceiling;

    private final Lock missingLock = new ReentrantLock();
    private final Condition missingCondition = missingLock.newCondition();
    private final Set<Integer> missing = new TreeSet<Integer>( );



    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     * @param componentMethods
     */
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, ComponentMethods componentMethods )
    {
        m_activator = activator;
        m_componentMetadata = metadata;
        this.m_componentMethods = componentMethods;
        m_componentId = -1;

        m_state = Disabled.getInstance();
        m_dependencyManagers = loadDependencyManagers( metadata );

        m_stateLock = new ReentrantLock( true );

        // dump component details
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            log(
                LogService.LOG_DEBUG,
                "Component {0} created: DS={1}, implementation={2}, immediate={3}, default-enabled={4}, factory={5}, configuration-policy={6}, activate={7}, deactivate={8}, modified={9} configuration-pid={10}",
                new Object[]
                    { metadata.getName(), metadata.getNamespaceCode(),
                        metadata.getImplementationClassName(), metadata.isImmediate(),
                        metadata.isEnabled(), metadata.getFactoryIdentifier(),
                        metadata.getConfigurationPolicy(), metadata.getActivate(), metadata.getDeactivate(),
                        metadata.getModified(), metadata.getConfigurationPid() }, null );

            if ( metadata.getServiceMetadata() != null )
            {
                log( LogService.LOG_DEBUG, "Component {0} Services: servicefactory={1}, services={2}", new Object[]
                    { metadata.getName(), Boolean.valueOf( metadata.getServiceMetadata().isServiceFactory() ),
                        Arrays.asList( metadata.getServiceMetadata().getProvides() ) }, null );
            }

            if ( metadata.getProperties() != null )
            {
                log( LogService.LOG_DEBUG, "Component {0} Properties: {1}", new Object[]
                    { metadata.getName(), metadata.getProperties() }, null );
            }
        }
    }

    final void obtainWriteLock( String source )
    {
        try
        {
            if (!m_stateLock.tryLock( getLockTimeout(), TimeUnit.MILLISECONDS ) )
            {
            	dumpThreads();
                throw new IllegalStateException( "Could not obtain lock" );
            }
        }
        catch ( InterruptedException e )
        {
            try
            {
                if (!m_stateLock.tryLock( getLockTimeout(), TimeUnit.MILLISECONDS ) )
                {
                    dumpThreads();
                    throw new IllegalStateException( "Could not obtain lock" );
                }
            }
            catch ( InterruptedException e1 )
            {
                Thread.currentThread().interrupt();
                //TODO is there a better exception to throw?
                throw new IllegalStateException( "Interrupted twice: Could not obtain lock" );
            }
            Thread.currentThread().interrupt();
        }
    }

    long getLockTimeout()
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.getConfiguration().lockTimeout();
        }
        return ScrConfiguration.DEFAULT_LOCK_TIMEOUT_MILLISECONDS;
    }

    final void releaseWriteLock( String source )
    {
        m_stateLock.unlock();
    }

    final boolean isWriteLocked()
    {
        return m_stateLock.getHoldCount() > 0;
    }
    
    void dumpThreads()
    {
        try
        {
            String dump = new ThreadDump().call();
            log( LogService.LOG_DEBUG, dump, null );
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_DEBUG, "Could not dump threads", t );
        }
    }

    //service event tracking
    void tracked( int trackingCount )
    {
        missingLock.lock();
        try
        {
            if (trackingCount == floor + 1 )
            {
                floor++;
                missing.remove( trackingCount );
            }
            else if ( trackingCount < ceiling )
            {
                missing.remove( trackingCount );
            }
            if ( trackingCount > ceiling )
            {
                for (int i = ceiling + 1; i < trackingCount; i++ )
                {
                    missing.add( i );
                }
                ceiling = trackingCount;
            }
            missingCondition.signalAll();
        }
        finally
        {
            missingLock.unlock();
        }
    }

    void waitForTracked( int trackingCount )
    {
        missingLock.lock();
        try
        {
            while ( ceiling  < trackingCount || ( !missing.isEmpty() && missing.iterator().next() < trackingCount))
            {
                log( LogService.LOG_DEBUG, "waitForTracked trackingCount: {0} ceiling: {1} missing: {2}",
                        new Object[] {trackingCount, ceiling, missing}, null );
                try
                {
                    if ( !doMissingWait())
                    {
                        return;                        
                    }
                }
                catch ( InterruptedException e )
                {
                    try
                    {
                        if ( !doMissingWait())
                        {
                            return;                        
                        }
                    }
                    catch ( InterruptedException e1 )
                    {
                        log( LogService.LOG_ERROR, "waitForTracked interrupted twice: {0} ceiling: {1} missing: {2},  Expect further errors",
                                new Object[] {trackingCount, ceiling, missing}, e1 );
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally
        {
            missingLock.unlock();
        }
    }
    
    private boolean doMissingWait() throws InterruptedException
    {
        if ( !missingCondition.await( getLockTimeout(), TimeUnit.MILLISECONDS ))
        {
            log( LogService.LOG_ERROR, "waitForTracked timed out: {0} ceiling: {1} missing: {2},  Expect further errors",
                    new Object[] {trackingCount, ceiling, missing}, null );
            dumpThreads();
            missing.clear();
            return false;
        }
        return true;
    }

//---------- Component ID management

    void registerComponentId()
    {
        final BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            this.m_componentId = activator.registerComponentId( this );
        }
    }


    void unregisterComponentId()
    {
        if ( this.m_componentId >= 0 )
        {
            final BundleComponentActivator activator = getActivator();
            if ( activator != null )
            {
                activator.unregisterComponentId( this );
            }
            this.m_componentId = -1;
        }
    }


    //---------- Asynchronous frontend to state change methods ----------------
    private static final AtomicLong taskCounter = new AtomicLong( );

    /**
     * Enables this component and - if satisfied - also activates it. If
     * enabling the component fails for any reason, the component ends up
     * disabled.
     * <p>
     * This method ignores the <i>enabled</i> flag of the component metadata
     * and just enables as requested.
     * <p>
     * This method enables and activates the component immediately.
     */
    public final void enable()
    {
        enable( true );
    }


    public final void enable( final boolean async )
    {
        if (enabled)
        {
            return;
        }
        try
        {
            synchronized ( enabledLatchLock )
            {
                if ( enabledLatch != null )
                {
                    enabledLatch.await();
                }
                enabledLatch  = new CountDownLatch( 1 );
            }
            enableInternal();
            if ( !async )
            {
                activateInternal( trackingCount.get() );
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if ( !async )
            {
                enabledLatch.countDown();
            }
            enabled = true;
        }

        if ( async )
        {
            m_activator.schedule( new Runnable()
            {

                long count = taskCounter.incrementAndGet();

                public void run()
                {
                    try
                    {
                        activateInternal( trackingCount.get() );
                    }
                    finally
                    {
                        enabledLatch.countDown();
                    }
                }

                public String toString()
                {
                    return "Async Activate: " + getComponentMetadata().getName() + " id: " + count;
                }
            } );
        }
    }

    /**
     * Disables this component and - if active - first deactivates it. The
     * component may be reenabled by calling the {@link #enable()} method.
     * <p>
     * This method deactivates and disables the component immediately.
     */
    public final void disable()
    {
        disable( true );
    }


    public final void disable( final boolean async )
    {
        if (!enabled)
        {
            return;
        }
        try
        {
            synchronized ( enabledLatchLock )
            {
                if (enabledLatch != null)
                {
                    enabledLatch.await();
                }
                enabledLatch = new CountDownLatch( 1 );
            }
            if ( !async )
            {
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED, true, trackingCount.get() );
            }
            disableInternal();
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (!async)
            {
                enabledLatch.countDown();
            }
            enabled = false;
        }

        if ( async )
        {
            m_activator.schedule( new Runnable()
            {

                long count = taskCounter.incrementAndGet();

                public void run()
                {
                    try
                    {
                        deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED, true, trackingCount.get() );
                    }
                    finally
                    {
                        enabledLatch.countDown();
                    }
                }

                public String toString()
                {
                    return "Async Deactivate: " + getComponentMetadata().getName() + " id: " + count;
                }

            } );
        }
    }

    // supports the ComponentInstance.dispose() method
    void dispose()
    {
        dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
    }

    /**
     * Disposes off this component deactivating and disabling it first as
     * required. After disposing off the component, it may not be used anymore.
     * <p>
     * This method unlike the other state change methods immediately takes
     * action and disposes the component. The reason for this is, that this
     * method has to actually complete before other actions like bundle stopping
     * may continue.
     */
    public void dispose( int reason )
    {
        disposed = true;
        disposeInternal( reason );
    }
    
    <T> void registerMissingDependency( DependencyManager<S, T> dm, ServiceReference<T> ref, int trackingCount)
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.registerMissingDependency( dm, ref, trackingCount );
        }
    }

    //---------- Component interface ------------------------------------------

    public long getId()
    {
        return m_componentId;
    }

    public String getName() {
        return m_componentMetadata.getName();
    }

    /**
     * Returns the <code>Bundle</code> providing this component. If the
     * component as already been disposed off, this method returns
     * <code>null</code>.
     */
    public Bundle getBundle()
    {
        final BundleContext context = getBundleContext();
        if ( context != null )
        {
            try
            {
                return context.getBundle();
            }
            catch ( IllegalStateException ise )
            {
                // if the bundle context is not valid any more
            }
        }
        // already disposed off component or bundle context is invalid
        return null;
    }
    
    BundleContext getBundleContext()
    {
        final BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.getBundleContext();
        }
        return null;        
    }


    public String getClassName()
    {
        return m_componentMetadata.getImplementationClassName();
    }

    public String getFactory()
    {
        return m_componentMetadata.getFactoryIdentifier();
    }

    public Reference[] getReferences()
    {
        if ( m_dependencyManagers != null && m_dependencyManagers.size() > 0 )
        {
            return (Reference[]) m_dependencyManagers.toArray(
                    new Reference[m_dependencyManagers.size()] );
        }

        return null;
    }

    public boolean isImmediate()
    {
        return m_componentMetadata.isImmediate();

    }

    public boolean isDefaultEnabled()
    {
        return m_componentMetadata.isEnabled();
    }


    public String getActivate()
    {
        return m_componentMetadata.getActivate();
    }


    public boolean isActivateDeclared()
    {
        return m_componentMetadata.isActivateDeclared();
    }


    public String getDeactivate()
    {
        return m_componentMetadata.getDeactivate();
    }


    public boolean isDeactivateDeclared()
    {
        return m_componentMetadata.isDeactivateDeclared();
    }


    public String getModified()
    {
        return m_componentMetadata.getModified();
    }


    public String getConfigurationPolicy()
    {
        return m_componentMetadata.getConfigurationPolicy();
    }

    public String getConfigurationPid()
    {
        return m_componentMetadata.getConfigurationPid();
    }

    public boolean isConfigurationPidDeclared()
    {
        return m_componentMetadata.isConfigurationPidDeclared();
    }

    public boolean isServiceFactory()
    {
        return m_componentMetadata.getServiceMetadata() != null
                && m_componentMetadata.getServiceMetadata().isServiceFactory();
    }

    public boolean isFactory()
    {
        return false;
    }

    public String[] getServices()
    {
        if ( m_componentMetadata.getServiceMetadata() != null )
        {
            return m_componentMetadata.getServiceMetadata().getProvides();
        }

        return null;
    }

    //-------------- atomic transition methods -------------------------------

    final void enableInternal()
    {
        if ( disposed )
        {
            throw new IllegalStateException( "enable: " + this );
        }
        if ( !isActivatorActive() )
        {
            log( LogService.LOG_DEBUG, "Bundle's component activator is not active; not enabling component",
                    null );
            return;
        }

        registerComponentId();
        changeState( Unsatisfied.getInstance() );
        log( LogService.LOG_DEBUG, "Component enabled", null );
        m_internalEnabled = true;
    }

    final void activateInternal( int trackingCount )
    {
         m_state.activate( this );
    }

    final void deactivateInternal( int reason, boolean disable, int trackingCount )
    {
        m_state.deactivate( this, reason, disable );
    }

    final void disableInternal()
    {
        m_internalEnabled = false;
        m_state.disable( this );
    }

    /**
     * Disposes off this component deactivating and disabling it first as
     * required. After disposing off the component, it may not be used anymore.
     * <p>
     * This method unlike the other state change methods immediately takes
     * action and disposes the component. The reason for this is, that this
     * method has to actually complete before other actions like bundle stopping
     * may continue.
     */
    public final void disposeInternal( int reason )
    {
        log( LogService.LOG_DEBUG, "Disposing component (reason: " + reason + ")", null );
        doDeactivate( reason, true );
        //may call this multiple times.
//        disableDependencyManagers();
        clear();
        changeState( Disposed.getInstance() );
             }
         
    void doDeactivate( int reason, boolean disable )
    {
        try
        {
            if ( !unregisterService() )
            {
                log( LogService.LOG_DEBUG, "Component deactivation occuring on another thread", null );
                //another thread is deactivating.
                return;
            }
            obtainWriteLock( "AbstractComponentManager.State.doDeactivate.1" );
            try
            {
                deleteComponent( reason );
                deactivateDependencyManagers();
                if ( disable )
                {
                    disableDependencyManagers();
                }
                unsetDependenciesCollected();
            }
            finally
            {
                releaseWriteLock( "AbstractComponentManager.State.doDeactivate.1" );
            }
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_WARNING, "Component deactivation threw an exception", t );
        }
    }

    final ServiceReference<S> getServiceReference()
    {
        ServiceRegistration<S> reg = getServiceRegistration();
        if (reg != null)
        {
            return reg.getReference();
        }
        return null;
    }

    //---------- Component handling methods ----------------------------------
    /**
     * Method is called by {@link State#activate(AbstractComponentManager)} in STATE_ACTIVATING or by
     * {@link DelayedComponentManager#getService(Bundle, ServiceRegistration)}
     * in STATE_REGISTERED.
     *
     * @return <code>true</code> if creation of the component succeeded. If
     *       <code>false</code> is returned, the cause should have been logged.
     */
    protected abstract boolean createComponent();

    protected abstract void deleteComponent( int reason );

    boolean getServiceInternal()
    {
        return false;
    }

    /**
     * All ComponentManagers are ServiceFactory instances
     *
     * @return this as a ServiceFactory.
     */
    private Object getService()
    {
        return this;
    }

    abstract State getSatisfiedState();

    abstract State getActiveState();

    ComponentMethods getComponentMethods()
    {
        return m_componentMethods;
    }
    
    protected String[] getProvidedServices()
    {
        if ( getComponentMetadata().getServiceMetadata() != null )
        {
            String[] provides = getComponentMetadata().getServiceMetadata().getProvides();
            return provides;
        }
        return null;
        
    }

    private final RegistrationManager<ServiceRegistration<S>> registrationManager = new RegistrationManager<ServiceRegistration<S>>()
    {

        @Override
        ServiceRegistration<S> register(String[] services)
        {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext == null) 
            {
                return null;
            }
            final Dictionary<String, Object> serviceProperties = getServiceProperties();
            ServiceRegistration<S> serviceRegistration = ( ServiceRegistration<S> ) bundleContext
                    .registerService( services, getService(), serviceProperties );
            return serviceRegistration;
        }

        @Override
        void unregister(ServiceRegistration<S> serviceRegistration)
        {
            serviceRegistration.unregister();
        }

        @Override
        void log(int level, String message, Object[] arguments, Throwable ex)
        {
            AbstractComponentManager.this.log(level, message, arguments, ex);
        }

        @Override
        long getTimeout()
        {
            return getLockTimeout();
        }

        @Override
        void reportTimeout()
        {
            dumpThreads();
        }
        
    };
    

    /**
     * Registers the service on behalf of the component.
     *
     */
    protected boolean registerService()
    {
        String[] services = getProvidedServices();
        if ( services != null )
        {
            return registrationManager.changeRegistration( RegistrationManager.RegState.registered, services);
        }
        return true;
    }

    protected boolean unregisterService()
    {
        String[] services = getProvidedServices();
        if ( services != null )
        {
            return registrationManager.changeRegistration( RegistrationManager.RegState.unregistered, services );
        }
        return true;
    }
    

    AtomicInteger getTrackingCount()
    {
        return trackingCount;
    }


    boolean initDependencyManagers()
    {
        if ( m_dependencyManagersInitialized )
        {
            return true;
        }
        final Bundle bundle = getBundle();
        if (bundle == null)
        {
            log( LogService.LOG_ERROR, "bundle shut down while trying to load implementation object class", null );
            return false;
        }
        Class<?> implementationObjectClass;
        try
        {
            implementationObjectClass = bundle.loadClass(
                    getComponentMetadata().getImplementationClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            log( LogService.LOG_ERROR, "Could not load implementation object class", e );
            return false;
        }
        m_componentMethods.initComponentMethods( m_componentMetadata, implementationObjectClass );

        for ( DependencyManager dependencyManager : m_dependencyManagers )
        {
            dependencyManager.initBindingMethods( m_componentMethods.getBindMethods( dependencyManager.getName() ) );
        }
        m_dependencyManagersInitialized = true;
        return true;
    }

    /**
     * Collect and store in m_dependencies_map all the services for dependencies, outside of any locks.
     * Throwing IllegalStateException on failure to collect all the dependencies is needed so getService can
     * know to return null.
     *
     * @return true if this thread collected the dependencies;
     *   false if some other thread successfully collected the dependencies;
     * @throws IllegalStateException if some dependency is no longer available.
     */
    protected boolean collectDependencies() throws IllegalStateException
    {
        if ( m_dependenciesCollected)
        {
            log( LogService.LOG_DEBUG, "dependencies already collected, do not collect dependencies", null );
            return false;
        }
        initDependencyManagers();
        for ( DependencyManager<S, ?> dependencyManager : m_dependencyManagers )
        {
            if ( !dependencyManager.prebind() )
            {
                //not actually satisfied any longer
                deactivateDependencyManagers();
                log( LogService.LOG_DEBUG, "Could not get required dependency for dependency manager: {0}",
                        new Object[] {dependencyManager.getName()}, null );
                throw new IllegalStateException( "Missing dependencies, not satisfied" );
            }
        }
        m_dependenciesCollected = true;
        log( LogService.LOG_DEBUG, "This thread collected dependencies", null );
        return true;
    }

    protected void unsetDependenciesCollected()
    {
        m_dependenciesCollected = false;
    }

    abstract <T> void invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount );

    abstract <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount );

    abstract <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> oldRefPair, int trackingCount );

    //**********************************************************************************************************
    public BundleComponentActivator getActivator()
    {
        return m_activator;
    }


    boolean isActivatorActive()
    {
        BundleComponentActivator activator = getActivator();
        return activator != null && activator.isActive();
    }


    final ServiceRegistration<S> getServiceRegistration() 
    {
        return registrationManager.getServiceRegistration();
    }


    synchronized void clear()
    {
        // for some testing, the activator may be null
        if ( m_activator != null )
        {
            m_activator.unregisterComponentId( this );
            m_activator = null;
        }
    }

    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public boolean isLogEnabled( int level )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.isLogEnabled( level );
        }

        // bundle activator has already been removed, so no logging
        return false;
    }


    public void log( int level, String message, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, getComponentMetadata(), m_componentId, ex );
        }
    }

    public void log( int level, String message, Object[] arguments, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, arguments, getComponentMetadata(), m_componentId, ex );
        }
    }


    public String toString()
    {
        return "Component: " + getName() + " (" + getId() + ")";
    }


    private boolean hasServiceRegistrationPermissions()
    {
        boolean allowed = true;
        if ( System.getSecurityManager() != null )
        {
            final ServiceMetadata serviceMetadata = getComponentMetadata().getServiceMetadata();
            if ( serviceMetadata != null )
            {
                final String[] services = serviceMetadata.getProvides();
                if ( services != null && services.length > 0 )
                {
                    final Bundle bundle = getBundle();
                    for ( String service : services )
                    {
                        final Permission perm = new ServicePermission( service, ServicePermission.REGISTER );
                        if ( !bundle.hasPermission( perm ) )
                        {
                            log( LogService.LOG_DEBUG, "Permission to register service {0} is denied", new Object[]
                                    {service}, null );
                            allowed = false;
                        }
                    }
                }
            }
        }

        // no security manager or no services to register
        return allowed;
    }


    private List<DependencyManager<S, ?>> loadDependencyManagers( ComponentMetadata metadata )
    {
        List<DependencyManager<S, ?>> depMgrList = new ArrayList<DependencyManager<S, ?>>(metadata.getDependencies().size());

        // If this component has got dependencies, create dependency managers for each one of them.
        if ( metadata.getDependencies().size() != 0 )
        {
            int index = 0;
            for ( ReferenceMetadata currentdependency: metadata.getDependencies() )
            {
                DependencyManager<S, ?> depmanager = new DependencyManager( this, currentdependency, index++ );

                depMgrList.add( depmanager );
            }
        }

        return depMgrList;
    }

    protected void updateTargets(Dictionary<String, Object> properties)
    {
        if ( m_internalEnabled )
        {
            for ( DependencyManager<S, ?> dm: getDependencyManagers() )
            {
                dm.setTargetFilter( properties );
            }
        }
    }

    protected boolean verifyDependencyManagers()
    {
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {

            if ( !dm.hasGetPermission() )
            {
                // bundle has no service get permission
                if ( dm.isOptional() )
                {
                    log( LogService.LOG_DEBUG, "No permission to get optional dependency: {0}; assuming satisfied",
                        new Object[]
                            { dm.getName() }, null );
                }
                else
                {
                    log( LogService.LOG_DEBUG, "No permission to get mandatory dependency: {0}; assuming unsatisfied",
                        new Object[]
                            { dm.getName() }, null );
                    satisfied = false;
                }
            }
            else if ( !dm.isSatisfied() )
            {
                // bundle would have permission but there are not enough services
                log( LogService.LOG_DEBUG, "Dependency not satisfied: {0}", new Object[]
                    { dm.getName() }, null );
                satisfied = false;
            }
        }

        return satisfied;
    }

    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in declaration order
     */
    List<DependencyManager<S, ?>> getDependencyManagers()
    {
        return m_dependencyManagers;
    }

    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in reversed declaration order
     */
    List<DependencyManager<S, ?>> getReversedDependencyManagers()
    {
        List<DependencyManager<S, ?>> list = new ArrayList<DependencyManager<S, ?>>( m_dependencyManagers );
        Collections.reverse( list );
        return list;
    }


    DependencyManager<S, ?> getDependencyManager(String name)
    {
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            if ( name.equals(dm.getName()) )
            {
                return dm;
            }
        }

        // not found
        return null;
    }

    private void deactivateDependencyManagers()
    {
        log( LogService.LOG_DEBUG, "Deactivating dependency managers", null);
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            dm.deactivate();
        }
    }

    private void disableDependencyManagers()
    {
        log( LogService.LOG_DEBUG, "Disabling dependency managers", null);
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            dm.unregisterServiceListener();
        }
    }

    public abstract boolean hasConfiguration();

    public abstract Dictionary<String, Object> getProperties();

    public abstract void setServiceProperties( Dictionary<String, Object> serviceProperties );

    /**
     * Returns the subset of component properties to be used as service
     * properties. These properties are all component properties where property
     * name does not start with dot (.), properties which are considered
     * private.
     */
    public Dictionary<String, Object> getServiceProperties()
    {
        return copyTo( null, getProperties(), false );
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code>.
     *
     * @param target The <code>Dictionary</code> into which to copy the
     *      properties. If <code>null</code> a new <code>Hashtable</code> is
     *      created.
     * @param source The <code>Dictionary</code> providing the properties to
     *      copy. If <code>null</code> or empty, nothing is copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *      <code>source</code> is <code>null</code> or empty and
     *      <code>target</code> was <code>null</code>.
     */
    protected static Dictionary<String, Object> copyTo( Dictionary<String, Object> target, Dictionary<String, Object> source )
    {
        return copyTo( target, source, true );
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code> except for private
     * properties (whose name has a leading dot) which are only copied if the
     * <code>allProps</code> parameter is <code>true</code>.
     *
     * @param target    The <code>Dictionary</code> into which to copy the
     *                  properties. If <code>null</code> a new <code>Hashtable</code> is
     *                  created.
     * @param source    The <code>Dictionary</code> providing the properties to
     *                  copy. If <code>null</code> or empty, nothing is copied.
     * @param allProps  Whether all properties (<code>true</code>) or only the
     *                  public properties (<code>false</code>) are to be copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *         <code>source</code> is <code>null</code> or empty and
     *         <code>target</code> was <code>null</code> or all properties are
     *         private and had not to be copied
     */
    protected static Dictionary<String, Object> copyTo( Dictionary<String, Object> target, final Dictionary<String, Object> source, final boolean allProps )
    {
        if ( target == null )
        {
            target = new Hashtable<String, Object>();
        }

        if ( source != null && !source.isEmpty() )
        {
            for ( Enumeration ce = source.keys(); ce.hasMoreElements(); )
            {
                // cast is save, because key must be a string as per the spec
                String key = ( String ) ce.nextElement();
                if ( allProps || key.charAt( 0 ) != '.' )
                {
                    target.put( key, source.get( key ) );
                }
            }
        }

        return target;
    }


    /**
     *
     */
    public ComponentMetadata getComponentMetadata()
    {
        return m_componentMetadata;
    }

    public int getState()
    {
        return m_state.getState();
    }

    protected State state()
    {
        return m_state;
    }

    /**
     * sets the state of the manager
     */
    void changeState( State newState )
    {
        log( LogService.LOG_DEBUG, "State transition : {0} -> {1} : service reg: {2}", new Object[]
            { m_state, newState, getServiceRegistration() }, null );
        m_state = newState;
    }

    public void setServiceProperties( MethodResult methodResult )
    {
        if ( methodResult.hasResult() )
        {
            Dictionary<String, Object> serviceProps = ( methodResult.getResult() == null) ? null : new Hashtable<String, Object>( methodResult.getResult() );
            setServiceProperties(serviceProps );
        }
    }

    boolean isEnabled()
    {
        return m_internalEnabled;
    }
    
   //--------- State classes

    /**
     * There are 12 states in all. They are: Disabled, Unsatisfied,
     * Registered, Factory, Active, Disposed, as well as the transient states
     * Enabling, Activating, Deactivating, Disabling, and Disposing.
     * The Registered, Factory, FactoryInstance and Active states are the
     * "Satisfied" state in concept. The tansient states will be changed to
     * other states automatically when work is done.
     * <p>
     * The transition cases are listed below.
     * <ul>
     * <li>Disabled -(enable/ENABLING) -> Unsatisifed</li>
     * <li>Disabled -(dispose/DISPOSING)-> Disposed</li>
     * <li>Unsatisfied -(activate/ACTIVATING, SUCCESS) -> Satisfied(Registered, Factory or Active)</li>
     * <li>Unsatisfied -(activate/ACTIVATING, FAIL) -> Unsatisfied</li>
     * <li>Unsatisfied -(disable/DISABLING) -> Disabled</li>
     * <li>Registered -(getService, SUCCESS) -> Active</li>
     * <li>Registered -(getService, FAIL) -> Unsatisfied</li>
     * <li>Satisfied -(deactivate/DEACTIVATING)-> Unsatisfied</li>
     * </ul>
     */
    protected static abstract class State
    {
        private final String m_name;
        private final int m_state;


        protected State( String name, int state )
        {
            m_name = name;
            m_state = state;
        }


        public String toString()
        {
            return m_name;
        }


        int getState()
        {
            return m_state;
        }

        void activate( AbstractComponentManager acm )
        {
            log( acm, "activate" );
        }


        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            log( acm, "deactivate (reason: " + REASONS[ reason ] + ") (dsable: " + disable + ")" );
        }


        void disable( AbstractComponentManager acm )
        {
            throw new IllegalStateException("disable" + this);
        }


        private void log( AbstractComponentManager acm, String event )
        {
            acm.log( LogService.LOG_DEBUG, "Current state: {0}, Event: {1}, Service registration: {2}", new Object[]
                { m_name, event, acm.getServiceRegistration() }, null );
        }

        void doDeactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            try
            {
                if ( !acm.unregisterService() )
                {
                    acm.log( LogService.LOG_DEBUG, "Component deactivation occuring on another thread", null );
                    //another thread is deactivating.
                    return;
                }
                acm.obtainWriteLock( "AbstractComponentManager.State.doDeactivate.1" );
                try
                {
                    acm.deleteComponent( reason );
                    acm.deactivateDependencyManagers();
                    if ( disable )
                    {
                        acm.disableDependencyManagers();
                    }
                    acm.unsetDependenciesCollected();
                }
                finally
                {
                    acm.releaseWriteLock( "AbstractComponentManager.State.doDeactivate.1" );
                }
            }
            catch ( Throwable t )
            {
                acm.log( LogService.LOG_WARNING, "Component deactivation threw an exception", t );
            }
        }

        public boolean isSatisfied()
        {
            return false;
        }
    }

    protected static final class Disabled extends State
    {
        private static final Disabled m_inst = new Disabled();


        private Disabled()
        {
            super( "Disabled", STATE_DISABLED );
        }


        static State getInstance()
        {
            return m_inst;
        }


        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            doDeactivate( acm, reason, disable );
        }

    }

    protected static final class Unsatisfied extends State
    {
        private static final Unsatisfied m_inst = new Unsatisfied();


        private Unsatisfied()
        {
            super( "Unsatisfied", STATE_UNSATISFIED );
        }


        static State getInstance()
        {
            return m_inst;
        }

        /**
         * returns true if this thread succeeds in activating the component, or the component is not able to be activated.
         * Returns false if some other thread succeeds in activating the component.
         * @param acm
         */
        void activate( AbstractComponentManager acm )
        {
            if ( !acm.isActivatorActive() )
            {
                acm.log( LogService.LOG_DEBUG, "Bundle's component activator is not active; not activating component",
                    null );
                return;
            }

            acm.log( LogService.LOG_DEBUG, "Activating component from state {0}", new Object[] {this},  null );

            // Before creating the implementation object, we are going to
            // test if we have configuration if such is required
            if ( !acm.hasConfiguration() && acm.getComponentMetadata().isConfigurationRequired() )
            {
                acm.log( LogService.LOG_DEBUG, "Missing required configuration, cannot activate", null );
                return;
            }

            // Before creating the implementation object, we are going to
            // test that the bundle has enough permissions to register services
            if ( !acm.hasServiceRegistrationPermissions() )
            {
                acm.log( LogService.LOG_DEBUG, "Component is not permitted to register all services, cannot activate",
                    null );
                return;
            }

            // Update our target filters.
            acm.log( LogService.LOG_DEBUG, "Updating target filters", null );
            acm.updateTargets( acm.getProperties() );

            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            if ( !acm.verifyDependencyManagers() )
            {
                acm.log( LogService.LOG_DEBUG, "Not all dependencies satisfied, cannot activate", null );
                return;
            }

            // set satisfied state before registering the service because
            // during service registration a listener may try to get the
            // service from the service reference which may cause a
            // delayed service object instantiation through the State

            // actually since we don't have the activating state any
            // longer, we have to set the satisfied state already
            // before actually creating the component such that services
            // may be accepted.
            final State satisfiedState = acm.getSatisfiedState();
            acm.changeState( satisfiedState );

            if ( !acm.registerService() )
            {
                //some other thread is activating us, or we got concurrently deactivated.
                return;
            }

            if ( ( acm.isImmediate() || acm.getComponentMetadata().isFactory() ) )
            {
                acm.getServiceInternal();
            }
 
        }

        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            acm.log( LogService.LOG_DEBUG, "Deactivating component in state Unsatisfied for reason {0}: disable: {1}",
                    new Object[] {reason, disable}, null );

            // catch any problems from deleting the component to prevent the
            // component to remain in the deactivating state !
            doDeactivate(acm, reason, disable );

            acm.log( LogService.LOG_DEBUG, "Component deactivated", null );
        }

        void disable( AbstractComponentManager acm )
        {
            acm.log( LogService.LOG_DEBUG, "Disabling component", null );
            acm.unregisterComponentId();

            // we are now disabled, ready for re-enablement or complete destroyal
            acm.changeState( Disabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disabled", null );
        }

    }

    protected static abstract class Satisfied extends State
    {
        protected Satisfied( String name, int state )
        {
            super( name, state );
        }

        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            acm.log( LogService.LOG_DEBUG, "Deactivating component for reason {0}", new Object[] {REASONS[ reason ]},  null );

            // catch any problems from deleting the component to prevent the
            // component to remain in the deactivating state !
            doDeactivate(acm, reason, disable );

            if ( acm.state().isSatisfied() )
            {
                acm.changeState( Unsatisfied.getInstance() );
            }
            acm.log( LogService.LOG_DEBUG, "Component deactivated", null );
        }

        void disable( AbstractComponentManager acm )
        {
            acm.unregisterComponentId();
            acm.changeState( Disabled.getInstance() );
            acm.log( LogService.LOG_DEBUG, "Component disabled", null );
        }

        @Override
        public boolean isSatisfied()
        {
            return true;
        }
    }

    /**
     * The <code>Active</code> state is the satisified state of an immediate
     * component after activation. Dealyed and service factory components switch
     * to this state from the {@link Registered} state once the service
     * object has (first) been requested.
     */
    protected static final class Active extends Satisfied
    {
        private static final Active m_inst = new Active();


        private Active()
        {
            super( "Active", STATE_ACTIVE );
        }


        static State getInstance()
        {
            return m_inst;
        }

    }

    /**
     * The <code>Registered</code> state is the statisfied state of a delayed or
     * service factory component before the actual service instance is
     * (first) retrieved. After getting the actualo service instance the
     * component switches to the {@link Active} state.
     */
    protected static final class Registered extends Satisfied
    {
        private static final Registered m_inst = new Registered();


        private Registered()
        {
            super( "Registered", STATE_REGISTERED );
        }


        static State getInstance()
        {
            return m_inst;
        }

    }

    /**
     * The <code>Factory</code> state is the satisfied state of component
     * factory components.
     */
    protected static final class Factory extends Satisfied
    {
        private static final Factory m_inst = new Factory();


        private Factory()
        {
            super( "Factory", STATE_FACTORY );
        }


        static State getInstance()
        {
            return m_inst;
        }
    }


    /**
     * The <code>FactoryInstance</code> state is the satisfied state of
     * instances of component factory components created with the
     * <code>ComponentFactory.newInstance</code> method. This state acts the
     * same as the {@link Active} state except that the
     * {@link org.apache.felix.scr.impl.manager.AbstractComponentManager.State#deactivate(AbstractComponentManager, int, boolean)} switches to the
     * real {@link Active} state before actually disposing off the component
     * because component factory instances are never reactivated after
     * deactivated due to not being satisified any longer. See section 112.5.5,
     * Factory Component, for full details.
     */
    protected static final class FactoryInstance extends Satisfied
    {
        private static final FactoryInstance m_inst = new FactoryInstance();


        private FactoryInstance()
        {
            super("FactoryInstance", STATE_ACTIVE);
        }

        static State getInstance()
        {
            return m_inst;
        }

        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            acm.disposeInternal( reason );
        }
    }

    /*
    final state.
     */
    protected static final class Disposed extends State
    {
        private static final Disposed m_inst = new Disposed();


        private Disposed()
        {
            super( "Disposed", STATE_DISPOSED );
        }


        static State getInstance()
        {
            return m_inst;
        }

        void activate( AbstractComponentManager acm )
        {
            throw new IllegalStateException( "activate: " + this );
        }

        void deactivate( AbstractComponentManager acm, int reason, boolean disable )
        {
            throw new IllegalStateException( "deactivate: " + this );
        }

        void disable( AbstractComponentManager acm )
        {
            throw new IllegalStateException( "disable: " + this );
        }

    }
}
