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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.BundleComponentActivator;
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
public abstract class AbstractComponentManager implements Component, SimpleLogger
{

    private static final boolean JUC_AVAILABLE;

    static {
        boolean juc_available;
        try
        {
            new JLock();
            juc_available = true;
        }
        catch (Throwable t)
        {
            juc_available = false;
        }
        JUC_AVAILABLE = juc_available;
    }

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
    private final List m_dependencyManagers;

    private boolean m_dependencyManagersInitialized;

    //<Map<DependencyManager, Map<ServiceReference, RefPair>>>
    private final AtomicReferenceWrapper m_dependencies_map;

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator;

    // The ServiceRegistration
    private final AtomicReferenceWrapper m_serviceRegistration;

    private final LockWrapper m_stateLock;

    private long m_timeout = 5000;

    private Thread lockingThread;
    private Throwable lockingStackTrace;
    private ArrayList lockingActivity = new ArrayList( );

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

        if (JUC_AVAILABLE)
        {
            m_stateLock = new JLock();
            m_dependencies_map = new JAtomicReferenceWrapper();
            m_serviceRegistration = new JAtomicReferenceWrapper();
        }
        else
        {
            m_stateLock = new EDULock();
            m_dependencies_map = new EDUAtomicReferenceWrapper();
            m_serviceRegistration = new EDUAtomicReferenceWrapper();
        }

        // dump component details
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            log(
                LogService.LOG_DEBUG,
                "Component {0} created: DS={1}, implementation={2}, immediate={3}, default-enabled={4}, factory={5}, configuration-policy={6}, activate={7}, deactivate={8}, modified={9} configuration-pid={10}",
                new Object[]
                    { metadata.getName(), new Integer( metadata.getNamespaceCode() ),
                        metadata.getImplementationClassName(), Boolean.valueOf( metadata.isImmediate() ),
                        Boolean.valueOf( metadata.isEnabled() ), metadata.getFactoryIdentifier(),
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

    //ImmediateComponentHolder should be in this manager package and this should be default access.
    public final boolean obtainReadLock( String source )
    {
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            lockingActivity.add( "obtainReadLock from: " +  source + " readLocks: " + m_stateLock.getReadHoldCount() + " writeLocks: " + m_stateLock.getWriteHoldCount() + " thread: " + Thread.currentThread() + " time: " + System.currentTimeMillis());
        }
        if (m_stateLock.getReadHoldCount() >0)
        {
            return false;
        }
        try
        {
            if (!m_stateLock.tryReadLock( m_timeout ) )
            {
                try
                {
                    dumpThreads();
                }
                catch ( Throwable e )
                {
                     //we are on a pre 1.6 vm, no worries
                }
                throw ( IllegalStateException ) new IllegalStateException( "Could not obtain lock in " + m_timeout + " milliseconds for component " + m_componentMetadata.getName() ).initCause( lockingStackTrace );
            }
        }
        catch ( InterruptedException e )
        {
            //TODO this is so wrong
            throw new IllegalStateException( "Could not obtain lock (Reason: " + e + ")" );
        }
        return true;
    }


    public final void releaseReadLock( String source )
    {
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            lockingActivity.add( "releaseReadLock from: " +  source + " readLocks: " + m_stateLock.getReadHoldCount() + " writeLocks: " + m_stateLock.getWriteHoldCount() + " thread: " + Thread.currentThread() + " time: " + System.currentTimeMillis());
        }
        try
        {
            m_stateLock.unlockReadLock();
        }
        catch ( IllegalMonitorStateException e )
        {
            logLockingInfo();
            throw e;
        }
        catch ( IllegalStateException e ) //for EDU lock
        {
            logLockingInfo();
            throw e;
        }
    }

    private void logLockingInfo()
    {
        StringBuffer b = new StringBuffer( "Locking activity before IllegalMonitorStateException: \n" );
        for (Iterator i = lockingActivity.iterator(); i.hasNext();)
        {
            b.append( "  " ).append( i.next() ).append( "\n" );
        }
        log( LogService.LOG_ERROR, b.toString(), null );
        dumpThreads();
    }

