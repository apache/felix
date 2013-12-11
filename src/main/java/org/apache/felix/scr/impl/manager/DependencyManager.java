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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.helper.BindMethod;
import org.apache.felix.scr.impl.helper.BindMethods;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;


/**
 * The <code>DependencyManager</code> manages the references to services
 * declared by a single <code>&lt;reference&gt;</code element in component
 * descriptor.
 */
public class DependencyManager<S, T> implements Reference
{
    // mask of states ok to send events
    private static final int STATE_MASK = 
         Component.STATE_ACTIVE | Component.STATE_REGISTERED | Component.STATE_FACTORY;

    // the component to which this dependency belongs
    private final AbstractComponentManager<S> m_componentManager;

    // Reference to the metadata
    private final ReferenceMetadata m_dependencyMetadata;
    
    private final int m_index;

    private final Customizer<T> m_customizer;

    //only set once, but it's not clear there is enough other synchronization to get the correct object before it's used.
    private volatile BindMethods m_bindMethods;

    //reset on filter change
    private volatile ServiceTracker<T, RefPair<T>> m_tracker;

    // the target service filter string
    private volatile String m_target;

    // the target service filter
    private volatile Filter m_targetFilter;

    //private volatile boolean m_registered;

    /**
     * Constructor that receives several parameters.
     * @param dependency An object that contains data about the dependency
     * @param index index of the dependency manager in the metadata
     */
    DependencyManager( AbstractComponentManager<S> componentManager, ReferenceMetadata dependency, int index )
    {
        m_componentManager = componentManager;
        m_dependencyMetadata = dependency;
        m_index = index;
        m_customizer = newCustomizer();

        // dump the reference information if DEBUG is enabled
        if ( m_componentManager.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            m_componentManager
                .log(
                    LogService.LOG_DEBUG,
                    "Dependency Manager {0} created: interface={1}, filter={2}, policy={3}, cardinality={4}, bind={5}, unbind={6}",
                    new Object[]
                        { getName(), dependency.getInterface(), dependency.getTarget(), dependency.getPolicy(),
                            dependency.getCardinality(), dependency.getBind(), dependency.getUnbind() }, null );
        }
    }
    
    int getIndex() 
    {
        return m_index;
    }   

    /**
     * Initialize binding methods.
     */
    void initBindingMethods(BindMethods bindMethods)
    {
       m_bindMethods = bindMethods;
    }

    private interface Customizer<T> extends ServiceTrackerCustomizer<T, RefPair<T>>
    {
        /**
         * attempt to obtain the services from the tracked service references that will be used in inital bind calls
         * before activation.
         * @return true if there are enough services for activation.
         */
        boolean prebind();

        void close();

        Collection<RefPair<T>> getRefs( AtomicInteger trackingCount );

        boolean isSatisfied();
        
        void setTracker( ServiceTracker<T, RefPair<T>> tracker );

        void setTrackerOpened();

        void setPreviousRefMap( Map<ServiceReference<T>, RefPair<T>> previousRefMap );
    }

    private abstract class AbstractCustomizer implements Customizer<T>
    {
        private final Map<ServiceReference<T>, RefPair<T>> EMPTY_REF_MAP = Collections.emptyMap();

        private volatile boolean trackerOpened;

        private volatile Map<ServiceReference<T>, RefPair<T>> previousRefMap = EMPTY_REF_MAP;

        public void setTracker( ServiceTracker<T, RefPair<T>> tracker )
        {
            m_tracker = tracker;
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracker reset (closed)", new Object[] {getName()}, null );
            trackerOpened = false;
        }

        public boolean isSatisfied()
        {
            ServiceTracker<T, RefPair<T>> tracker = getTracker();
            if ( tracker == null)
            {
                return false;
            }
            if (isOptional())
            {
                return true;
            }            
            return !tracker.isEmpty();
        }

        protected ServiceTracker<T, RefPair<T>> getTracker()
        {
            return m_tracker;
        }

        /**
         *
         * @return whether the tracker
         */
        protected boolean isActive()
        {
            return getTracker().isActive();
        }

        protected boolean isTrackerOpened()
        {
            return trackerOpened;
        }

        public void setTrackerOpened()
        {
            trackerOpened = true;
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracker opened", new Object[] {getName()}, null );
        }

        protected void deactivateTracker()
        {
            ServiceTracker<T, RefPair<T>> tracker = getTracker();
            if ( tracker != null )
            {
                tracker.deactivate();
            }
        }

        protected Map<ServiceReference<T>, RefPair<T>> getPreviousRefMap()
        {
            return previousRefMap;
        }

        public void setPreviousRefMap( Map<ServiceReference<T>, RefPair<T>> previousRefMap )
        {
            if ( previousRefMap != null )
            {
                this.previousRefMap = previousRefMap;
            }
            else
            {
                this.previousRefMap = EMPTY_REF_MAP;
            }

        }

        protected void ungetService( RefPair<T> ref )
        {
            Object service = ref.unsetServiceObject();
            if ( service != null )
            {
                BundleContext bundleContext = m_componentManager.getBundleContext();
                if ( bundleContext != null )
                {
                    bundleContext.ungetService( ref.getRef() );
                }
            }
        }

        protected void tracked( int trackingCount )
        {
            m_componentManager.tracked( trackingCount );
        }

    }


    private class FactoryCustomizer extends AbstractCustomizer {

        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = new RefPair<T>( serviceReference  );
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            if ( !isOptional() )
            {
                m_componentManager.activateInternal( trackingCount );
            }
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            refPair.setDeleted( true );
            if ( !isOptional() )
            {
                if (getTracker().isEmpty())
                {
                    m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                }
            }
        }

        public boolean prebind()
        {
            boolean success = m_dependencyMetadata.isOptional() || !getTracker().isEmpty();
            AtomicInteger trackingCount = new AtomicInteger( );
            getTracker().getTracked( true, trackingCount );
            return success;
        }

