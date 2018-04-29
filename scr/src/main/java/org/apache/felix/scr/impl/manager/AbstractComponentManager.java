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

<<<<<<< HEAD
=======
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
<<<<<<< HEAD
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
=======
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

<<<<<<< HEAD
import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.Activator;
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
=======
import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
<<<<<<< HEAD
import org.osgi.service.log.LogService;

=======
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
<<<<<<< HEAD
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

    private final boolean m_factoryInstance;
    // the ID of this component
    private long m_componentId;

    // The metadata
    private final ComponentMetadata m_componentMetadata;

    private final ComponentMethods m_componentMethods;
=======
public abstract class AbstractComponentManager<S> implements ComponentManager<S>
{
    //useful text for deactivation reason numbers
    static final String[] REASONS = { "Unspecified", "Component disabled", "Reference became unsatisfied",
            "Configuration modified", "Configuration deleted", "Component disabled", "Bundle stopped" };

    protected enum State
    {
        //disposed is a final state, normally only for factory components
        disposed(-1, false, false, false),
        //Since enable/disable on the component description are asynchronous, this tracks the component configuration state
        //which may differ while the enable/disable is occurring.
        disabled(-1, false, false, false),
        unsatisfiedReference(ComponentConfigurationDTO.UNSATISFIED_REFERENCE, true, false, false),
        satisfied(ComponentConfigurationDTO.SATISFIED, true, true, false),
        active(ComponentConfigurationDTO.ACTIVE, true, true, true),
        failed(ComponentConfigurationDTO.FAILED_ACTIVATION, true, true, false);

        private final int specState;

        private final boolean enabled;

        private final boolean satisfed;

        private final boolean actve;

        private State(int specState, boolean enabled, boolean satisfied, boolean active)
        {
            this.specState = specState;
            this.enabled = enabled;
            this.satisfed = satisfied;
            this.actve = active;
        }

        public int getSpecState()
        {
            return specState;
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public boolean isSatisfied()
        {
            return satisfed;
        }

        public boolean isActive()
        {
            return actve;
        }

    }

    protected final ComponentContainer<S> m_container;

    //true for normal spec factory instances. False for "persistent" factory instances and obsolete use of factory component with factory configurations.
    protected final boolean m_factoryInstance;
    // the ID of this component
    private long m_componentId;

    private final ComponentMethods<S> m_componentMethods;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    // The dependency managers that manage every dependency
    private final List<DependencyManager<S, ?>> m_dependencyManagers;

    private volatile boolean m_dependencyManagersInitialized;

<<<<<<< HEAD
    private volatile boolean m_dependenciesCollected;

    private final AtomicInteger m_trackingCount = new AtomicInteger( );

    // A reference to the BundleComponentActivator
    private BundleComponentActivator m_activator;

    // The ServiceRegistration is now tracked in the RegistrationManager
    
    private final ReentrantLock m_stateLock;

    protected volatile boolean m_enabled;
    protected final AtomicReference< CountDownLatch> m_enabledLatchRef = new AtomicReference<CountDownLatch>( new CountDownLatch(0) );

    protected volatile boolean m_internalEnabled;
    
    protected volatile boolean m_disposed;
    
=======
    private final AtomicInteger m_trackingCount = new AtomicInteger();

    // The ServiceRegistration is now tracked in the RegistrationManager

    private final ReentrantLock m_stateLock;

    /**
     * This latch prevents concurrent enable, disable, and reconfigure.  Since the enable and disable operations may use
     * two threads and the initiating thread does not wait for the operation to complete, we can't use a regular lock.
     */
    private final AtomicReference<Deferred<Void>> m_enabledLatchRef = new AtomicReference<>(
            new Deferred<Void>());

    private final AtomicReference<State> state = new AtomicReference<>(State.disabled);

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    //service event tracking
    private int m_floor;

    private volatile int m_ceiling;

    private final Lock m_missingLock = new ReentrantLock();
    private final Condition m_missingCondition = m_missingLock.newCondition();
<<<<<<< HEAD
    private final Set<Integer> m_missing = new TreeSet<Integer>( );

    volatile boolean m_activated;
    
    protected final ReentrantReadWriteLock m_activationLock = new ReentrantReadWriteLock();

    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     * @param componentMethods
     */
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, ComponentMethods componentMethods )
    {
        this( activator, metadata, componentMethods, false );
    }
    