    final void obtainWriteLock( String source )
    {
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            lockingActivity.add( "obtainWriteLock from: " +  source + " readLocks: " + m_stateLock.getReadHoldCount() + " writeLocks: " + m_stateLock.getWriteHoldCount() + " thread: " + Thread.currentThread() + " time: " + System.currentTimeMillis());
        }
        try
        {
            if (!m_stateLock.tryWriteLock( m_timeout ) )
            {
                lockingActivity.add( "obtainWriteLock failure from: " +  source + " readLocks: " + m_stateLock.getReadHoldCount() + " writeLocks: " + m_stateLock.getWriteHoldCount() + " thread: " + Thread.currentThread() + " time: " + System.currentTimeMillis() + " Could not obtain write lock.");
                throw new IllegalStateException( "Could not obtain lock" );
            }
            lockingThread = Thread.currentThread();
            lockingStackTrace = new Exception("Write lock stack trace for thread: " + lockingThread);
        }
        catch ( InterruptedException e )
        {
            //TODO this is so wrong
            throw new IllegalStateException( "Could not obtain lock (Reason: " + e + ")" );
        }
    }

    final void deescalateLock( String source )
    {
        if ( isLogEnabled( LogService.LOG_DEBUG ) )
        {
            lockingActivity.add( "deescalateLock from: " +  source + " readLocks: " + m_stateLock.getReadHoldCount() + " writeLocks: " + m_stateLock.getWriteHoldCount() + " thread: " + Thread.currentThread() + " time: " + System.currentTimeMillis());
        }
        m_stateLock.deescalate();
        lockingThread = null;
        lockingStackTrace = null;
    }

    final void checkLocked()
    {
        if ( m_stateLock.getReadHoldCount() == 0 && m_stateLock.getWriteHoldCount() == 0 )
        {
            throw new IllegalStateException( "State lock should be held by current thread" );
        }
    }

    final boolean isWriteLocked()
    {
        return m_stateLock.getWriteHoldCount() > 0;
    }

    private void dumpThreads()
    {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        StringBuffer b = new StringBuffer( "Thread dump\n" );
        ThreadInfo[] infos = threadMXBean.dumpAllThreads( threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported() );
        for ( int i = 0; i < infos.length; i++ )
        {
            ThreadInfo ti = infos[i];
            b.append( "\n\nThreadId: " ).append( ti.getThreadId() ).append( " : name: " ).append( ti.getThreadName() ).append( " State: " ).append( ti.getThreadState() );
            b.append( "\n  LockInfo: " ).append( ti.getLockInfo() ).append( " LockOwnerId: " ).append( ti.getLockOwnerId() ).append( " LockOwnerName: ").append( ti.getLockOwnerName() );
            StackTraceElement[] stackTrace = ti.getStackTrace();
            for (int j = 0; j < stackTrace.length; j++ )
            {
                b.append( "\n  " ).append( stackTrace[j] );
            }
        }
        log( LogService.LOG_ERROR, b.toString(), null );
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
        final boolean release = obtainReadLock( "AbstractComponentManager.enable.1" );
        try
        {
            enableInternal();
            if ( !async )
            {
                activateInternal();
            }
        }
        finally
        {
            if ( release )
            {
                releaseReadLock( "AbstractComponentManager.enable.1" );
            }
        }

        if ( async )
        {
            m_activator.schedule( new Runnable()
            {
                public void run()
                {
                    final boolean release = obtainReadLock( "AbstractComponentManager.enable.2" );
                    try
                    {
                        activateInternal();
                    }
                    finally
                    {
                        if ( release )
                        {
                            releaseReadLock( "AbstractComponentManager.enable.2" );
                        }
                    }
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
        final boolean release = obtainReadLock( "AbstractComponentManager.disable.1" );
        try
        {
            if ( !async )
            {
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED );
            }
            disableInternal();
        }
        finally
        {
            if ( release )
            {
                releaseReadLock( "AbstractComponentManager.disable.1" );
            }
        }

        if ( async )
        {
            m_activator.schedule( new Runnable()
            {
                public void run()
                {
                    final boolean release = obtainReadLock( "AbstractComponentManager.disable.2" );
                    try
                    {
                        deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED );
                    }
                    finally
                    {
                        if ( release )
                        {
                            releaseReadLock( "AbstractComponentManager.disable.2" );
                        }
                    }
                }
            } );
        }
    }

    /**
     * Get the object that is implementing this descriptor
     *
     * @return the object that implements the services
     */
    abstract Object getInstance();

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
        final boolean release = obtainReadLock( "AbstractComponentManager.dispose.1" );
        try
        {
            disposeInternal( reason );
        }
        finally
        {
            if ( release )
            {
                releaseReadLock( "AbstractComponentManager.dispose.1" );
            }
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
        final BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            final BundleContext context = activator.getBundleContext();
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
        }

        // already disposed off component or bundle context is invalid
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
        m_state.enable( this );
    }

    final boolean activateInternal()
    {
        return m_state.activate( this );
    }

    final void deactivateInternal( int reason )
    {
        m_state.deactivate( this, reason );
    }

    final void disableInternal()
    {
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
        m_state.dispose( this, reason );
    }


    final ServiceReference getServiceReference()
    {
        // This method is not synchronized even though it accesses the state.
        // The reason for this is that we just want to have the state return
        // the service reference which comes from the service registration.
        // The only thing that may happen is that the service registration is
        // still set on this instance but the service has already been
        // unregistered. In this case an IllegalStateException may be thrown
        // which we just catch and ignore returning null
        State state = m_state;
        try
        {
            return state.getServiceReference( this );
        }
        catch ( IllegalStateException ise )
        {
            // may be thrown if the service has already been unregistered but
            // the service registration is still set on this component manager
            // we ignore this exception and assume there is no service reference
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
    /**
     * Registers the service on behalf of the component.
     *
     * @return The <code>ServiceRegistration</code> for the registered
     *      service or <code>null</code> if no service is registered.
     */
    protected void registerService()
    {
        if ( getComponentMetadata().getServiceMetadata() != null )
        {
            String[] provides = getComponentMetadata().getServiceMetadata().getProvides();
            registerService( provides );
        }
    }

    protected void registerService( String[] provides )
    {
        releaseReadLock( "register.service.1" );
        try
        {
            ServiceRegistration existing = ( ServiceRegistration ) m_serviceRegistration.get();
            if ( existing == null )
            {
                log( LogService.LOG_DEBUG, "registering services", null );

                // get a copy of the component properties as service properties
                final Dictionary serviceProperties = getServiceProperties();

                ServiceRegistration newRegistration = getActivator().getBundleContext().registerService(
                    provides,
                    getService(), serviceProperties );
                boolean weWon = m_serviceRegistration.compareAndSet( existing, newRegistration );
                if (weWon)
                {
                    return;
                }
                newRegistration.unregister();
            }
        }
        finally
        {
            obtainReadLock( "register.service.1" );
        }

    }

    /**
     * Registers the service on behalf of the component using the
     * {@link #registerService()} method. Also records the service
     * registration for later {@link #unregisterComponentService()}.
     * <p>
     * Due to more extensive locking FELIX-3317 is no longer relevant.
     *
     */
    final void registerComponentService()
    {
        registerService();
    }

    final void unregisterComponentService()
    {
        ServiceRegistration sr = ( ServiceRegistration ) m_serviceRegistration.get();

        if ( sr != null && m_serviceRegistration.compareAndSet( sr, null ) )
        {
            log( LogService.LOG_DEBUG, "Unregistering the services", null );
            sr.unregister();
        }
    }

    boolean initDependencyManagers()
    {
        if ( m_dependencyManagersInitialized )
        {
            return true;
        }
        Class implementationObjectClass;
        try
        {
            implementationObjectClass = getActivator().getBundleContext().getBundle().loadClass(
                    getComponentMetadata().getImplementationClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            log( LogService.LOG_ERROR, "Could not load implementation object class", e );
            return false;
        }
        m_componentMethods.initComponentMethods( this, m_componentMetadata, implementationObjectClass );

        for (Iterator it = m_dependencyManagers.iterator(); it.hasNext(); )
        {
            DependencyManager dependencyManager = ( DependencyManager ) it.next();

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
        Map old = ( Map ) m_dependencies_map.get();
        if ( old != null)
        {
            log( LogService.LOG_DEBUG, "dependency map already present, do not collect dependencies", null );
            return false;
        }
        initDependencyManagers();
        Map newDeps = new HashMap( );//<DependencyManager, Map<ServiceReference, RefPair>
        for (Iterator it = m_dependencyManagers.iterator(); it.hasNext(); )
        {
            DependencyManager dependencyManager = ( DependencyManager ) it.next();

            if (!dependencyManager.prebind( newDeps) )
            {
                //not actually satisfied any longer
                returnServices( newDeps );
                log( LogService.LOG_DEBUG, "Could not get required dependency for dependency manager: {0}",
                        new Object[] {dependencyManager}, null );
                throw new IllegalStateException( "Missing dependencies, not satisfied" );
            }
        }
        if ( !setDependencyMap( old, newDeps ) )
        {
            returnServices(newDeps);
            log( LogService.LOG_DEBUG, "Another thread set the dependency map already present, do not keep collected dependencies", null );
            return false;
        }
        log( LogService.LOG_DEBUG, "This thread collected dependencies", null );
        return true;
    }

    protected boolean setDependencyMap( Map old, Map newDeps )
    {
        return m_dependencies_map.compareAndSet( old, newDeps );
    }

    protected void unsetDependencyMap()
    {
        m_dependencies_map.set( null );
    }

    private void returnServices( Map deps )
    {
         for (Iterator it = deps.values().iterator(); it.hasNext(); )
         {
             Map refs = ( Map ) it.next();
             if ( refs != null )
             {
                 for (Iterator ri = refs.entrySet().iterator(); ri.hasNext(); )
                 {
                     Map.Entry entry = ( Map.Entry ) ri.next();
                     RefPair args = ( RefPair ) entry.getValue();
                     if ( args.getServiceObject() != null )
                     {
                         getActivator().getBundleContext().ungetService( (ServiceReference) entry.getKey() );
                     }
                 }
             }
         }
    }

    abstract void update( DependencyManager dependencyManager, ServiceReference ref );

    abstract void invokeBindMethod( DependencyManager dependencyManager, ServiceReference reference );

    abstract void invokeUnbindMethod( DependencyManager dependencyManager, ServiceReference oldRef );

    public static class RefPair
    {
        private final ServiceReference ref;
        private Object serviceObject;

        public RefPair( ServiceReference ref, Object serviceObject )
        {
            this.ref = ref;
            this.serviceObject = serviceObject;
        }

        public ServiceReference getRef()
        {
            return ref;
        }

        public Object getServiceObject()
        {
            return serviceObject;
        }

        public void setServiceObject( Object serviceObject )
        {
            this.serviceObject = serviceObject;
        }
    }

    Map getDependencyMap()
    {
        return ( Map ) m_dependencies_map.get();
    }

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


    final ServiceRegistration getServiceRegistration()
    {
        return ( ServiceRegistration ) m_serviceRegistration.get();
    }


    void clear()
    {
        // for some testing, the activator may be null
        if ( m_activator != null )
        {
            m_activator.unregisterComponentId( this );
            m_activator = null;
        }

        m_dependencyManagers.clear();
    }

    //<DependencyManager, Map<ServiceReference, RefPair>>
    protected Map getParameterMap()
    {
        return ( Map ) m_dependencies_map.get();
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
            activator.log( level, message, getComponentMetadata(), ex );
        }
    }

    public void log( int level, String message, Object[] arguments, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, arguments, getComponentMetadata(), ex );
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
                    for ( int i = 0; i < services.length; i++ )
                    {
                        final Permission perm = new ServicePermission( services[i], ServicePermission.REGISTER );
                        if ( !bundle.hasPermission( perm ) )
                        {
                            log( LogService.LOG_DEBUG, "Permission to register service {0} is denied", new Object[]
                                { services[i] }, null );
                            allowed = false;
                        }
                    }
                }
            }
        }

        // no security manager or no services to register
        return allowed;
    }


    private List loadDependencyManagers( ComponentMetadata metadata )
    {
        List depMgrList = new ArrayList(metadata.getDependencies().size());

        // If this component has got dependencies, create dependency managers for each one of them.
        if ( metadata.getDependencies().size() != 0 )
        {
            Iterator dependencyit = metadata.getDependencies().iterator();

            while ( dependencyit.hasNext() )
            {
                ReferenceMetadata currentdependency = (ReferenceMetadata) dependencyit.next();

                DependencyManager depmanager = new DependencyManager( this, currentdependency );

                depMgrList.add( depmanager );
            }
        }

        return depMgrList;
    }

    private void enableDependencyManagers() throws InvalidSyntaxException
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.enable();
        }
    }

    protected boolean verifyDependencyManagers( Dictionary properties )
    {
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();

            // ensure the target filter is correctly set
            dm.setTargetFilter( properties );

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
    Iterator getDependencyManagers()
    {
        return m_dependencyManagers.iterator();
    }

    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in reversed declaration order
     */
    Iterator getReversedDependencyManagers()
    {
        List list = new ArrayList( m_dependencyManagers );
        Collections.reverse( list );
        return list.iterator();
    }


    DependencyManager getDependencyManager(String name)
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
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
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.deactivate();
        }
    }

    private void disableDependencyManagers()
    {
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = (DependencyManager) it.next();
            dm.disable();
        }
    }

    public abstract boolean hasConfiguration();

    public abstract Dictionary getProperties();

    public abstract void setServiceProperties( Dictionary serviceProperties );

    /**
     * Returns the subset of component properties to be used as service
     * properties. These properties are all component properties where property
     * name does not start with dot (.), properties which are considered
     * private.
     */
    public Dictionary getServiceProperties()
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
    protected static Dictionary copyTo( Dictionary target, Dictionary source )
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
    protected static Dictionary copyTo( Dictionary target, final Dictionary source, final boolean allProps )
    {
        if ( target == null )
        {
            target = new Hashtable();
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
        log( LogService.LOG_DEBUG, "State transition : {0} -> {1}", new Object[]
            { m_state, newState }, null );
        m_state = newState;
    }

    public void setServiceProperties( MethodResult methodResult )
    {
        if ( methodResult.hasResult() )
        {
            Dictionary serviceProps = ( methodResult.getResult() == null) ? null : new Hashtable( methodResult.getResult() );
            setServiceProperties(serviceProps );
        }
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


        ServiceReference getServiceReference( AbstractComponentManager acm )
        {
            throw new IllegalStateException("getServiceReference" + this);
        }


        Object getService( ImmediateComponentManager dcm )
        {
            throw new IllegalStateException("getService" + this);
        }


        void ungetService( ImmediateComponentManager dcm )
        {
            throw new IllegalStateException("ungetService" + this);
        }


        void enable( AbstractComponentManager acm )
        {
            log( acm, "enable" );
        }


        boolean activate( AbstractComponentManager acm )
        {
            log( acm, "activate" );
            return false;
        }


        void deactivate( AbstractComponentManager acm, int reason )
        {
            log( acm, "deactivate (reason: " + reason + ")" );
        }


        void disable( AbstractComponentManager acm )
        {
            throw new IllegalStateException("disable" + this);
        }


        void dispose( AbstractComponentManager acm, int reason )
        {
            throw new IllegalStateException("dispose" + this);
        }


        private void log( AbstractComponentManager acm, String event )
        {
            acm.log( LogService.LOG_DEBUG, "Current state: {0}, Event: {1}", new Object[]
                { m_name, event }, null );
        }

        void doDeactivate( AbstractComponentManager acm, int reason )
        {
            try
            {
                acm.releaseReadLock( "AbstractComponentManager.State.doDeactivate.1" );
                acm.unregisterComponentService();
                acm.obtainWriteLock( "AbstractComponentManager.State.doDeactivate.1" );
                try
                {
                    acm.deleteComponent( reason );
                    acm.deactivateDependencyManagers();
                    acm.unsetDependencyMap();
                }
                finally
                {
                    acm.deescalateLock( "AbstractComponentManager.State.doDeactivate.1" );
                }
            }
            catch ( Throwable t )
            {
                acm.log( LogService.LOG_WARNING, "Component deactivation threw an exception", t );
            }
        }

        void doDisable( AbstractComponentManager acm )
        {
            // dispose and recreate dependency managers
            acm.disableDependencyManagers();

            // reset the component id now (a disabled component has none)
            acm.unregisterComponentId();
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


        void enable( AbstractComponentManager acm )
        {
            if ( !acm.isActivatorActive() )
            {
                acm.log( LogService.LOG_DEBUG, "Bundle's component activator is not active; not enabling component",
                    null );
                return;
            }

            acm.registerComponentId();
            try
            {
                acm.enableDependencyManagers();
                acm.changeState( Unsatisfied.getInstance() );
                acm.log( LogService.LOG_DEBUG, "Component enabled", null );
            }
            catch ( InvalidSyntaxException ise )
            {
                // one of the reference target filters is invalid, fail
                acm.log( LogService.LOG_ERROR, "Failed enabling Component", ise );
                acm.disableDependencyManagers();
                acm.unregisterComponentId();
                acm.changeState( Disabled.getInstance() );
            }
        }

        void deactivate( AbstractComponentManager acm, int reason )
        {
            doDeactivate( acm, reason );
        }

        void ungetService( ImmediateComponentManager dcm )
        {
            //do nothing, deactivate will unget all the services.
        }

        void dispose( AbstractComponentManager acm, int reason )
        {
            acm.log( LogService.LOG_DEBUG, "Disposing component (reason: " + reason + ")", null );
            acm.clear();
            acm.changeState( Disposed.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disposed", null );
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
         * @return
         */
        boolean activate( AbstractComponentManager acm )
        {
            if ( !acm.isActivatorActive() )
            {
                acm.log( LogService.LOG_DEBUG, "Bundle's component activator is not active; not activating component",
                    null );
                return true;
            }

            acm.log( LogService.LOG_DEBUG, "Activating component", null );

            // Before creating the implementation object, we are going to
            // test if we have configuration if such is required
            if ( !acm.hasConfiguration() && acm.getComponentMetadata().isConfigurationRequired() )
            {
                acm.log( LogService.LOG_DEBUG, "Missing required configuration, cannot activate", null );
                return true;
            }

            // Before creating the implementation object, we are going to
            // test that the bundle has enough permissions to register services
            if ( !acm.hasServiceRegistrationPermissions() )
            {
                acm.log( LogService.LOG_DEBUG, "Component is not permitted to register all services, cannot activate",
                    null );
                return true;
            }

            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            if ( !acm.verifyDependencyManagers( acm.getProperties() ) )
            {
                acm.log( LogService.LOG_DEBUG, "Not all dependencies satisfied, cannot activate", null );
                return true;
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

            acm.registerComponentService();

            // 1. Load the component implementation class
            // 2. Create the component instance and component context
            // 3. Bind the target services
            // 4. Call the activate method, if present
            if ( ( acm.isImmediate() || acm.getComponentMetadata().isFactory() ) )
            {
                acm.releaseReadLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                //don't collect dependencies for a factory component.
                try
                {
                    if ( !acm.collectDependencies() )
                    {
                        acm.log( LogService.LOG_DEBUG, "Not all dependencies collected, cannot create object (1)", null );
                        acm.obtainReadLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                        return false;
                    }
                    else
                    {
                        acm.log( LogService.LOG_DEBUG,
                                "activate won collecting dependencies, proceed to creating object.", null );

                    }
                }
                catch ( IllegalStateException e )
                {
                    acm.log( LogService.LOG_DEBUG, "Not all dependencies collected, cannot create object (2)", null );
                    acm.obtainReadLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                    return false;
                }
                catch ( Throwable t )
                {
                    acm.log( LogService.LOG_ERROR, "Unexpected throwable from attempt to collect dependencies", t );
                    acm.obtainReadLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                    return false;
                }
                acm.obtainWriteLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                try
                {
                    acm.changeState( acm.getActiveState() );
                    if ( !acm.createComponent() )
                    {
                        // component creation failed, not active now
                        acm.log( LogService.LOG_ERROR, "Component instance could not be created, activation failed", null );
                        acm.changeState( Unsatisfied.getInstance() );
                    }
                }
                finally
                {
                    acm.deescalateLock( "AbstractComponentManager.Unsatisfied.activate.1" );
                }

            }
            return true;

        }


        void disable( AbstractComponentManager acm )
        {
            acm.log( LogService.LOG_DEBUG, "Disabling component", null );
            doDisable( acm );

            // we are now disabled, ready for re-enablement or complete destroyal
            acm.changeState( Disabled.getInstance() );

            acm.log( LogService.LOG_DEBUG, "Component disabled", null );
        }

        void dispose( AbstractComponentManager acm, int reason )
        {
            doDisable( acm );
            acm.clear();   //content of Disabled.dispose
            acm.changeState( Disposed.getInstance() );
        }

    }

    protected static abstract class Satisfied extends State
    {
        protected Satisfied( String name, int state )
        {
            super( name, state );
        }


        ServiceReference getServiceReference( AbstractComponentManager acm )
        {
            ServiceRegistration sr = acm.getServiceRegistration();
            return sr == null ? null : sr.getReference();
        }


        void deactivate( AbstractComponentManager acm, int reason )
        {
            acm.log( LogService.LOG_DEBUG, "Deactivating component", null );

            // catch any problems from deleting the component to prevent the
            // component to remain in the deactivating state !
            doDeactivate(acm, reason);

            acm.changeState( Unsatisfied.getInstance() );
            acm.log( LogService.LOG_DEBUG, "Component deactivated", null );
        }

        void disable( AbstractComponentManager acm )
        {
            doDisable( acm );
            acm.changeState( Disabled.getInstance() );
        }

        void dispose( AbstractComponentManager acm, int reason )
        {
            doDeactivate( acm, reason );
            doDisable(acm);
            acm.clear();   //content of Disabled.dispose
            acm.changeState( Disposed.getInstance() );
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


        Object getService( ImmediateComponentManager dcm )
        {
            return dcm.getInstance();
        }


        void ungetService( ImmediateComponentManager dcm )
        {
            dcm.deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
            dcm.changeState( Registered.getInstance() );
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


        Object getService( ImmediateComponentManager dcm )
        {
            if ( dcm.createComponent() )
            {
                dcm.changeState( Active.getInstance() );
                return dcm.getInstance();
            }

            // log that the delayed component cannot be created (we don't
            // know why at this moment; this should already have been logged)
            dcm.log( LogService.LOG_ERROR, "Failed creating the component instance; see log for reason", null );

            // component could not really be created. This may be temporary
            // so we stay in the registered state but ensure the component
            // instance is deleted
            try
            {
                dcm.deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
            }
            catch ( Throwable t )
            {
                dcm.log( LogService.LOG_DEBUG, "Cannot delete incomplete component instance. Ignoring.", t );
            }

            // no service can be returned (be prepared for more logging !!)
            return null;
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
     * {@link #deactivate(AbstractComponentManager, int)} switches to the
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

        Object getService( ImmediateComponentManager dcm )
        {
            return dcm.getInstance();
        }


        void ungetService( ImmediateComponentManager dcm )
        {
            dcm.deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
            dcm.changeState( Registered.getInstance() );
        }

        void deactivate( AbstractComponentManager acm, int reason )
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

        boolean activate( AbstractComponentManager acm )
        {
            throw new IllegalStateException( "activate: " + this );
        }

        void deactivate( AbstractComponentManager acm, int reason )
        {
            throw new IllegalStateException( "deactivate: " + this );
        }

        void disable( AbstractComponentManager acm )
        {
            throw new IllegalStateException( "disable: " + this );
        }

        void dispose( AbstractComponentManager acm, int reason )
        {
            //factory instance can have dispose called with no effect. 112.5.5
        }

        void enable( AbstractComponentManager acm )
        {
            throw new IllegalStateException( "enable: " + this );
        }
    }

    private static interface LockWrapper
    {
        boolean tryReadLock( long milliseconds ) throws InterruptedException;
        long getReadHoldCount();
        void unlockReadLock();

        boolean tryWriteLock( long milliseconds ) throws InterruptedException;
        long getWriteHoldCount();
        void unlockWriteLock();
        void deescalate();


    }

    private static class JLock implements LockWrapper
    {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock( true );

        public boolean tryReadLock( long milliseconds ) throws InterruptedException
        {
             return lock.readLock().tryLock( milliseconds, TimeUnit.MILLISECONDS );
        }

        public long getReadHoldCount()
        {
            return lock.getReadHoldCount();
        }

        public void unlockReadLock()
        {
            lock.readLock().unlock();
        }

        public boolean tryWriteLock( long milliseconds ) throws InterruptedException
        {
            return lock.writeLock().tryLock( milliseconds, TimeUnit.MILLISECONDS );
        }

        public long getWriteHoldCount()
        {
            return lock.getWriteHoldCount();
        }

        public void unlockWriteLock()
        {
            lock.writeLock().unlock();
        }

        public void deescalate()
        {
            lock.readLock().lock();
            lock.writeLock().unlock();
        }
    }

    private static class EDULock  implements LockWrapper
    {
        private final edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock lock = new edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock( );

        public boolean tryReadLock( long milliseconds ) throws InterruptedException
        {
             return lock.readLock().tryLock( milliseconds, edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS );
        }

        public long getReadHoldCount()
        {
            return lock.getReadHoldCount();
        }

        public void unlockReadLock()
        {
            lock.readLock().unlock();
        }

        public boolean tryWriteLock( long milliseconds ) throws InterruptedException
        {
            return lock.writeLock().tryLock( milliseconds, edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS );
        }

        public long getWriteHoldCount()
        {
            return lock.getWriteHoldCount();
        }

        public void unlockWriteLock()
        {
            lock.writeLock().unlock();
        }

        public void deescalate()
        {
            lock.readLock().lock();
            lock.writeLock().unlock();
        }
    }

    private interface AtomicReferenceWrapper
    {
        Object get();

        void set(Object o);

        boolean compareAndSet(Object expected, Object replacement);

    }

    private static class JAtomicReferenceWrapper implements AtomicReferenceWrapper
    {
        private final AtomicReference ref = new AtomicReference(  );

        public Object get()
        {
            return ref.get();
        }

        public void set(Object o)
        {
            ref.set( o );
        }

        public boolean compareAndSet(Object expected, Object replacement)
        {
            return ref.compareAndSet( expected, replacement );
        }
    }

    private static class EDUAtomicReferenceWrapper implements AtomicReferenceWrapper
    {
        private final edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference ref = new edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference(  );

        public Object get()
        {
            return ref.get();
        }

        public void set(Object o)
        {
            ref.set( o );
        }

        public boolean compareAndSet(Object expected, Object replacement)
        {
            return ref.compareAndSet( expected, replacement );
        }
    }

}