        public void close()
        {
            deactivateTracker();
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            return Collections.emptyList();
        }
    }

    private class MultipleDynamicCustomizer extends AbstractCustomizer {

        private RefPair<T> lastRefPair;
        private int lastRefPairTrackingCount;

        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = getPreviousRefMap().get( serviceReference );
            if ( refPair == null )
            {
                refPair = new RefPair<T>( serviceReference  );
            }
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic added {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
        	boolean tracked = false;
            if ( getPreviousRefMap().remove( serviceReference ) == null )
            {
                if (isActive())
                {
                    m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic already active, binding {2}", new Object[] {getName(), trackingCount, serviceReference}, null );
                    getServiceObject( m_bindMethods.getBind(), refPair );
                    if ( !refPair.isFailed() )
                    {
                        m_componentManager.invokeBindMethod( DependencyManager.this, refPair, trackingCount );
                    }
                    else {
                        m_componentManager.registerMissingDependency( DependencyManager.this, serviceReference, trackingCount );
                    }
                }
                else if ( isTrackerOpened() && !isOptional() )
                {
                    m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic, activating", new Object[] {getName(), trackingCount}, null );
                    tracked( trackingCount );
                    tracked = true;
                    m_componentManager.activateInternal( trackingCount );
                }
                else 
                {
                    m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic, inactive, doing nothing: tracker opened: {2}, optional: {3}", new Object[] {getName(), trackingCount, isTrackerOpened(), isOptional()}, null );                    
                }
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic added {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            if ( !tracked )
            {
				tracked(trackingCount);
			}
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic modified {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            if (isActive())
            {
                m_componentManager.invokeUpdatedMethod( DependencyManager.this, refPair, trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic modified {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic removed {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            refPair.setDeleted( true );
            boolean unbind = isOptional() || !getTracker().isEmpty();
            if ( unbind )
            {
                if ( isActive() )
                {
                    m_componentManager.invokeUnbindMethod( DependencyManager.this, refPair, trackingCount );
                }
                m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic removed (unbind) {2}", new Object[] {getName(), trackingCount, serviceReference}, null );
                tracked( trackingCount );
            }
            else
            {
                lastRefPair = refPair;
                lastRefPairTrackingCount = trackingCount;
                tracked( trackingCount );
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                lastRefPair = null;
                m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic removed (deactivate) {2}", new Object[] {getName(), trackingCount, serviceReference}, null );
            }
            ungetService( refPair );
        }

        public boolean prebind()
        {
            boolean success = m_dependencyMetadata.isOptional();
            AtomicInteger trackingCount = new AtomicInteger( );
            SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( true, trackingCount );
            for (RefPair<T> refPair: tracked.values())
            {
                if (getServiceObject( m_bindMethods.getBind(), refPair ))
                {
                     success = true;
                }
                else
                {
                     m_componentManager.registerMissingDependency( DependencyManager.this, refPair.getRef(), trackingCount.get() );
                }
            }
            return success;
        }

        public void close()
        {
            AtomicInteger trackingCount = new AtomicInteger( );
            for ( RefPair<T> ref : getRefs( trackingCount ) )
            {
                ungetService( ref );
            }
            deactivateTracker();
        }


        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            if ( lastRefPair == null )
            {
                ServiceTracker<T, RefPair<T>> tracker = getTracker();
                if (tracker == null) {
                    trackingCount.set( lastRefPairTrackingCount );
                    return Collections.emptyList();                    
                }
                return getTracker().getTracked( true, trackingCount ).values();
            }
            else
            {
                trackingCount.set( lastRefPairTrackingCount );
                return Collections.singletonList( lastRefPair );
            }
        }
    }

    private class MultipleStaticGreedyCustomizer extends AbstractCustomizer {


        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = new RefPair<T>( serviceReference  );
            if (isActive())
            {
                 getServiceObject( m_bindMethods.getBind(), refPair );
            }
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy added {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
            if (isActive())
            {
                m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                        {getName(), m_dependencyMetadata.getInterface()}, null );
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                m_componentManager.activateInternal( trackingCount );

            }
            else if ( isTrackerOpened() &&  !isOptional() )
            {
                m_componentManager.activateInternal( trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy added {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy modified {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            if (isActive())
            {
                m_componentManager.invokeUpdatedMethod( DependencyManager.this, refPair, trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy modified {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy removed {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            refPair.setDeleted( true );
            tracked( trackingCount );
            if ( isActive() )
            {
                //deactivate while ref is still tracked
                m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                        {getName(), m_dependencyMetadata.getInterface()}, null );
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                //try to reactivate after ref is no longer tracked.
                m_componentManager.activateInternal( trackingCount );
            }
            else if ( !isOptional() && getTracker().isEmpty() )
            {
                m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                        {getName(), m_dependencyMetadata.getInterface()}, null );
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );                
            }
            //This is unlikely
            ungetService( refPair );
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy removed {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public boolean prebind()
        {
            boolean success = m_dependencyMetadata.isOptional();
            AtomicInteger trackingCount = new AtomicInteger( );
            SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( success || !getTracker().isEmpty(), trackingCount );
            for (RefPair<T> refPair: tracked.values())
            {
                success |= getServiceObject( m_bindMethods.getBind(), refPair );
                if ( refPair.isFailed() )
                {
                    m_componentManager.registerMissingDependency( DependencyManager.this, refPair.getRef(),
                            trackingCount.get() );
                }
            }
            return success;
        }

        public void close()
        {
            AtomicInteger trackingCount = new AtomicInteger( );
            for ( RefPair<T> ref: getRefs( trackingCount ))
            {
                ungetService( ref );
            }
            deactivateTracker();
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            ServiceTracker<T, RefPair<T>> tracker = getTracker();
            if (tracker == null) {
                return Collections.emptyList();                    
            }            
            return tracker.getTracked( null, trackingCount ).values();
        }
    }

    private class MultipleStaticReluctantCustomizer extends AbstractCustomizer {

        private final AtomicReference<Collection<RefPair<T>>> refs = new AtomicReference<Collection<RefPair<T>>>();
        private int trackingCount;

        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = new RefPair<T>( serviceReference  );
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant added {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
            if ( isTrackerOpened() && !isOptional() && !isActive())
            {
                m_componentManager.activateInternal( trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant added {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant modified {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            Collection<RefPair<T>> refs = this.refs.get();
            if (isActive() && refs.contains( refPair ))
            {                
                m_componentManager.invokeUpdatedMethod( DependencyManager.this, refPair, trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant modified {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant removed {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            refPair.setDeleted( true );
            tracked( trackingCount );
            Collection<RefPair<T>> refs = this.refs.get();
            if ( isActive() && refs != null )
            {
                if (refs.contains( refPair ))
                {
                    //we are tracking the used refs, so we can deactivate here.
                    m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                            { getName(), m_dependencyMetadata.getInterface() }, null );
                    m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );

                    // FELIX-2368: immediately try to reactivate
                    m_componentManager.activateInternal( trackingCount );

                }
            }
            else if ( !isOptional() && getTracker().isEmpty() )
            {
                m_componentManager.log( LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken", new Object[]
                        {getName(), m_dependencyMetadata.getInterface()}, null );
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );                
            }
            ungetService( refPair );
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant removed {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public boolean prebind()
        {
            boolean success = m_dependencyMetadata.isOptional();
            Collection<RefPair<T>> refs = this.refs.get();
            if (refs != null) {
                //another thread is concurrently opening, and it got done already
                for (RefPair<T> refPair: refs)
                {
                    success |= getServiceObject( m_bindMethods.getBind(), refPair );
                }
                return success;
            }
            refs = new ArrayList<RefPair<T>>();
            AtomicInteger trackingCount = new AtomicInteger( );
            SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( true, trackingCount );
            for (RefPair<T> refPair: tracked.values())
            {
                success |= getServiceObject( m_bindMethods.getBind(), refPair );
                refs.add(refPair) ;
            }
            if ( this.refs.compareAndSet( null, refs ) )
            {
                this.trackingCount = trackingCount.get();
            } 
            else 
            {
                //some other thread got done first.  If we have more refPairs, we might need to unget some services.
                Collection<RefPair<T>> actualRefs = this.refs.get();
                refs.removeAll( actualRefs );
                for (RefPair<T> ref: refs) 
                {
                    ungetService( ref );
                }
            }
            return success;
        }

        public void close()
        {
            Collection<RefPair<T>> refs = this.refs.getAndSet( null );
            if ( refs != null )
            {
                for ( RefPair<T> ref: refs )
                {
                    ungetService( ref );
                }
            }
            deactivateTracker();
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            trackingCount.set( this.trackingCount );
            Collection<RefPair<T>> refs = this.refs.get();
            return refs == null? Collections.<RefPair<T>>emptyList(): refs;
        }
    }

    private class SingleDynamicCustomizer extends AbstractCustomizer {

        private RefPair<T> refPair;
        private int trackingCount;

        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = getPreviousRefMap().get( serviceReference );
            if ( refPair == null )
            {
                refPair = new RefPair<T>( serviceReference  );
            }
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic added {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            boolean tracked = false;
            if ( getPreviousRefMap().remove( serviceReference ) == null )
            {
                if (isActive() )
                {
                    boolean invokeBind;
                    synchronized ( getTracker().tracked() )
                    {
                        invokeBind = this.refPair == null
                                || ( !isReluctant() && refPair.getRef().compareTo( this.refPair.getRef() ) > 0 );
                    }
                    if ( invokeBind )
                    {
                        getServiceObject( m_bindMethods.getBind(), refPair );
                        if ( !refPair.isFailed() )
                        {
                            m_componentManager.invokeBindMethod( DependencyManager.this, refPair, trackingCount );
                            if ( this.refPair != null )
                            {
                                m_componentManager.invokeUnbindMethod( DependencyManager.this, this.refPair,
                                        trackingCount );
                                closeRefPair();
                            }
                        }
                        else
                        {
                            m_componentManager.registerMissingDependency( DependencyManager.this, serviceReference,
                                    trackingCount );
                        }
                        this.refPair = refPair;
                    }
                }
                else if ( isTrackerOpened() && !isOptional() )
                {
                    tracked( trackingCount );
                    tracked = true;
                    m_componentManager.activateInternal( trackingCount );
                }
            }
            this.trackingCount = trackingCount;
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic added {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            if ( !tracked )
            {
                tracked(trackingCount);
            }
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic modified {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            boolean invokeUpdated;
            synchronized (getTracker().tracked())
            {
                invokeUpdated = isActive() && refPair == this.refPair;
            }
            if ( invokeUpdated )
            {
                m_componentManager.invokeUpdatedMethod( DependencyManager.this, refPair, trackingCount );
            }
            this.trackingCount = trackingCount;
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic modified {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic removed {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            refPair.setDeleted( true );
            boolean deactivate = false;
            boolean untracked = true;
            RefPair<T> oldRefPair = null;
            RefPair<T> nextRefPair = null;
            synchronized ( getTracker().tracked() )
            {
                if ( refPair == this.refPair && isActive() )
                {
                    if ( !getTracker().isEmpty() )
                    {
                        AtomicInteger trackingCount2 = new AtomicInteger();
                        SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( true,
                                trackingCount2 );
                        nextRefPair = tracked.values().iterator().next();
                    }
                    if ( isOptional() || nextRefPair != null )
                    {
                        oldRefPair = this.refPair;
                        this.refPair = null;
                    }
                    else 
                    {
                        deactivate = true;            //required and no replacement service, deactivate
                    }
                }
                else if ( !isOptional() && this.refPair == null && getTracker().isEmpty())
                {
                    deactivate = true;
                }
            }
            if ( nextRefPair != null )
            {
                if ( !getServiceObject( m_bindMethods.getBind(), nextRefPair ) )
                {
                    //TODO error???
                }
                if ( !nextRefPair.isFailed() )
                {
                    m_componentManager.invokeBindMethod( DependencyManager.this, nextRefPair,
                            trackingCount );
                }
            }

            if ( oldRefPair != null )
            {
                this.trackingCount = trackingCount;
                m_componentManager.invokeUnbindMethod( DependencyManager.this, oldRefPair, trackingCount );
                synchronized ( getTracker().tracked() )
                {
                    this.refPair = nextRefPair;
                }
                tracked( trackingCount );
                untracked = false;
            }
            else if ( deactivate )
            {
                this.trackingCount = trackingCount;
                tracked( trackingCount );
                untracked = false;
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
            }
            if ( oldRefPair != null )
            {
                ungetService( oldRefPair );
            }
            if (untracked) // not ours
            {
                this.trackingCount = trackingCount;
                tracked( trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic removed {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public boolean prebind()
        {
            RefPair<T> refPair = null;
            boolean success = m_dependencyMetadata.isOptional();
            AtomicInteger trackingCount = new AtomicInteger();
            synchronized ( getTracker().tracked() )
            {
                if ( success || !getTracker().isEmpty() )
                {
                    SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( true, trackingCount );
                    if ( !tracked.isEmpty() )
                    {
                        refPair = tracked.values().iterator().next();
                        this.refPair = refPair;
                    }
                }
            }
            if (refPair != null) 
            {
                success |= getServiceObject( m_bindMethods.getBind(), refPair );
                if ( refPair.isFailed() )
                {
                    m_componentManager.registerMissingDependency( DependencyManager.this, refPair.getRef(),
                            trackingCount.get() );
                }
            }
            return success;
        }

        public void close()
        {
            closeRefPair();
            deactivateTracker();
        }

        private void closeRefPair()
        {
            if ( refPair != null )
            {
                ungetService( refPair );
            }
            refPair = null;
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            Object monitor = getTracker() == null? null: getTracker().tracked();
            if ( monitor != null )
            {
                synchronized ( monitor )
                {
                    trackingCount.set( this.trackingCount );
                    return refPair == null? Collections.<RefPair<T>> emptyList(): Collections.singleton( refPair );
                }
            }
            else
            {
                return Collections.<RefPair<T>> emptyList();
            }
        }
    }

    private class SingleStaticCustomizer extends AbstractCustomizer
    {

        private RefPair<T> refPair;
        private int trackingCount;

        public RefPair<T> addingService( ServiceReference<T> serviceReference )
        {
            RefPair<T> refPair = new RefPair<T>( serviceReference );
            return refPair;
        }

        public void addedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic added {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            this.trackingCount = trackingCount;
            tracked( trackingCount );
            if ( isActive() )
            {
                boolean reactivate;
                synchronized (getTracker().tracked())
                {
                    reactivate = !isReluctant() && ( this.refPair == null || refPair.getRef().compareTo( this.refPair.getRef() ) > 0 );
                }
                if ( reactivate )
                {
                    m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                    m_componentManager.activateInternal( trackingCount );
                }
                else 
                {
                    m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic active but new {2} is worse match than old {3}", new Object[] {getName(), trackingCount, refPair, this.refPair, }, null );               
                }
            }
            else if (isTrackerOpened() && !isOptional() )
            {
                m_componentManager.activateInternal( trackingCount );
            }
            else 
            {
                m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic active: {2} trackerOpened: {3} optional: {4}", new Object[] {getName(), trackingCount, isActive(), isTrackerOpened(), isOptional()}, null );               
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic added {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public void modifiedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic modified {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            boolean invokeUpdated;
            synchronized (getTracker().tracked())
            {
                invokeUpdated = isActive() && refPair == this.refPair;
            }
            if ( invokeUpdated )
            {
                m_componentManager.invokeUpdatedMethod( DependencyManager.this, refPair, trackingCount );
            }
            this.trackingCount = trackingCount;
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic modified {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
            tracked( trackingCount );
        }

        public void removedService( ServiceReference<T> serviceReference, RefPair<T> refPair, int trackingCount )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic removed {2} (enter)", new Object[] {getName(), trackingCount, serviceReference}, null );
            refPair.setDeleted( true );
            this.trackingCount = trackingCount;
            tracked( trackingCount );
            boolean reactivate;
            synchronized (getTracker().tracked())
            {
                reactivate = ( isActive() && refPair == this.refPair) || ( !isOptional() && getTracker().isEmpty());
                if (!reactivate && refPair == this.refPair) {
                    this.refPair = null;
                }
            }
            if ( reactivate )
            {
                m_componentManager.deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                synchronized ( getTracker().tracked() )
                {
                    if (refPair == this.refPair)
                    {
                        this.refPair = null;
                    }
                }
                m_componentManager.activateInternal( trackingCount );
            }
            m_componentManager.log( LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic removed {2} (exit)", new Object[] {getName(), trackingCount, serviceReference}, null );
        }

        public boolean prebind()
        {
            boolean success = m_dependencyMetadata.isOptional();
            if ( success || !getTracker().isEmpty() )
            {
                RefPair<T> refPair = null;
                AtomicInteger trackingCount = new AtomicInteger();
                synchronized ( getTracker().tracked() )
                {
                    SortedMap<ServiceReference<T>, RefPair<T>> tracked = getTracker().getTracked( true, trackingCount );
                    if ( !tracked.isEmpty() )
                    {
                        refPair = tracked.values().iterator().next();
                        this.refPair = refPair;
                    }
                }
                if ( refPair != null )
                {
                    success |= getServiceObject( m_bindMethods.getBind(), refPair );
                    if ( refPair.isFailed() )
                    {
                        m_componentManager.registerMissingDependency( DependencyManager.this, refPair.getRef(),
                                trackingCount.get() );
                    }
                }
            }
            return success;
        }

        public void close()
        {
            ServiceTracker<T, RefPair<T>> tracker = getTracker();
            if ( tracker != null )
            {
                RefPair<T> ref;
                synchronized ( tracker.tracked() )
                {
                    ref = refPair;
                    refPair = null;
                }
                if ( ref != null )
                {
                    ungetService( ref );
                }
                tracker.deactivate();
            }
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            Object monitor = getTracker() == null? null: getTracker().tracked();
            if ( monitor != null )
            {
                synchronized ( monitor )
                {
                    trackingCount.set( this.trackingCount );
                    return refPair == null? Collections.<RefPair<T>> emptyList(): Collections.singleton( refPair );
                }
            }
            else
            {
                return Collections.<RefPair<T>> emptyList();
            }
        }
    }

    private class NoPermissionsCustomizer implements Customizer<T>
    {

        public boolean prebind()
        {
            return false;
        }

        public void close()
        {
        }

        public Collection<RefPair<T>> getRefs( AtomicInteger trackingCount )
        {
            return Collections.emptyList();
        }

        public boolean isSatisfied()
        {
            return isOptional();
        }

        public void setTracker( ServiceTracker<T, RefPair<T>> tRefPairServiceTracker )
        {
        }

        public void setTrackerOpened()
        {
        }

        public void setPreviousRefMap( Map<ServiceReference<T>, RefPair<T>> previousRefMap )
        {
        }

        public RefPair<T> addingService( ServiceReference<T> tServiceReference )
        {
            return null;
        }

        public void addedService( ServiceReference<T> tServiceReference, RefPair<T> service, int trackingCount )
        {
        }

        public void modifiedService( ServiceReference<T> tServiceReference, RefPair<T> service, int trackingCount )
        {
        }

        public void removedService( ServiceReference<T> tServiceReference, RefPair<T> service, int trackingCount )
        {
        }
    }

    //---------- Reference interface ------------------------------------------

    public String getServiceName()
    {
        return m_dependencyMetadata.getInterface();
    }

    public boolean isOptional()
    {
        return m_dependencyMetadata.isOptional();
    }


    public boolean isMultiple()
    {
        return m_dependencyMetadata.isMultiple();
    }


    public boolean isStatic()
    {
        return m_dependencyMetadata.isStatic();
    }

    public boolean isReluctant()
    {
        return m_dependencyMetadata.isReluctant();
    }

    public String getBindMethodName()
    {
        return m_dependencyMetadata.getBind();
    }


    public String getUnbindMethodName()
    {
        return m_dependencyMetadata.getUnbind();
    }


    public String getUpdatedMethodName()
    {
        return m_dependencyMetadata.getUpdated();
    }


    //---------- Service tracking support -------------------------------------


    void deactivate()
    {
        m_customizer.close();
    }


    /**
     * Returns the number of services currently registered in the system,
     * which match the service criteria (interface and optional target filter)
     * configured for this dependency. The number returned by this method has
     * no correlation to the number of services bound to this dependency
     * manager. It is actually the maximum number of services which may be
     * bound to this dependency manager.
     *
     * @see #isSatisfied()
     */
    int size()
    {
        AtomicInteger trackingCount = new AtomicInteger( );
        return m_tracker.getTracked( null, trackingCount ).size();
    }


    private ServiceReference<T>[] getFrameworkServiceReferences( String targetFilter )
    {
        if ( hasGetPermission() )
        {
            // component activator may be null if disposed concurrently
            BundleComponentActivator bca = m_componentManager.getActivator();
            if ( bca == null )
            {
                return null;
            }

            // get bundle context, may be null if component deactivated since getting bca
            BundleContext bc = bca.getBundleContext();
            if ( bc == null )
            {
                return null;
            }

            try
            {
                return ( ServiceReference<T>[] ) bc.getServiceReferences(
                    m_dependencyMetadata.getInterface(), targetFilter );
            }
            catch ( IllegalStateException ise )
            {
                // bundle context is not valid any longer, cannot log
            }
            catch ( InvalidSyntaxException ise )
            {
                m_componentManager.log( LogService.LOG_ERROR, "Unexpected problem with filter ''{0}''", new Object[]
                    { targetFilter }, ise );
                return null;
            }
        }

        m_componentManager.log( LogService.LOG_DEBUG, "No permission to access the services", null );
        return null;
    }


    /**
     * Returns a <code>ServiceReference</code> instances for a service
     * implementing the interface and complying to the (optional) target filter
     * declared for this dependency. If no matching service can be found
     * <code>null</code> is returned. If the configured target filter is
     * syntactically incorrect an error message is logged with the LogService
     * and <code>null</code> is returned. If multiple matching services are
     * registered the service with the highest service.ranking value is
     * returned. If multiple matching services have the same service.ranking
     * value, the service with the lowest service.id is returned.
     * <p>
     */
    private RefPair<T> getBestRefPair()
    {
        Collection<RefPair<T>> refs = m_customizer.getRefs( new AtomicInteger( ) );
        if (refs.isEmpty())
        {
            return null;
        }
        return refs.iterator().next();
    }


    /**
     * Returns the service instance for the service reference returned by the
     * {@link #getBestRefPair()} method. If this returns a
     * non-<code>null</code> service instance the service is then considered
     * bound to this instance.
     */
    T getService()
    {
        RefPair<T> sr = getBestRefPair();
        return getService( sr );
    }


    /**
     * Returns an array of service instances for the service references returned
     * by the customizer. If no services
     * match the criteria configured for this dependency <code>null</code> is
     * returned. All services returned by this method will be considered bound
     * after this method returns.
     */
    T[] getServices()
    {
        Collection<RefPair<T>> refs = m_customizer.getRefs(  new AtomicInteger( ) );
        List<T> services = new ArrayList<T>( refs.size() );
        for ( RefPair<T> ref: refs)
        {
            T service = getService(ref);
            if (service != null)
            {
                services.add( service );
            }
        }
        return services.isEmpty()? null: (T[])services.toArray( new Object[ services.size()] );
    }


    //---------- bound services maintenance -----------------------------------

    /**
     * Returns an array of <code>ServiceReference</code> instances of all
     * services this instance is bound to or <code>null</code> if no services
     * are actually bound.
     */
    public ServiceReference<T>[] getServiceReferences()
    {
        Collection<RefPair<T>> bound = m_customizer.getRefs(  new AtomicInteger( ) );
        if ( bound.isEmpty() )
        {
            return null;
        }
        ServiceReference<T>[] result = new ServiceReference[bound.size()];
        int i = 0;
        for (RefPair<T> ref: bound)
        {
            result[i++] = ref.getRef();
        }
        return result;
    }

    /**
     * a mistake, use getServiceReferences instead
     */
    @Deprecated
    public ServiceReference[] getBoundServiceReferences() 
    {
        return getServiceReferences();
    }
    
    /**
     * Returns the RefPair containing the given service reference and the bound service
     * or <code>null</code> if this is instance is not currently bound to that
     * service.
     *
     * @param serviceReference The reference to the bound service
     *
     * @return RefPair the reference and service for the reference
     *      if the service is bound or <code>null</code> if the service is not
     *      bound.
     */
    private RefPair<T> getRefPair( ServiceReference<T> serviceReference )
    {
        AtomicInteger trackingCount = new AtomicInteger( );
        return m_tracker.getTracked( null, trackingCount ).get( serviceReference );
    }


    /**
     * Returns the service described by the ServiceReference. If this instance
     * is already bound the given service, that bound service instance is
     * returned. Otherwise the service retrieved from the service registry
     * and kept as a bound service for future use.
     *
     * @param serviceReference The reference to the service to be returned
     *
     * @return The requested service or <code>null</code> if no service is
     *      registered for the service reference (any more).
     */
    T getService( ServiceReference<T> serviceReference )
    {
        // check whether we already have the service and return that one
        RefPair<T> refPair = getRefPair( serviceReference );
        return getService( refPair );
    }

    private T getService( RefPair<T> refPair )
    {
        if (refPair == null)
        {
            //we don't know about this reference
            return null;
        }
        T serviceObject;
        if ( (serviceObject = refPair.getServiceObject()) != null )
        {
            return serviceObject;
        }
        // otherwise acquire the service
        final BundleContext bundleContext = m_componentManager.getBundleContext();
        if (bundleContext == null)
        {
            m_componentManager.log( LogService.LOG_ERROR, "Bundle shut down while getting service {0} ({1}/{2,number,#})", new Object[]
                    { getName(), m_dependencyMetadata.getInterface(),
                        refPair.getRef().getProperty( Constants.SERVICE_ID ) }, null );
                return null;
        }
        try
        {
            serviceObject = bundleContext.getService( refPair.getRef() );
        }
        catch ( Exception e )
        {
            // caused by getService() called on invalid bundle context
            // or if there is a service reference cycle involving service
            // factories !
            m_componentManager.log( LogService.LOG_ERROR, "Failed getting service {0} ({1}/{2,number,#})", new Object[]
                { getName(), m_dependencyMetadata.getInterface(),
                    refPair.getRef().getProperty( Constants.SERVICE_ID ) }, e );
            return null;
        }

        // keep the service for later ungetting
        if ( !refPair.setServiceObject( serviceObject ) )
        {
            //another thread got the service first
            bundleContext.ungetService( refPair.getRef() );
        }

        // return the acquired service (may be null of course)
        //even if we did not set the service object, all the getService are for the same bundle so will have the same object.
        return serviceObject;
    }

    //---------- DependencyManager core ---------------------------------------

    /**
     * Returns the name of the service reference.
     */
    public String getName()
    {
        return m_dependencyMetadata.getName();
    }


    /**
     * Returns <code>true</code> if this dependency manager is satisfied, that
     * is if either the dependency is optional or the number of services
     * registered in the framework and available to this dependency manager is
     * not zero.
     */
    public boolean isSatisfied()
    {
        return m_customizer.isSatisfied();
    }


    /**
     * Returns <code>true</code> if the component providing bundle has permission
     * to get the service described by this reference.
     */
    public boolean hasGetPermission()
    {
        if ( System.getSecurityManager() != null )
        {
            Permission perm = new ServicePermission( getServiceName(), ServicePermission.GET );
            return m_componentManager.getBundle().hasPermission( perm );
        }

        // no security manager, hence permission given
        return true;
    }

    boolean prebind()
    {
        return m_customizer.prebind();
    }

    /**
     * initializes a dependency. This method binds all of the service
     * occurrences to the instance object
     * @param edgeInfo Edge info for the combination of this component instance and this dependency manager.
     *
     * @return true if the dependency is satisfied and at least the minimum
     *      number of services could be bound. Otherwise false is returned.
     */
    boolean open( S componentInstance, EdgeInfo edgeInfo )
    {
        // assume success to begin with: if the dependency is optional,
        // we don't care, whether we can bind a service. Otherwise, we
        // require at least one service to be bound, thus we require
        // flag being set in the loop below
        boolean success = m_dependencyMetadata.isOptional();
        AtomicInteger trackingCount =  new AtomicInteger( );
        Collection<RefPair<T>> refs;
        CountDownLatch openLatch;
        synchronized ( m_tracker.tracked() )
        {
            refs = m_customizer.getRefs( trackingCount );
            edgeInfo.setOpen( trackingCount.get() );
            openLatch = edgeInfo.getOpenLatch( );
        }
        m_componentManager.log( LogService.LOG_DEBUG,
            "For dependency {0}, optional: {1}; to bind: {2}",
            new Object[]{ getName(), success, refs }, null );
        for ( RefPair<T> refPair : refs )
        {
            if ( !refPair.isDeleted() && !refPair.isFailed() )
            {
                if ( !doInvokeBindMethod( componentInstance, refPair ) )
                {
                    m_componentManager.log( LogService.LOG_DEBUG,
                            "For dependency {0}, failed to invoke bind method on object {1}",
                            new Object[] {getName(), refPair}, null );

                }
                success = true;
            }
        }
        openLatch.countDown();
        return success;
    }

    /**
     * Revoke the given bindings. This method cannot throw an exception since
     * it must try to complete all that it can
     * @param componentInstance instance we are unbinding from.
     * @param edgeInfo EdgeInfo for the combination of this component instance and this dependency manager.
     */
    void close( S componentInstance, EdgeInfo edgeInfo )
    {
        // only invoke the unbind method if there is an instance (might be null
        // in the delayed component situation) and the unbind method is declared.
        boolean doUnbind = componentInstance != null && m_dependencyMetadata.getUnbind() != null;

        AtomicInteger trackingCount = new AtomicInteger();
        Collection<RefPair<T>> refPairs;
        CountDownLatch latch;
        synchronized ( m_tracker.tracked() )
        {
            refPairs = m_customizer.getRefs( trackingCount );
            edgeInfo.setClose( trackingCount.get() );
            latch = edgeInfo.getCloseLatch( );
        }

        m_componentManager.log( LogService.LOG_DEBUG,
                "DependencyManager: {0} close component unbinding from {1} at tracking count {2} refpairs: {3}",
                new Object[] {getName(), componentInstance, trackingCount.get(), refPairs}, null );
        m_componentManager.waitForTracked( trackingCount.get() );
        for ( RefPair<T> boundRef : refPairs )
        {
            if ( doUnbind && !boundRef.isFailed() )
            {
                invokeUnbindMethod( componentInstance, boundRef, trackingCount.get(), edgeInfo );
            }

        }
        latch.countDown();
    }

    public void invokeBindMethodLate( final ServiceReference<T> ref, int trackingCount )
    {
        if ( !isSatisfied() )
        {
            return;
        }
        if ( !isMultiple() )
        {
            Collection<RefPair<T>> refs = m_customizer.getRefs( new AtomicInteger( ) );
            if (refs.isEmpty())
            {
                return;
            }
            RefPair<T> test = refs.iterator().next();
            if ( ref != test.getRef())
            {
                //another ref is now better
                return;
            }
        }
        //TODO dynamic reluctant
        RefPair<T> refPair = m_tracker.getService( ref );
        if (refPair.getServiceObject() != null)
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                    "DependencyManager : late binding of service reference {1} skipped as service has already been located",
                    new Object[] {ref}, null );
            //something else got the reference and may be binding it.
            return;
        }
        getServiceObject( m_bindMethods.getBind(), refPair );
        m_componentManager.invokeBindMethod( this, refPair, trackingCount );
    }

    /**
     * Calls the bind method. In case there is an exception while calling the
     * bind method, the service is not considered to be bound to the instance
     * object
     * <p>
     * If the reference is singular and a service has already been bound to the
     * component this method has no effect and just returns <code>true</code>.
     *
     *
     *
     * @param componentInstance instance we are binding to
     * @param refPair the service reference, service object tuple.
     * @param trackingCount service event counter for this service.
     * @param edgeInfo EdgeInfo for the combination of this instance and this dependency manager.
     * @return true if the service should be considered bound. If no bind
     *      method is found or the method call fails, <code>true</code> is
     *      returned. <code>false</code> is only returned if the service must
     *      be handed over to the bind method but the service cannot be
     *      retrieved using the service reference.
     */
    boolean invokeBindMethod( S componentInstance, RefPair refPair, int trackingCount, EdgeInfo info )
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( componentInstance != null )
        {
            synchronized ( m_tracker.tracked() )
            {
                if (info.outOfRange( trackingCount ) )
                {
                    //ignore events before open started or we will have duplicate binds.
                    return true;
                }
            }
            //edgeInfo open has been set, so binding has started.
            return doInvokeBindMethod( componentInstance, refPair );

        }
        else
        {
            m_componentManager.log( LogService.LOG_DEBUG,
                    "DependencyManager : component not yet created, assuming bind method call succeeded",
                    null );

            return true;
        }
    }

    private boolean doInvokeBindMethod(S componentInstance, RefPair refPair)
    {
        MethodResult result = m_bindMethods.getBind().invoke( componentInstance, refPair, MethodResult.VOID, m_componentManager );
        if ( result == null )
        {
            return false;
        }
        m_componentManager.setServiceProperties( result );
        return true;
    }


    /**
     * Calls the updated method.
     *
     * @param componentInstance instance we are calling updated on.
     * @param refPair A service reference corresponding to the service whose service
     * @param edgeInfo EdgeInfo for the comibination of this instance and this dependency manager.
     */
    void invokeUpdatedMethod( S componentInstance, final RefPair<T> refPair, int trackingCount, EdgeInfo info )
    {
        if ( m_dependencyMetadata.getUpdated() == null )
        {
            return;
        }
        // The updated method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( componentInstance != null )
        {
            synchronized ( m_tracker.tracked() )
            {
                if (info.outOfRange( trackingCount ) )
                {
                    //ignore events after close started or we will have duplicate unbinds.
                    return;
                }
            }
            info.waitForOpen( m_componentManager, getName(), "invokeUpdatedMethod" );
            if ( !getServiceObject( m_bindMethods.getUpdated(), refPair ))
            {
                m_componentManager.log( LogService.LOG_WARNING,
                        "DependencyManager : invokeUpdatedMethod : Service not available from service registry for ServiceReference {0} for reference {1}",
                        new Object[] {refPair.getRef(), getName()}, null );
                return;

            }
            MethodResult methodResult = m_bindMethods.getUpdated().invoke( componentInstance, refPair, MethodResult.VOID, m_componentManager );
            if ( methodResult != null)
            {
                m_componentManager.setServiceProperties( methodResult );
            }
        }
        else
        {
            // don't care whether we can or cannot call the updated method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log( LogService.LOG_DEBUG,
                    "DependencyManager : Component not set, no need to call updated method", null );
        }
    }


    /**
     * Calls the unbind method.
     * <p>
     * If the reference is singular and the given service is not the one bound
     * to the component this method has no effect and just returns
     * <code>true</code>.
     *
     * @param componentInstance instance we are unbinding from
     * @param refPair A service reference, service pair that will be unbound
     * @param trackingCount service event count for this reference
     * @param info EdgeInfo for the combination of this instance and this dependency manager
     */
    void invokeUnbindMethod( S componentInstance, final RefPair<T> refPair, int trackingCount, EdgeInfo info )
    {
        // The unbind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if ( componentInstance != null )
        {
            synchronized ( m_tracker.tracked() )
            {
                if (info.beforeRange( trackingCount ))
                {
                    //never bound
                    return;
                }
            }
            info.waitForOpen( m_componentManager, getName(), "invokeUnbindMethod" );
            boolean outOfRange;
            synchronized ( m_tracker.tracked() )
            {
                outOfRange = info.afterRange( trackingCount );
            }
            if ( outOfRange )
            {
                //wait for unbinds to complete
                info.waitForClose( m_componentManager, getName(), "invokeUnbindMethod" );
                //ignore events after close started or we will have duplicate unbinds.
                return;
            }

            if ( !getServiceObject( m_bindMethods.getUnbind(), refPair ))
            {
                m_componentManager.log( LogService.LOG_WARNING,
                        "DependencyManager : invokeUnbindMethod : Service not available from service registry for ServiceReference {0} for reference {1}",
                        new Object[] {refPair.getRef(), getName()}, null );
                return;

            }
            MethodResult methodResult = m_bindMethods.getUnbind().invoke( componentInstance, refPair, MethodResult.VOID, m_componentManager );
            if ( methodResult != null )
            {
                m_componentManager.setServiceProperties( methodResult );
            }
        }
        else
        {
            // don't care whether we can or cannot call the unbind method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log( LogService.LOG_DEBUG,
                "DependencyManager : Component not set, no need to call unbind method", null );
        }
    }
    
    private long getLockTimeout()
    {
        return m_componentManager.getLockTimeout();
    }


    //------------- Service target filter support -----------------------------

    /**
     * Returns <code>true</code> if the <code>properties</code> can be
     * dynamically applied to the component to which the dependency manager
     * belongs.
     * <p>
     * This method applies the following heuristics (in the given order):
     * <ol>
     * <li>If there is no change in the target filter for this dependency, the
     * properties can be applied</li>
     * <li>If the dependency is static and there are changes in the target
     * filter we cannot dynamically apply the configuration because the filter
     * may (assume they do for simplicity here) cause the bindings to change.</li>
     * <li>If there is still at least one service matching the new target filter
     * we can apply the configuration because the depdency is dynamic.</li>
     * <li>If there are no more services matching the filter, we can still
     * apply the configuration if the dependency is optional.</li>
     * <li>Ultimately, if all other checks do not apply we cannot dynamically
     * apply.</li>
     * </ol>
     */
    boolean canUpdateDynamically( Dictionary<String, Object> properties )
    {
        // 1. no target filter change
        final String newTarget = ( String ) properties.get( m_dependencyMetadata.getTargetPropertyName() );
        final String currentTarget = getTarget();
        if ( ( currentTarget == null && newTarget == null )
            || ( currentTarget != null && currentTarget.equals( newTarget ) ) )
        {
            // can update if target filter is not changed, since there is
            // no change is service binding
            return true;
        }
        // invariant: target filter change

        // 2. if static policy, cannot update dynamically
        // (for simplicity assuming change in target service binding)
        if ( m_dependencyMetadata.isStatic() )
        {
            // cannot update if services are statically bound and the target
            // filter is modified, since there is (potentially at least)
            // a change is service bindings
            return false;
        }
        // invariant: target filter change + dynamic policy

        // 3. check optionality
        if ( m_dependencyMetadata.isOptional() )
        {
            // can update since even if no service matches the new filter, this
            // makes no difference because the dependency is optional
            return true;
        }
        // invariant: target filter change + mandatory + dynamic policy

        // 4. check target services matching the new filter
        ServiceReference<T>[] refs = getFrameworkServiceReferences( newTarget );
        if ( refs != null && refs.length > 0 )
        {
            // can update since there is at least on service matching the
            // new target filter and the services may be exchanged dynamically
            return true;
        }
        // invariant: target filter change + dynamic policy + no more matching service + required

        // 5. cannot dynamically update because the target filter results in
        // no more applicable services which is not acceptable
        return false;
    }


    /**
     * Sets the target filter from target filter property contained in the
     * properties. The filter is taken from a property whose name is derived
     * from the dependency name and the suffix <code>.target</code> as defined
     * for target properties on page 302 of the Declarative Services
     * Specification, section 112.6.
     *
     * @param properties The properties containing the optional target service
     *      filter property
     */
    void setTargetFilter( Dictionary<String, Object> properties )
    {
        setTargetFilter( ( String ) properties.get( m_dependencyMetadata.getTargetPropertyName() ) );
    }


    /**
     * Sets the target filter of this dependency to the new filter value. If the
     * new target filter is the same as the old target filter, this method has
     * not effect. Otherwise any services currently bound but not matching the
     * new filter are unbound. Likewise any registered services not currently
     * bound but matching the new filter are bound.
     *
     * @param target The new target filter to be set. This may be
     *      <code>null</code> if no target filtering is to be used.
     */
    private void setTargetFilter( String target)
    {
        // if configuration does not set filter, use the value from metadata
        if (target == null)
        {
            target = m_dependencyMetadata.getTarget();
        }
        // do nothing if target filter does not change
        if ( ( m_target == null && target == null ) || ( m_target != null && m_target.equals( target ) ) )
        {
            m_componentManager.log( LogService.LOG_DEBUG, "No change in target property for dependency {0}: currently registered: {1}", new Object[]
                    {getName(), m_tracker != null}, null );
            if (m_tracker != null)
            {
                return;
            }
        }
        m_target = target;
        String filterString = "(" + Constants.OBJECTCLASS + "=" + m_dependencyMetadata.getInterface() + ")";
        if (m_target != null)
        {
            filterString = "(&" + filterString + m_target + ")";
        }

        final ServiceTracker<T, RefPair<T>> oldTracker = m_tracker;
        AtomicInteger trackingCount = new AtomicInteger();
        SortedMap<ServiceReference<T>, RefPair<T>> refMap = unregisterServiceListener( trackingCount );
        if ( trackingCount.get() != -1 )
        {
            //wait for service events to complete before processing initial set from new tracker.
            m_componentManager.waitForTracked( trackingCount.get() );
        }
        m_componentManager.log( LogService.LOG_DEBUG, "Setting target property for dependency {0} to {1}", new Object[]
                {getName(), target}, null );
        BundleContext bundleContext = m_componentManager.getBundleContext();
        if ( bundleContext != null )
        {
            try
            {
                m_targetFilter = bundleContext.createFilter( filterString );
            }
            catch ( InvalidSyntaxException ise )
            {
                m_componentManager.log( LogService.LOG_ERROR, "Invalid syntax in target property for dependency {0} to {1}", new Object[]
                        {getName(), target}, null );
                
                //create a filter that will never be satisfied
                filterString = "(component.id=-1)";
                try
                {
                    m_targetFilter = bundleContext.createFilter( filterString );
                }
                catch ( InvalidSyntaxException e )
                {
                    //this should not happen
                    return;
                }

            }
        }
        else
        {
            m_componentManager.log( LogService.LOG_ERROR, "Bundle is shut down for dependency {0} to {1}", new Object[]
                    {getName(), target}, null );
            return;                
        }

        m_customizer.setPreviousRefMap( refMap );
        boolean initialActive = oldTracker != null && oldTracker.isActive();
        m_componentManager.log( LogService.LOG_DEBUG, "New service tracker for {0}, initial active: {1}, previous references: {2}", new Object[]
                {getName(), initialActive, refMap}, null );
        ServiceTracker<T, RefPair<T>> tracker = new ServiceTracker<T, RefPair<T>>( bundleContext, m_targetFilter, m_customizer, initialActive );
        m_customizer.setTracker( tracker );
        //        m_registered = true;
        tracker.open( m_componentManager.getTrackingCount() );
        m_customizer.setTrackerOpened();
        if ( oldTracker != null )
        {
            oldTracker.completeClose( refMap );
        }
        m_componentManager.log( LogService.LOG_DEBUG, "registering service listener for dependency {0}", new Object[]
                {getName()}, null );
    }

    private Customizer<T> newCustomizer()
    {
        Customizer<T> customizer;
        if (!hasGetPermission())
        {
            customizer = new NoPermissionsCustomizer();
            m_componentManager.log( LogService.LOG_INFO, "No permission to get services for {0}", new Object[]
                    {getName()}, null );
        }
        else if (m_componentManager.isFactory())
        {
            customizer = new FactoryCustomizer();
        }
        else if ( isMultiple() )
        {
            if ( isStatic() )
            {
                if ( isReluctant() )
                {
                    customizer = new MultipleStaticReluctantCustomizer();
                }
                else
                {
                    customizer = new MultipleStaticGreedyCustomizer();
                }
            }
            else
            {
                customizer = new MultipleDynamicCustomizer();
            }
        }
        else
        {
            if ( isStatic() )
            {
                customizer = new SingleStaticCustomizer();
            }
            else
            {
                customizer = new SingleDynamicCustomizer();
            }
        }
        return customizer;
    }

    SortedMap<ServiceReference<T>, RefPair<T>> unregisterServiceListener( AtomicInteger trackingCount )
    {
        SortedMap<ServiceReference<T>, RefPair<T>> refMap;
        ServiceTracker<T, RefPair<T>> tracker = m_tracker;
        if ( tracker != null )
        {
            refMap = tracker.close( trackingCount );
            m_tracker = null;
            m_componentManager.log( LogService.LOG_DEBUG, "unregistering service listener for dependency {0}", new Object[]
                    {getName()}, null );
        }
        else
        {
            refMap = new TreeMap<ServiceReference<T>, RefPair<T>>(Collections.reverseOrder());
            m_componentManager.log( LogService.LOG_DEBUG, " No existing service listener to unregister for dependency {0}", new Object[]
                    {getName()}, null );
            trackingCount.set( -1 );
        }
//        m_registered = false;
        return refMap;
    }


    /**
     * Returns the target filter of this dependency as a string or
     * <code>null</code> if this dependency has no target filter set.
     *
     * @return The target filter of this dependency or <code>null</code> if
     *      none is set.
     */
    public String getTarget()
    {
        return m_target;
    }


    public String toString()
    {
        return "DependencyManager: Component [" + m_componentManager + "] reference [" + getName() + "]";
    }

    boolean getServiceObject(BindMethod bindMethod, RefPair<T> refPair)
    {
        BundleContext bundleContext = m_componentManager.getBundleContext();
        if ( bundleContext != null )
        {
            return bindMethod.getServiceObject( refPair, bundleContext, m_componentManager );
        }
        else 
        {
            refPair.setFailed();
            return false;
        }
    }
}