    protected AbstractComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, ComponentMethods componentMethods, boolean factoryInstance )
    {
        m_factoryInstance = factoryInstance;
        m_activator = activator;
        m_componentMetadata = metadata;
        this.m_componentMethods = componentMethods;
        m_componentId = -1;

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
=======
    private final Set<Integer> m_missing = new TreeSet<>();

    protected final ReentrantReadWriteLock m_activationLock = new ReentrantReadWriteLock();

    private volatile String failureReason;

    /**
     * The constructor receives both the activator and the metadata
     *
     * @param container
     * @param componentMethods
     */
    protected AbstractComponentManager(ComponentContainer<S> container, ComponentMethods<S> componentMethods)
    {
        this(container, componentMethods, false);
    }

    protected AbstractComponentManager(ComponentContainer<S> container, ComponentMethods<S> componentMethods, boolean factoryInstance)
    {
        m_enabledLatchRef.get().resolve(null);
        m_factoryInstance = factoryInstance;
        m_container = container;
        m_componentMethods = componentMethods;
        m_componentId = -1;

        ComponentMetadata metadata = container.getComponentMetadata();

        m_dependencyManagers = loadDependencyManagers(metadata);

        m_stateLock = new ReentrantLock(true);

        // dump component details
        if (m_container.getLogger().isLogEnabled(LogService.LOG_DEBUG))
        {
            m_container.getLogger().log(LogService.LOG_DEBUG,
                    "Component created: DS={0}, implementation={1}, immediate={2}, default-enabled={3}, factory={4}, configuration-policy={5}, activate={6}, deactivate={7}, modified={8} configuration-pid={9}",
                    null,
                    metadata.getDSVersion(), metadata.getImplementationClassName(),
                            metadata.isImmediate(), metadata.isEnabled(), metadata.getFactoryIdentifier(),
                            metadata.getConfigurationPolicy(), metadata.getActivate(), metadata.getDeactivate(),
                            metadata.getModified(), metadata.getConfigurationPid());

            if (metadata.getServiceMetadata() != null)
            {
                m_container.getLogger().log(LogService.LOG_DEBUG,
                        "Component Services: scope={0}, services={1}",
                        null,
                        metadata.getServiceScope(), Arrays.toString(metadata.getServiceMetadata().getProvides()));
            }
            if (metadata.getProperties() != null)
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "Component Properties: {0}", null,
                         metadata.getProperties() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

    final long getLockTimeout()
    {
<<<<<<< HEAD
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.getConfiguration().lockTimeout();
=======
        //for tests....
        if (m_container.getActivator().getConfiguration() != null)
        {
            return m_container.getActivator().getConfiguration().lockTimeout();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return ScrConfiguration.DEFAULT_LOCK_TIMEOUT_MILLISECONDS;
    }

<<<<<<< HEAD
    private void obtainLock( Lock lock, String source )
    {
        try
        {
            if (!lock.tryLock( getLockTimeout(), TimeUnit.MILLISECONDS ) )
            {
                dumpThreads();
                throw new IllegalStateException( "Could not obtain lock" );
            }
        }
        catch ( InterruptedException e )
        {
            try
            {
                if (!lock.tryLock( getLockTimeout(), TimeUnit.MILLISECONDS ) )
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
=======
    private void obtainLock(Lock lock)
    {
        try
        {
            if (!lock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS))
            {
                dumpThreads();
                throw new IllegalStateException("Could not obtain lock");
            }
        }
        catch (InterruptedException e)
        {
            try
            {
                if (!lock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS))
                {
                    dumpThreads();
                    throw new IllegalStateException("Could not obtain lock");
                }
            }
            catch (InterruptedException e1)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted twice: Could not obtain lock");
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
            Thread.currentThread().interrupt();
        }
    }
<<<<<<< HEAD
    
    final void obtainActivationReadLock( String source )
    {
        obtainLock( m_activationLock.readLock(), source);
    }

    final void releaseActivationReadLock( String source )
    {
        m_activationLock.readLock().unlock();
    }
    
    final void obtainActivationWriteLock( String source )
    {
        obtainLock( m_activationLock.writeLock(), source);
    }

    final void releaseActivationWriteeLock( String source )
    {
        if ( m_activationLock.getWriteHoldCount() > 0 )
=======

    final void obtainActivationReadLock()
    {
        obtainLock(m_activationLock.readLock());
    }

    final void releaseActivationReadLock()
    {
        m_activationLock.readLock().unlock();
    }

    final void obtainActivationWriteLock()
    {
        obtainLock(m_activationLock.writeLock());
    }

    final void releaseActivationWriteeLock()
    {
        if (m_activationLock.getWriteHoldCount() > 0)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            m_activationLock.writeLock().unlock();
        }
    }
<<<<<<< HEAD
    
    final void obtainStateLock( String source )
    {
        obtainLock( m_stateLock, source );
    }

    final void releaseStateLock( String source )
=======

    final void obtainStateLock()
    {
        obtainLock(m_stateLock);
    }

    final void releaseStateLock()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        m_stateLock.unlock();
    }

    final boolean isStateLocked()
    {
        return m_stateLock.getHoldCount() > 0;
    }
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    final void dumpThreads()
    {
        try
        {
            String dump = new ThreadDump().call();
<<<<<<< HEAD
            log( LogService.LOG_DEBUG, dump, null );
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_DEBUG, "Could not dump threads", t );
=======
            m_container.getLogger().log(LogService.LOG_DEBUG, dump, null);
        }
        catch (Throwable t)
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Could not dump threads", t);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    //service event tracking
<<<<<<< HEAD
    void tracked( int trackingCount )
=======
    void tracked(int trackingCount)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        m_missingLock.lock();
        try
        {
<<<<<<< HEAD
            if (trackingCount == m_floor + 1 )
            {
                m_floor++;
                m_missing.remove( trackingCount );
            }
            else if ( trackingCount < m_ceiling )
            {
                m_missing.remove( trackingCount );
            }
            if ( trackingCount > m_ceiling )
            {
                for (int i = m_ceiling + 1; i < trackingCount; i++ )
                {
                    m_missing.add( i );
=======
            if (trackingCount == m_floor + 1)
            {
                m_floor++;
                m_missing.remove(trackingCount);
            }
            else if (trackingCount < m_ceiling)
            {
                m_missing.remove(trackingCount);
            }
            if (trackingCount > m_ceiling)
            {
                for (int i = m_ceiling + 1; i < trackingCount; i++)
                {
                    m_missing.add(i);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
                m_ceiling = trackingCount;
            }
            m_missingCondition.signalAll();
        }
        finally
        {
            m_missingLock.unlock();
        }
    }

    /**
<<<<<<< HEAD
     * We effectively maintain the set of completely processed service event tracking counts.  This method waits for all events prior 
     * to the parameter tracking count to complete, then returns.  See further documentation in EdgeInfo.
     * @param trackingCount
     */
    void waitForTracked( int trackingCount )
=======
     * We effectively maintain the set of completely processed service event tracking counts.  This method waits for all events prior
     * to the parameter tracking count to complete, then returns.  See further documentation in EdgeInfo.
     * @param trackingCount
     */
    void waitForTracked(int trackingCount)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        m_missingLock.lock();
        try
        {
<<<<<<< HEAD
            while ( m_ceiling  < trackingCount || ( !m_missing.isEmpty() && m_missing.iterator().next() < trackingCount))
            {
                log( LogService.LOG_DEBUG, "waitForTracked trackingCount: {0} ceiling: {1} missing: {2}",
                        new Object[] {trackingCount, m_ceiling, m_missing}, null );
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
                                new Object[] {trackingCount, m_ceiling, m_missing}, e1 );
=======
            while (m_ceiling < trackingCount || (!m_missing.isEmpty() && m_missing.iterator().next() < trackingCount))
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "waitForTracked trackingCount: {0} ceiling: {1} missing: {2}", null,
                         trackingCount, m_ceiling, m_missing );
                try
                {
                    if (!doMissingWait())
                    {
                        return;
                    }
                }
                catch (InterruptedException e)
                {
                    try
                    {
                        if (!doMissingWait())
                        {
                            return;
                        }
                    }
                    catch (InterruptedException e1)
                    {
                        m_container.getLogger().log(LogService.LOG_ERROR,
                                "waitForTracked interrupted twice: {0} ceiling: {1} missing: {2},  Expect further errors", e1,
                                 trackingCount, m_ceiling, m_missing );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally
        {
            m_missingLock.unlock();
        }
    }
<<<<<<< HEAD
    
    private boolean doMissingWait() throws InterruptedException
    {
        if ( !m_missingCondition.await( getLockTimeout(), TimeUnit.MILLISECONDS ))
        {
            log( LogService.LOG_ERROR, "waitForTracked timed out: {0} ceiling: {1} missing: {2},  Expect further errors",
                    new Object[] {m_trackingCount, m_ceiling, m_missing}, null );
=======

    private boolean doMissingWait() throws InterruptedException
    {
        if (!m_missingCondition.await(getLockTimeout(), TimeUnit.MILLISECONDS))
        {
            m_container.getLogger().log(LogService.LOG_ERROR, "waitForTracked timed out: {0} ceiling: {1} missing: {2},  Expect further errors", null,
                     m_trackingCount, m_ceiling, m_missing);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            dumpThreads();
            m_missing.clear();
            return false;
        }
        return true;
    }

<<<<<<< HEAD
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
        if (m_enabled)
        {
            return;
        }
        CountDownLatch enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            enableInternal();
            if ( !async )
            {
                activateInternal( m_trackingCount.get() );
=======
    //---------- Component ID management

    void registerComponentId()
    {
        this.m_componentId = m_container.getActivator().registerComponentId(this);
        this.m_container.getLogger().setComponentId(this.m_componentId);
    }

    void unregisterComponentId()
    {
        if (this.m_componentId >= 0)
        {
            m_container.getActivator().unregisterComponentId(this);
            this.m_componentId = -1;
            this.m_container.getLogger().setComponentId(this.m_componentId);
        }
    }

    //---------- Asynchronous frontend to state change methods ----------------
    private static final AtomicLong taskCounter = new AtomicLong();

    public final Promise<Void> enable(final boolean async)
    {
        Deferred<Void> enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if (!async)
            {
                enableInternal();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        finally
        {
<<<<<<< HEAD
            if ( !async )
            {
                enableLatch.countDown();
            }
            m_enabled = true;
        }

        if ( async )
        {
            final CountDownLatch latch = enableLatch;
            m_activator.schedule( new Runnable()
=======
            if (!async)
            {
                enableLatch.resolve(null);
            }
        }

        if (async)
        {
            final Deferred<Void> latch = enableLatch;
            m_container.getActivator().schedule(new Runnable()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {

                long count = taskCounter.incrementAndGet();

<<<<<<< HEAD
=======
                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public void run()
                {
                    try
                    {
<<<<<<< HEAD
                        activateInternal( m_trackingCount.get() );
                    }
                    finally
                    {
                        latch.countDown();
                    }
                }

=======
                        enableInternal();
                    }
                    finally
                    {
                        latch.resolve(null);
                    }
                }

                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public String toString()
                {
                    return "Async Activate: " + getComponentMetadata().getName() + " id: " + count;
                }
<<<<<<< HEAD
            } );
        }
=======
            });
        }
        return enableLatch.getPromise();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Use a CountDownLatch as a non-reentrant "lock" that can be passed between threads.
     * This lock assures that enable, disable, and reconfigure operations do not overlap.
<<<<<<< HEAD
     * 
     * @return the latch to count down when the operation is complete (in the calling or another thread)
     * @throws InterruptedException
     */
    CountDownLatch enableLatchWait()
    {
        CountDownLatch enabledLatch;
        CountDownLatch newEnabledLatch;
=======
     *
     * @return the latch to count down when the operation is complete (in the calling or another thread)
     * @throws InterruptedException
     */
    Deferred<Void> enableLatchWait()
    {
        Deferred<Void> enabledLatch;
        Deferred<Void> newEnabledLatch;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        do
        {
            enabledLatch = m_enabledLatchRef.get();
            boolean waited = false;
            boolean interrupted = false;
<<<<<<< HEAD
            while ( !waited )
            {
                try
                {
                    enabledLatch.await();
                    waited = true;
                }
                catch ( InterruptedException e )
                {
                    interrupted = true;
                }
            }
            if ( interrupted )
            {
                Thread.currentThread().interrupt();
            }
            newEnabledLatch = new CountDownLatch(1);
        }
        while ( !m_enabledLatchRef.compareAndSet( enabledLatch, newEnabledLatch) );
        return newEnabledLatch;  
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
        if (!m_enabled)
        {
            return;
        }
        CountDownLatch enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if ( !async )
            {
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED, true, false );
            }
            disableInternal();
=======
            while (!waited)
            {
                try
                {
                    enabledLatch.getPromise().getValue();
                    waited = true;
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                }
                catch (InvocationTargetException e)
                {
                    //this is not going to happen
                }
            }
            if (interrupted)
            {
                Thread.currentThread().interrupt();
            }
            newEnabledLatch = new Deferred<>();
        }
        while (!m_enabledLatchRef.compareAndSet(enabledLatch, newEnabledLatch));
        return newEnabledLatch;
    }

    public final Promise<Void> disable(final boolean async)
    {
        Deferred<Void> enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if (!async)
            {
                disableInternal();
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        finally
        {
            if (!async)
            {
<<<<<<< HEAD
                enableLatch.countDown();
            }
            m_enabled = false;
        }

        if ( async )
        {
            final CountDownLatch latch = enableLatch;
            m_activator.schedule( new Runnable()
=======
                enableLatch.resolve(null);
            }
        }

        if (async)
        {
            final Deferred<Void> latch = enableLatch;
            m_container.getActivator().schedule(new Runnable()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {

                long count = taskCounter.incrementAndGet();

<<<<<<< HEAD
=======
                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public void run()
                {
                    try
                    {
<<<<<<< HEAD
                        deactivateInternal( ComponentConstants.DEACTIVATION_REASON_DISABLED, true, false );
                    }
                    finally
                    {
                        latch.countDown();
                    }
                }

=======
                        disableInternal();
                    }
                    finally
                    {
                        latch.resolve(null);
                    }
                }

                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public String toString()
                {
                    return "Async Deactivate: " + getComponentMetadata().getName() + " id: " + count;
                }

<<<<<<< HEAD
            } );
        }
=======
            });
        }
        return enableLatch.getPromise();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    // supports the ComponentInstance.dispose() method
    void dispose()
    {
<<<<<<< HEAD
        dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
=======
        dispose(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
    public void dispose( int reason )
    {
        deactivateInternal( reason, true, true );
    }
    
    <T> void registerMissingDependency( DependencyManager<S, T> dm, ServiceReference<T> ref, int trackingCount)
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.registerMissingDependency( dm, ref, trackingCount );
        }
=======
    public void dispose(int reason)
    {
        deactivateInternal(reason, true, true);
    }

    <T> void registerMissingDependency(DependencyManager<S, T> dm, ServiceReference<T> ref, int trackingCount)
    {
        m_container.getActivator().registerMissingDependency(dm, ref, trackingCount);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    //---------- Component interface ------------------------------------------

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public long getId()
    {
        return m_componentId;
    }

<<<<<<< HEAD
    public String getName() {
        return m_componentMetadata.getName();
    }

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Returns the <code>Bundle</code> providing this component. If the
     * component as already been disposed off, this method returns
     * <code>null</code>.
     */
    public Bundle getBundle()
    {
        final BundleContext context = getBundleContext();
<<<<<<< HEAD
        if ( context != null )
=======
        if (context != null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            try
            {
                return context.getBundle();
            }
<<<<<<< HEAD
            catch ( IllegalStateException ise )
=======
            catch (IllegalStateException ise)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                // if the bundle context is not valid any more
            }
        }
        // already disposed off component or bundle context is invalid
        return null;
    }
<<<<<<< HEAD
    
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
=======

    BundleContext getBundleContext()
    {
        return m_container.getActivator().getBundleContext();
    }

    protected boolean isImmediate()
    {
        return getComponentMetadata().isImmediate();

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public boolean isFactory()
    {
        return false;
    }

<<<<<<< HEAD
    public String[] getServices()
    {
        if ( m_componentMetadata.getServiceMetadata() != null )
        {
            return m_componentMetadata.getServiceMetadata().getProvides();
        }

        return null;
    }

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    //-------------- atomic transition methods -------------------------------

    final void enableInternal()
    {
<<<<<<< HEAD
        if ( m_disposed )
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
        // Before creating the implementation object, we are going to
        // test if we have configuration if such is required
        if ( hasConfiguration() || !getComponentMetadata().isConfigurationRequired() )
        {
            // Update our target filters.
            log( LogService.LOG_DEBUG, "Updating target filters", null );
            updateTargets( getProperties() );
        }

        m_internalEnabled = true;
        log( LogService.LOG_DEBUG, "Component enabled", null );
    }

    final void activateInternal( int trackingCount )
    {
        log( LogService.LOG_DEBUG, "ActivateInternal",
                null );
        if ( m_disposed )
        {
            log( LogService.LOG_DEBUG, "ActivateInternal: disposed",
                    null );
            return;
        }
        if ( m_activated ) {
            log( LogService.LOG_DEBUG, "ActivateInternal: already activated",
                    null );
            return;
        }
        if ( !isEnabled())
        {
            log( LogService.LOG_DEBUG, "Component is not enabled; not activating component",
                    null );
            return;
        }
        if ( !isActivatorActive() )
        {
            log( LogService.LOG_DEBUG, "Bundle's component activator is not active; not activating component",
                    null );
            return;
        }

        log( LogService.LOG_DEBUG, "Activating component from state {0}", new Object[] {getState()},  null );

        // Before creating the implementation object, we are going to
        // test if we have configuration if such is required
        if ( !hasConfiguration() && getComponentMetadata().isConfigurationRequired() )
        {
            log( LogService.LOG_DEBUG, "Missing required configuration, cannot activate", null );
            return;
        }

        // Before creating the implementation object, we are going to
        // test that the bundle has enough permissions to register services
        if ( !hasServiceRegistrationPermissions() )
        {
            log( LogService.LOG_DEBUG, "Component is not permitted to register all services, cannot activate",
                    null );
            return;
        }

        obtainActivationReadLock( "activateInternal" );
        try
        {
            // Double check conditions now that we have obtained the lock
            if ( m_disposed )
            {
                log( LogService.LOG_DEBUG, "ActivateInternal: disposed",
                        null );
                return;
            }
            if ( m_activated ) {
                log( LogService.LOG_DEBUG, "ActivateInternal: already activated",
                        null );
                return;
            }
            if ( !isEnabled() )
            {
                log( LogService.LOG_DEBUG, "Component is not enabled; not activating component",
                        null );
=======
        State previousState;
        if ((previousState = getState()) == State.disposed)
        {
            throw new IllegalStateException("enable: " + this);
        }
        if (!m_container.getActivator().isActive())
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Bundle's component activator is not active; not enabling component", null);
            return;
        }
        if (previousState.isEnabled())
        {
            m_container.getLogger().log(LogService.LOG_WARNING, "enable called but component is already in state {0}", null,
                     previousState);
            return;
        }

        registerComponentId();
        m_container.getLogger().log(LogService.LOG_DEBUG, "Updating target filters", null);
        updateTargets(getProperties());

        setState(previousState, State.unsatisfiedReference);
        m_container.getLogger().log(LogService.LOG_DEBUG, "Component enabled", null);
        activateInternal();
    }

    final void activateInternal()
    {
        m_container.getLogger().log(LogService.LOG_DEBUG, "ActivateInternal", null);
        State s = getState();
        if (s == State.disposed)
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "ActivateInternal: disposed", null);
            return;
        }
        if (s == State.active)
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "ActivateInternal: already activated", null);
            return;
        }
        if (!s.isEnabled())
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Component is not enabled; not activating component", null);
            return;
        }
        if (!m_container.getActivator().isActive())
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Bundle's component activator is not active; not activating component", null);
            return;
        }

        m_container.getLogger().log(LogService.LOG_DEBUG, "Activating component from state {0}", null,  getState() );

        // Before creating the implementation object, we are going to
        // test that the bundle has enough permissions to register services
        if (!hasServiceRegistrationPermissions())
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Component is not permitted to register all services, cannot activate", null);
            return;
        }

        obtainActivationReadLock();
        try
        {
            // Double check conditions now that we have obtained the lock
            s = getState();
            if (s == State.disposed)
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "ActivateInternal: disposed", null);
                return;
            }
            if (s == State.active)
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "ActivateInternal: already activated", null);
                return;
            }
            if (!s.isEnabled())
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "Component is not enabled; not activating component", null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                return;
            }
            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
<<<<<<< HEAD
            if ( !verifyDependencyManagers() )
            {
                log( LogService.LOG_DEBUG, "Not all dependencies satisfied, cannot activate", null );
                return;
            }

            if ( !registerService() )
=======
            if (!verifyDependencyManagers())
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "Not all dependencies satisfied, cannot activate", null);
                return;
            }

            if (!registerService())
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                //some other thread is activating us, or we got concurrently deactivated.
                return;
            }

<<<<<<< HEAD

            if ( ( isImmediate() || getComponentMetadata().isFactory() ) )
            {
                getServiceInternal();
=======
            if ((isImmediate() || getComponentMetadata().isFactory()))
            {
                ServiceRegistration<S> serviceRegistration = registrationManager.getServiceRegistration();
                if ( serviceRegistration != null )
                {
                    m_container.getActivator().enterCreate( serviceRegistration.getReference() );
                    try
                    {
                        getServiceInternal( serviceRegistration );
                    }
                    finally
                    {
                        m_container.getActivator().leaveCreate( serviceRegistration.getReference() );
                    }
                }
                else
                {
                    getServiceInternal( null );
                }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        finally
        {
<<<<<<< HEAD
            releaseActivationReadLock( "activateInternal" );
=======
            releaseActivationReadLock();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    /**
     * Handles deactivating, disabling, and disposing a component manager. Deactivating a factory instance
     * always disables and disposes it.  Deactivating a factory disposes it.
     * @param reason reason for action
     * @param disable whether to also disable the manager
     * @param dispose whether to also dispose of the manager
     */
<<<<<<< HEAD
    final void deactivateInternal( int reason, boolean disable, boolean dispose )
    {
        synchronized ( this )
        {
            if ( m_disposed )
            {
                return;
            }
            m_disposed = dispose;
        }
        log( LogService.LOG_DEBUG, "Deactivating component", null );

        // catch any problems from deleting the component to prevent the
        // component to remain in the deactivating state !
        obtainActivationReadLock( "deactivateInternal" );
        try
        {
            doDeactivate( reason, disable || m_factoryInstance );
        }
        finally 
        {
            releaseActivationReadLock( "deactivateInternal" );
        }
        if ( isFactory() || m_factoryInstance || dispose )
        {
            log( LogService.LOG_DEBUG, "Disposing component (reason: " + reason + ")", null );
=======
    final void deactivateInternal(int reason, boolean disable, boolean dispose)
    {
        if (!getState().isEnabled())
        {
            return;
        }
        State nextState = State.unsatisfiedReference;
        if (disable)
        {
            nextState = State.disabled;
        }
        if (dispose)
        {
            nextState = State.disposed;
        }
        m_container.getLogger().log(LogService.LOG_DEBUG, "Deactivating component", null);

        // catch any problems from deleting the component to prevent the
        // component to remain in the deactivating state !
        obtainActivationReadLock();
        try
        {
            //doDeactivate may trigger a state change from active to satisfied as the registration is removed.
            doDeactivate(reason, disable || m_factoryInstance);
            setState(getState(), nextState);
        }
        finally
        {
            releaseActivationReadLock();
        }
        if (isFactory() || m_factoryInstance || dispose)
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Disposing component (reason: " + reason + ")", null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            clear();
        }
    }

<<<<<<< HEAD
    private void doDeactivate( int reason, boolean disable )
    {
        try
        {
            if ( !unregisterService() )
            {
                log( LogService.LOG_DEBUG, "Component deactivation occuring on another thread", null );
            }
            obtainStateLock( "AbstractComponentManager.State.doDeactivate.1" );
            try
            {
                if ( m_activated )
                {
                    m_activated = false;
                    deleteComponent( reason );
                    deactivateDependencyManagers();
                    if ( disable )
                    {
                        disableDependencyManagers();
                    }
                    unsetDependenciesCollected();
=======
    private void doDeactivate(int reason, boolean disable)
    {
        try
        {
            if (!unregisterService())
            {
                m_container.getLogger().log(LogService.LOG_DEBUG, "Component deactivation occuring on another thread", null);
            }
            obtainStateLock();
            try
            {
                //              setState(previousState, State.unsatisfiedReference);
                deleteComponent(reason);
                deactivateDependencyManagers();
                if (disable)
                {
                    disableDependencyManagers();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
            finally
            {
<<<<<<< HEAD
                releaseStateLock( "AbstractComponentManager.State.doDeactivate.1" );
            }
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_WARNING, "Component deactivation threw an exception", t );
=======
                releaseStateLock();
            }
        }
        catch (Throwable t)
        {
            m_container.getLogger().log(LogService.LOG_WARNING, "Component deactivation threw an exception", t);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    final void disableInternal()
    {
<<<<<<< HEAD
        m_internalEnabled = false;
        if ( m_disposed )
        {
            throw new IllegalStateException( "Cannot disable a disposed component " + getName() );
        }
        unregisterComponentId();
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
=======
        deactivateInternal(ComponentConstants.DEACTIVATION_REASON_DISABLED, true, false);
        unregisterComponentId();
    }

    //---------- Component handling methods ----------------------------------

    protected abstract void deleteComponent(int reason);

    boolean getServiceInternal(ServiceRegistration<S> serviceRegistration)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    ComponentMethods getComponentMethods()
    {
        return m_componentMethods;
    }
    
    protected String[] getProvidedServices()
    {
        if ( getComponentMetadata().getServiceMetadata() != null )
=======

    ComponentMethods<S> getComponentMethods()
    {
        return m_componentMethods;
    }

    protected String[] getProvidedServices()
    {
        if (getComponentMetadata().getServiceMetadata() != null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            String[] provides = getComponentMetadata().getServiceMetadata().getProvides();
            return provides;
        }
        return null;
<<<<<<< HEAD
        
    }

    private final RegistrationManager<ServiceRegistration<S>> registrationManager = new RegistrationManager<ServiceRegistration<S>>()
=======

    }

    final RegistrationManager<ServiceRegistration<S>> registrationManager = new RegistrationManager<ServiceRegistration<S>>()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {

        @Override
        ServiceRegistration<S> register(String[] services)
        {
            BundleContext bundleContext = getBundleContext();
<<<<<<< HEAD
            if (bundleContext == null) 
=======
            if (bundleContext == null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                return null;
            }
            final Dictionary<String, Object> serviceProperties = getServiceProperties();
<<<<<<< HEAD
            ServiceRegistration<S> serviceRegistration = ( ServiceRegistration<S> ) bundleContext
                    .registerService( services, getService(), serviceProperties );
            return serviceRegistration;
=======
            try
            {
                @SuppressWarnings("unchecked")
                ServiceRegistration<S> serviceRegistration = (ServiceRegistration<S>) bundleContext.registerService(
                        services, getService(), serviceProperties);
                return serviceRegistration;
            }
            catch (ServiceException e)
            {
                log(LogService.LOG_ERROR, "Unexpected error registering component service with properties {0}", e, serviceProperties);
                return null;
            }
        }

        @Override
        void postRegister(ServiceRegistration<S> t)
        {
            AbstractComponentManager.this.postRegister();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        @Override
        void unregister(ServiceRegistration<S> serviceRegistration)
        {
<<<<<<< HEAD
=======
            AbstractComponentManager.this.preDeregister();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            serviceRegistration.unregister();
        }

        @Override
<<<<<<< HEAD
        void log(int level, String message, Object[] arguments, Throwable ex)
        {
            AbstractComponentManager.this.log(level, message, arguments, ex);
=======
        void log(int level, String message, Throwable ex, Object... arguments)
        {
            AbstractComponentManager.this.getLogger().log(level, message, ex, arguments);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        
    };
    
=======

    };
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * Registers the service on behalf of the component.
     *
     */
    protected boolean registerService()
    {
        String[] services = getProvidedServices();
<<<<<<< HEAD
        if ( services != null )
        {
            return registrationManager.changeRegistration( RegistrationManager.RegState.registered, services);
=======
        if (services != null)
        {
            return registrationManager.changeRegistration(RegistrationManager.RegState.registered, services);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return true;
    }

    protected boolean unregisterService()
    {
        String[] services = getProvidedServices();
<<<<<<< HEAD
        if ( services != null )
        {
            return registrationManager.changeRegistration( RegistrationManager.RegState.unregistered, services );
        }
        return true;
    }
    
=======
        if (services != null)
        {
            return registrationManager.changeRegistration(RegistrationManager.RegState.unregistered, services);
        }
        return true;
    }

    protected ServiceRegistration<S> getServiceRegistration()
    {
        return registrationManager.getServiceRegistration();
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    AtomicInteger getTrackingCount()
    {
        return m_trackingCount;
    }

<<<<<<< HEAD

    private void initDependencyManagers()
    {
        if ( m_dependencyManagersInitialized )
=======
    private void initDependencyManagers(final ComponentContextImpl<S> componentContext)
    {
        if (m_dependencyManagersInitialized)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            return;
        }
        final Bundle bundle = getBundle();
        if (bundle == null)
        {
<<<<<<< HEAD
            log( LogService.LOG_ERROR, "bundle shut down while trying to load implementation object class", null );
            throw new IllegalStateException("bundle shut down while trying to load implementation object class");
        }
        Class<?> implementationObjectClass;
        try
        {
            implementationObjectClass = bundle.loadClass(
                    getComponentMetadata().getImplementationClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            log( LogService.LOG_ERROR, "Could not load implementation object class {0}", 
                    new Object[] {getComponentMetadata().getImplementationClassName()}, e );
            throw new IllegalStateException("Could not load implementation object class "
                    + getComponentMetadata().getImplementationClassName());
        }
        m_componentMethods.initComponentMethods( m_componentMetadata, implementationObjectClass );

        for ( DependencyManager dependencyManager : m_dependencyManagers )
        {
            dependencyManager.initBindingMethods( m_componentMethods.getBindMethods( dependencyManager.getName() ) );
=======
            m_container.getLogger().log(LogService.LOG_ERROR, "bundle shut down while trying to load implementation object class", null);
            throw new IllegalStateException("bundle shut down while trying to load implementation object class");
        }
        Class<S> implementationObjectClass;
        try
        {
            implementationObjectClass = (Class<S>) bundle.loadClass(getComponentMetadata().getImplementationClassName());
        }
        catch (ClassNotFoundException e)
        {
            m_container.getLogger().log(LogService.LOG_ERROR, "Could not load implementation object class {0}",
                    e, getComponentMetadata().getImplementationClassName() );
            throw new IllegalStateException(
                    "Could not load implementation object class " + getComponentMetadata().getImplementationClassName());
        }
        m_componentMethods.initComponentMethods(getComponentMetadata(), implementationObjectClass, componentContext.getLogger());

        for (DependencyManager<S, ?> dependencyManager : m_dependencyManagers)
        {
            dependencyManager.initBindingMethods(m_componentMethods.getBindMethods(dependencyManager.getName()));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        m_dependencyManagersInitialized = true;
    }

    /**
     * Collect and store in m_dependencies_map all the services for dependencies, outside of any locks.
<<<<<<< HEAD
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
        }
    }

    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public boolean isLogEnabled( int level )
    {
        return Activator.isLogEnabled( level );
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
=======
     * @param componentContext possible instance key for prototype scope references
     *
     * @return true if all references can be collected,
     *   false if some dependency is no longer available.
     */
    protected boolean collectDependencies(ComponentContextImpl<S> componentContext)
    {
        initDependencyManagers(componentContext);
        for (DependencyManager<S, ?> dependencyManager : m_dependencyManagers)
        {
            if (!dependencyManager.prebind(componentContext))
            {
                //not actually satisfied any longer
                deactivateDependencyManagers();
                m_container.getLogger().log(LogService.LOG_DEBUG, "Could not get required dependency for dependency manager: {0}", null,
                         dependencyManager.getName());
                return false;
            }
        }
        m_container.getLogger().log(LogService.LOG_DEBUG, "This thread collected dependencies", null);
        return true;
    }

    /**
     * Invoke updated method
     * @return {@code true} if the component needs reactivation, {@code false} otherwise.
     */
    abstract <T> boolean invokeUpdatedMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair,
            int trackingCount);

    abstract <T> void invokeBindMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair,
            int trackingCount);

    abstract <T> void invokeUnbindMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRefPair,
            int trackingCount);

    void notifyWaiters()
    {
        if ( registrationManager.getServiceRegistration() != null )
        {
            //see if our service has been requested but returned null....
            m_container.getLogger().log( LogService.LOG_DEBUG, "Notifying possible clients that service might be available with activator {0}", null,
                m_container.getActivator()  );
            m_container.getActivator().missingServicePresent( registrationManager.getServiceRegistration().getReference() );
        }

    }

    //**********************************************************************************************************
    public ComponentActivator getActivator()
    {
        return m_container.getActivator();
    }

    synchronized void clear()
    {
        m_container.getActivator().unregisterComponentId(this);
    }

    public ComponentLogger getLogger() {
        return m_container.getLogger();
    }

    @Override
    public String toString()
    {
        return "Component: " + getComponentMetadata().getName() + " (" + getId() + ")";
    }

    private boolean hasServiceRegistrationPermissions()
    {
        boolean allowed = true;
        if (System.getSecurityManager() != null)
        {
            final ServiceMetadata serviceMetadata = getComponentMetadata().getServiceMetadata();
            if (serviceMetadata != null)
            {
                final String[] services = serviceMetadata.getProvides();
                if (services != null && services.length > 0)
                {
                    final Bundle bundle = getBundle();
                    for (String service : services)
                    {
                        final Permission perm = new ServicePermission(service, ServicePermission.REGISTER);
                        if (!bundle.hasPermission(perm))
                        {
                            m_container.getLogger().log(LogService.LOG_DEBUG, "Permission to register service {0} is denied", null,
                                    service );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            allowed = false;
                        }
                    }
                }
            }
        }

        // no security manager or no services to register
        return allowed;
    }

<<<<<<< HEAD

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
=======
    private List<DependencyManager<S, ?>> loadDependencyManagers(ComponentMetadata metadata)
    {
        List<DependencyManager<S, ?>> depMgrList = new ArrayList<>(
                metadata.getDependencies().size());

        // If this component has got dependencies, create dependency managers for each one of them.
        if (metadata.getDependencies().size() != 0)
        {
            int index = 0;
            for (ReferenceMetadata currentdependency : metadata.getDependencies())
            {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                DependencyManager<S, ?> depmanager = new DependencyManager(this, currentdependency, index++);

                depMgrList.add(depmanager);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }

        return depMgrList;
    }

<<<<<<< HEAD
    final void updateTargets(Dictionary<String, Object> properties)
    {
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            dm.setTargetFilter( properties );
=======
    final void updateTargets(Map<String, Object> properties)
    {
        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {
            dm.setTargetFilter(properties);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    protected boolean verifyDependencyManagers()
    {
<<<<<<< HEAD
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
=======
        State previousState = getState();
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {

            if (!dm.hasGetPermission())
            {
                // bundle has no service get permission
                if (dm.isOptional())
                {
                    m_container.getLogger().log(LogService.LOG_DEBUG, "No permission to get optional dependency: {0}; assuming satisfied",
                            null, dm.getName() );
                }
                else
                {
                    m_container.getLogger().log(LogService.LOG_DEBUG, "No permission to get mandatory dependency: {0}; assuming unsatisfied",
                            null, dm.getName() );
                    satisfied = false;
                }
            }
            else if (!dm.isSatisfied())
            {
                // bundle would have permission but there are not enough services
                m_container.getLogger().log(LogService.LOG_DEBUG, "Dependency not satisfied: {0}", null, dm.getName() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                satisfied = false;
            }
        }

<<<<<<< HEAD
=======
        //Only try to change the state if the satisfied attribute is different.
        //We only succeed if no one else has changed the state meanwhile.
        if (satisfied != previousState.isSatisfied())
        {
            setState(previousState, satisfied ? State.satisfied : State.unsatisfiedReference);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
=======
    @Override
    public List<? extends ReferenceManager<S, ?>> getReferenceManagers()
    {
        return m_dependencyManagers;
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in reversed declaration order
     */
    List<DependencyManager<S, ?>> getReversedDependencyManagers()
    {
<<<<<<< HEAD
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
=======
        List<DependencyManager<S, ?>> list = new ArrayList<>(m_dependencyManagers);
        Collections.reverse(list);
        return list;
    }

    DependencyManager<S, ?> getDependencyManager(String name)
    {
        for (ReferenceManager<S, ?> dm : getDependencyManagers())
        {
            if (name.equals(dm.getName()))
            {
                return (DependencyManager<S, ?>) dm;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }

        // not found
        return null;
    }

    private void deactivateDependencyManagers()
    {
<<<<<<< HEAD
        log( LogService.LOG_DEBUG, "Deactivating dependency managers", null);
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
=======
        m_container.getLogger().log(LogService.LOG_DEBUG, "Deactivating dependency managers", null);
        for (DependencyManager<S, ?> dm : getDependencyManagers())
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            dm.deactivate();
        }
    }

    private void disableDependencyManagers()
    {
<<<<<<< HEAD
        log( LogService.LOG_DEBUG, "Disabling dependency managers", null);
        AtomicInteger trackingCount = new AtomicInteger();
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            dm.unregisterServiceListener( trackingCount );
        }
    }

    public abstract boolean hasConfiguration();

    public abstract Dictionary<String, Object> getProperties();

    public abstract void setServiceProperties( Dictionary<String, Object> serviceProperties );
=======
        m_container.getLogger().log(LogService.LOG_DEBUG, "Disabling dependency managers", null);
        AtomicInteger trackingCount = new AtomicInteger();
        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {
            dm.unregisterServiceListener(trackingCount);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.manager.ComponentManager#getProperties()
     */
    @Override
    public abstract Map<String, Object> getProperties();

    public abstract void setServiceProperties(Dictionary<String, ?> serviceProperties);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * Returns the subset of component properties to be used as service
     * properties. These properties are all component properties where property
     * name does not start with dot (.), properties which are considered
     * private.
     */
    public Dictionary<String, Object> getServiceProperties()
    {
<<<<<<< HEAD
        return copyTo( null, getProperties(), false );
=======
        return copyTo(null, getProperties(), false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
<<<<<<< HEAD
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
=======
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
    protected static Dictionary<String, Object> copyTo(Dictionary<String, Object> target, final Map<String, ?> source,
            final boolean allProps)
    {
        if (target == null)
        {
            target = new Hashtable<>();
        }

        if (source != null && !source.isEmpty())
        {
            for (Map.Entry<String, ?> entry : source.entrySet())
            {
                // cast is save, because key must be a string as per the spec
                String key = entry.getKey();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, entry.getValue());
                }
            }
        }

        return target;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code> except for private
     * properties (whose name has a leading dot) which are only copied if the
     * <code>allProps</code> parameter is <code>true</code>.
<<<<<<< HEAD
     *
     * @param target    The <code>Dictionary</code> into which to copy the
     *                  properties. If <code>null</code> a new <code>Hashtable</code> is
     *                  created.
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
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
=======
    protected static Map<String, Object> copyToMap(final Dictionary<String, ?> source, final boolean allProps)
    {
        Map<String, Object> target = new HashMap<>();

        if (source != null && !source.isEmpty())
        {
            for (Enumeration<String> ce = source.keys(); ce.hasMoreElements();)
            {
                // cast is save, because key must be a string as per the spec
                String key = ce.nextElement();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, source.get(key));
                }
            }
        }

        return target;
    }

    protected static Dictionary<String, Object> copyToDictionary(final Dictionary<String, ?> source,
            final boolean allProps)
    {
        Hashtable<String, Object> target = new Hashtable<>();

        if (source != null && !source.isEmpty())
        {
            for (Enumeration<String> ce = source.keys(); ce.hasMoreElements();)
            {
                // cast is save, because key must be a string as per the spec
                String key = ce.nextElement();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, source.get(key));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
        }

        return target;
    }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     *
     */
    public ComponentMetadata getComponentMetadata()
    {
<<<<<<< HEAD
        return m_componentMetadata;
    }

    public int getState()
    {
        if (m_disposed)
        {
            return Component.STATE_DISPOSED;
        }
        if ( !m_internalEnabled)
        {
            return Component.STATE_DISABLED;
        }
        if ( getServiceRegistration() == null && (getProvidedServices() != null || !hasInstance()))
        {
            return Component.STATE_UNSATISFIED;
        }
        if ( isFactory() && !m_factoryInstance )
        {
            return Component.STATE_FACTORY;
        }
        if ( hasInstance() )
        {
            return Component.STATE_ACTIVE;
        }
        return Component.STATE_REGISTERED;
=======
        return m_container.getComponentMetadata();
    }

    @Override
    public int getSpecState()
    {
        return getState().getSpecState();
    }

    State getState()
    {
        State s = state.get();
        m_container.getLogger().log(LogService.LOG_DEBUG, "Querying state {0}", null, s );
        return s;
    }

    @Override
    public String getFailureReason() {
        return this.failureReason;
    }

    public void setFailureReason(final Throwable e)
    {
        if ( e != null )
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            this.failureReason = sw.toString();
        }
    }

    void setState(State previousState, State newState)
    {
        if (state.compareAndSet(previousState, newState))
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Changed state from {0} to {1}", null, previousState, newState );
            if ( newState != State.failed )
            {
                this.failureReason = null;
            }
            m_container.getActivator().updateChangeCount();
        }
        else
        {
            m_container.getLogger().log(LogService.LOG_DEBUG, "Did not change state from {0} to {1}: current state {2}",
                    null, previousState, newState, state.get() );
        }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    abstract boolean hasInstance();

<<<<<<< HEAD
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
    
=======
    public void setServiceProperties(MethodResult methodResult, Integer trackingCount)
    {
        if (methodResult.hasResult())
        {
            if (trackingCount != null)
            {
                tracked(trackingCount);
            }
            Dictionary<String, Object> serviceProps = (methodResult.getResult() == null) ? null
                    : new Hashtable<>(methodResult.getResult());
            setServiceProperties(serviceProps);
        }
    }

    abstract void postRegister();

    abstract void preDeregister();

    public abstract void reconfigure(Map<String, Object> configuration, boolean configurationDeleted, TargetedPID factoryPid);

    public abstract void getComponentManagers(List<AbstractComponentManager<S>> cms);

    @Override
    public ServiceReference<S> getRegisteredServiceReference()
    {
        return null;
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
