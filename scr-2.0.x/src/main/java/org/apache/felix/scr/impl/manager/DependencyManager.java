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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.impl.helper.Coercions;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ReferenceMethod;
import org.apache.felix.scr.impl.helper.ReferenceMethods;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata.ReferenceScope;
import org.apache.felix.scr.impl.metadata.ServiceMetadata.Scope;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;

/**
 * The <code>DependencyManager</code> manages the references to services
 * declared by a single <code>&lt;reference&gt;</code element in component
 * descriptor.
 */
public class DependencyManager<S, T> implements ReferenceManager<S, T>
{

    // the component to which this dependency belongs
    private final AbstractComponentManager<S> m_componentManager;

    // Reference to the metadata
    private final ReferenceMetadata m_dependencyMetadata;

    private final int m_index;

    private volatile Customizer<S, T> m_customizer;

    //only set once, but it's not clear there is enough other synchronization to get the correct object before it's used.
    private volatile ReferenceMethods m_bindMethods;

    //reset on filter change
    private volatile ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> m_tracker;

    // the target service filter string
    private volatile String m_target;

    private volatile int m_minCardinality;

    /**
     * Constructor that receives several parameters.
     * @param dependency An object that contains data about the dependency
     * @param index index of the dependency manager in the metadata
     */
    DependencyManager(AbstractComponentManager<S> componentManager, ReferenceMetadata dependency, int index)
    {
        m_componentManager = componentManager;
        m_dependencyMetadata = dependency;
        m_index = index;
        m_customizer = newCustomizer();

        m_minCardinality = defaultMinimumCardinality(dependency);

        // dump the reference information if DEBUG is enabled
        if (m_componentManager.isLogEnabled(LogService.LOG_DEBUG))
        {
            m_componentManager.log(LogService.LOG_DEBUG, "Dependency Manager created " + dependency.getDebugInfo(),
                null);
        }
    }

    private static int defaultMinimumCardinality(ReferenceMetadata dependency)
    {
        return dependency.isOptional() ? 0 : 1;
    }

    int getIndex()
    {
        return m_index;
    }

    /**
     * Initialize binding methods.
     */
    void initBindingMethods(ReferenceMethods bindMethods)
    {
        m_bindMethods = bindMethods;
    }

    private interface Customizer<S, T> extends ServiceTrackerCustomizer<T, RefPair<S, T>, ExtendedServiceEvent>
    {
        /**
         * attempt to obtain the services from the tracked service references that will be used in inital bind calls
         * before activation.
         * @param key TODO
         * @return true if there are enough services for activation.
         */
        boolean prebind(ComponentContextImpl<S> key);

        void close();

        Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount);

        boolean isSatisfied();

        void setTracker(ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker);

        void setTrackerOpened();

        void setPreviousRefMap(Map<ServiceReference<T>, RefPair<S, T>> previousRefMap);
    }

    private abstract class AbstractCustomizer implements Customizer<S, T>
    {
        private final Map<ServiceReference<T>, RefPair<S, T>> EMPTY_REF_MAP = Collections.emptyMap();

        private volatile boolean trackerOpened;

        private volatile Map<ServiceReference<T>, RefPair<S, T>> previousRefMap = EMPTY_REF_MAP;

        public void setTracker(ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker)
        {
            m_tracker = tracker;
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracker reset (closed)", new Object[] { getName() },
                null);
            trackerOpened = false;
        }

        public boolean isSatisfied()
        {
            ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
            if (tracker == null)
            {
                return false;
            }
            return cardinalitySatisfied(tracker.getServiceCount());
        }

        protected ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> getTracker()
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
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracker opened", new Object[] { getName() }, null);
        }

        protected void deactivateTracker()
        {
            ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
            if (tracker != null)
            {
                tracker.deactivate();
            }
        }

        protected Map<ServiceReference<T>, RefPair<S, T>> getPreviousRefMap()
        {
            return previousRefMap;
        }

        public void setPreviousRefMap(Map<ServiceReference<T>, RefPair<S, T>> previousRefMap)
        {
            if (previousRefMap != null)
            {
                this.previousRefMap = previousRefMap;
            }
            else
            {
                this.previousRefMap = EMPTY_REF_MAP;
            }

        }

        protected void ungetService(RefPair<S, T> ref)
        {
            Object service = ref.unsetServiceObject(null);
            if (service != null)
            {
                BundleContext bundleContext = m_componentManager.getBundleContext();
                if (bundleContext != null)
                {
                    bundleContext.ungetService(ref.getRef());
                }
            }
        }

        protected void tracked(int trackingCount)
        {
            m_componentManager.tracked(trackingCount);
        }

    }

    private class FactoryCustomizer extends AbstractCustomizer
    {

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = newRefPair(serviceReference);
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            if (cardinalityJustSatisfied(serviceCount))
            {
                m_componentManager.activateInternal();
            }
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            refPair.markDeleted();
            if (!cardinalitySatisfied(getTracker().getServiceCount()))
            {
                deactivateComponentManager();
            }
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            AtomicInteger trackingCount = new AtomicInteger();
            int serviceCount = getTracker().getTracked(true, trackingCount).size();
            return cardinalitySatisfied(serviceCount);
        }

        public void close()
        {
            deactivateTracker();
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            return Collections.emptyList();
        }
    }

    private class MultipleDynamicCustomizer extends AbstractCustomizer
    {

        private RefPair<S, T> lastRefPair;
        private int lastRefPairTrackingCount;

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = getPreviousRefMap().get(serviceReference);
            if (refPair == null)
            {
                refPair = newRefPair(serviceReference);
            }
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic added {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean tracked = false;
            if (getPreviousRefMap().remove(serviceReference) == null)
            {
                if (isActive())
                {
                    m_componentManager.log(LogService.LOG_DEBUG,
                        "dm {0} tracking {1} MultipleDynamic already active, binding {2}",
                        new Object[] { getName(), trackingCount, serviceReference }, null);
                    m_componentManager.invokeBindMethod(DependencyManager.this, refPair, trackingCount);
                    if (refPair.isFailed())
                    {
                        m_componentManager.registerMissingDependency(DependencyManager.this, serviceReference,
                            trackingCount);
                    }
                }
                else if (isTrackerOpened() && cardinalityJustSatisfied(serviceCount))
                {
                    m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic, activating",
                        new Object[] { getName(), trackingCount }, null);
                    tracked(trackingCount);
                    tracked = true;
                    m_componentManager.activateInternal();
                }
                else
                {
                    m_componentManager.log(LogService.LOG_DEBUG,
                        "dm {0} tracking {1} MultipleDynamic, inactive, doing nothing: tracker opened: {2}, optional: {3}",
                        new Object[] { getName(), trackingCount, isTrackerOpened(), isOptional() }, null);
                }
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic added {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            if (!tracked)
            {
                tracked(trackingCount);
            }
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic modified {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            if (isActive())
            {
                m_componentManager.invokeUpdatedMethod(DependencyManager.this, refPair, trackingCount);
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic modified {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            tracked(trackingCount);
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic removed {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            refPair.markDeleted();
            boolean unbind = cardinalitySatisfied(getTracker().getServiceCount());
            if (unbind)
            {
                if (isActive())
                {
                    m_componentManager.invokeUnbindMethod(DependencyManager.this, refPair, trackingCount);
                }
                m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleDynamic removed (unbind) {2}",
                    new Object[] { getName(), trackingCount, serviceReference }, null);
                tracked(trackingCount);
            }
            else
            {
                lastRefPair = refPair;
                lastRefPairTrackingCount = trackingCount;
                tracked(trackingCount);
                deactivateComponentManager();
                lastRefPair = null;
                m_componentManager.log(LogService.LOG_DEBUG,
                    "dm {0} tracking {1} MultipleDynamic removed (deactivate) {2}",
                    new Object[] { getName(), trackingCount, serviceReference }, null);
            }
            ungetService(refPair);
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            int serviceCount = 0;
            AtomicInteger trackingCount = new AtomicInteger();
            SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = getTracker().getTracked(true, trackingCount);
            List<RefPair<S, T>> failed = new ArrayList<RefPair<S, T>>();
            for (RefPair<S, T> refPair : tracked.values())
            {
                if (getServiceObject(key, m_bindMethods.getBind(), refPair))
                {
                    serviceCount++;
                }
                else
                {
                    failed.add(refPair);
                }
            }
            if (cardinalitySatisfied(serviceCount))
            {
                for (RefPair<S, T> refPair : failed)
                {
                    m_componentManager.registerMissingDependency(DependencyManager.this, refPair.getRef(),
                        trackingCount.get());
                }
                return true;
            }
            return false;
        }

        public void close()
        {
            AtomicInteger trackingCount = new AtomicInteger();
            for (RefPair<S, T> ref : getRefs(trackingCount))
            {
                ungetService(ref);
            }
            deactivateTracker();
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            if (lastRefPair == null)
            {
                ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
                if (tracker == null)
                {
                    trackingCount.set(lastRefPairTrackingCount);
                    return Collections.emptyList();
                }
                return getTracker().getTracked(null, trackingCount).values();
            }
            else
            {
                trackingCount.set(lastRefPairTrackingCount);
                return Collections.singletonList(lastRefPair);
            }
        }
    }

    private class MultipleStaticGreedyCustomizer extends AbstractCustomizer
    {

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = newRefPair(serviceReference);
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy added {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            tracked(trackingCount);
            if (isActive())
            {
                m_componentManager.log(LogService.LOG_DEBUG,
                    "Dependency Manager: Static dependency on {0}/{1} is broken",
                    new Object[] { getName(), m_dependencyMetadata.getInterface() }, null);
                deactivateComponentManager();
                //event may be null during initial operations.
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }

            }
            else if (isTrackerOpened() && cardinalityJustSatisfied(serviceCount))
            {
                m_componentManager.activateInternal();
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy added {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticGreedy modified {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean reactivate = false;
            if (isActive())
            {
                reactivate = m_componentManager.invokeUpdatedMethod(DependencyManager.this, refPair, trackingCount);
            }
            tracked(trackingCount);
            if (reactivate)
            {
                deactivateComponentManager();
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy modified {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy removed {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            refPair.markDeleted();
            tracked(trackingCount);
            if (isActive())
            {
                //deactivate while ref is still tracked
                m_componentManager.log(LogService.LOG_DEBUG,
                    "Dependency Manager: Static dependency on {0}/{1} is broken",
                    new Object[] { getName(), m_dependencyMetadata.getInterface() }, null);
                deactivateComponentManager();
                //try to reactivate after ref is no longer tracked.
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }
            }
            else if (!cardinalitySatisfied(getTracker().getServiceCount())) //may be called from an old tracker, so getTracker() may give a different answer
            {
                m_componentManager.log(LogService.LOG_DEBUG,
                    "Dependency Manager: Static dependency on {0}/{1} is broken",
                    new Object[] { getName(), m_dependencyMetadata.getInterface() }, null);
                deactivateComponentManager();
            }
            //This is unlikely
            ungetService(refPair);
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticGreedy removed {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            int serviceCount = 0;
            AtomicInteger trackingCount = new AtomicInteger();
            final ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
            SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = tracker.getTracked(
                cardinalitySatisfied(tracker.getServiceCount()), trackingCount);
            for (RefPair<S, T> refPair : tracked.values())
            {
                if (getServiceObject(key, m_bindMethods.getBind(), refPair))
                {
                    serviceCount++;
                }
            }
            return cardinalitySatisfied(serviceCount);
        }

        public void close()
        {
            AtomicInteger trackingCount = new AtomicInteger();
            for (RefPair<S, T> ref : getRefs(trackingCount))
            {
                ungetService(ref);
            }
            deactivateTracker();
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
            if (tracker == null)
            {
                return Collections.emptyList();
            }
            return tracker.getTracked(null, trackingCount).values();
        }
    }

    private class MultipleStaticReluctantCustomizer extends AbstractCustomizer
    {

        private final AtomicReference<Collection<RefPair<S, T>>> refs = new AtomicReference<Collection<RefPair<S, T>>>();
        private int trackingCount;

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = newRefPair(serviceReference);
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticReluctant added {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            tracked(trackingCount);
            if (isTrackerOpened() && cardinalityJustSatisfied(serviceCount) && !isActive())
            {
                m_componentManager.activateInternal();
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} MultipleStaticReluctant added {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticReluctant modified {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean reactivate = false;
            Collection<RefPair<S, T>> refs = this.refs.get();
            if (isActive() && refs.contains(refPair))
            {
                reactivate = m_componentManager.invokeUpdatedMethod(DependencyManager.this, refPair, trackingCount);
            }
            tracked(trackingCount);
            if (reactivate)
            {
                deactivateComponentManager();
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }
            }
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticReluctant modified {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticReluctant removed {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            refPair.markDeleted();
            tracked(trackingCount);
            Collection<RefPair<S, T>> refs = this.refs.get();
            if (isActive() && refs != null)
            {
                if (refs.contains(refPair))
                {
                    //we are tracking the used refs, so we can deactivate here.
                    m_componentManager.log(LogService.LOG_DEBUG,
                        "Dependency Manager: Static dependency on {0}/{1} is broken",
                        new Object[] { getName(), m_dependencyMetadata.getInterface() }, null);
                    deactivateComponentManager();

                    // FELIX-2368: immediately try to reactivate
                    if (event != null)
                    {
                        event.addComponentManager(m_componentManager);
                    }

                }
            }
            else if (!cardinalitySatisfied(getTracker().getServiceCount()))
            {
                m_componentManager.log(LogService.LOG_DEBUG,
                    "Dependency Manager: Static dependency on {0}/{1} is broken",
                    new Object[] { getName(), m_dependencyMetadata.getInterface() }, null);
                deactivateComponentManager();
            }
            ungetService(refPair);
            m_componentManager.log(LogService.LOG_DEBUG,
                "dm {0} tracking {1} MultipleStaticReluctant removed {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            int serviceCount = 0;
            Collection<RefPair<S, T>> refs = this.refs.get();
            if (refs != null)
            {
                //another thread is concurrently opening, and it got done already
                for (RefPair<S, T> refPair : refs)
                {
                    if (getServiceObject(key, m_bindMethods.getBind(), refPair))
                    {
                        serviceCount++;
                    }
                }
                return cardinalitySatisfied(serviceCount);
            }
            refs = new ArrayList<RefPair<S, T>>();
            AtomicInteger trackingCount = new AtomicInteger();
            SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = getTracker().getTracked(true, trackingCount);
            for (RefPair<S, T> refPair : tracked.values())
            {
                if (getServiceObject(key, m_bindMethods.getBind(), refPair))
                {
                    serviceCount++;
                }
                refs.add(refPair);
            }
            if (this.refs.compareAndSet(null, refs))
            {
                this.trackingCount = trackingCount.get();
            }
            else
            {
                //some other thread got done first.  If we have more refPairs, we might need to unget some services.
                Collection<RefPair<S, T>> actualRefs = this.refs.get();
                refs.removeAll(actualRefs);
                for (RefPair<S, T> ref : refs)
                {
                    ungetService(ref);
                }
            }
            return cardinalitySatisfied(serviceCount);
        }

        public void close()
        {
            Collection<RefPair<S, T>> refs = this.refs.getAndSet(null);
            if (refs != null)
            {
                for (RefPair<S, T> ref : refs)
                {
                    ungetService(ref);
                }
            }
            deactivateTracker();
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            trackingCount.set(this.trackingCount);
            Collection<RefPair<S, T>> refs = this.refs.get();
            return refs == null ? Collections.<RefPair<S, T>> emptyList() : refs;
        }
    }

    private class SingleDynamicCustomizer extends AbstractCustomizer
    {

        private RefPair<S, T> refPair;
        private int trackingCount;

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = getPreviousRefMap().get(serviceReference);
            if (refPair == null)
            {
                refPair = newRefPair(serviceReference);
            }
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic added {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean tracked = false;
            if (getPreviousRefMap().remove(serviceReference) == null)
            {
                if (isActive())
                {
                    boolean invokeBind;
                    synchronized (getTracker().tracked())
                    {
                        invokeBind = this.refPair == null
                            || (!isReluctant() && refPair.getRef().compareTo(this.refPair.getRef()) > 0);
                    }
                    if (invokeBind)
                    {
                        m_componentManager.invokeBindMethod(DependencyManager.this, refPair, trackingCount);
                        if (!refPair.isFailed())
                        {
                            if (this.refPair != null)
                            {
                                m_componentManager.invokeUnbindMethod(DependencyManager.this, this.refPair,
                                    trackingCount);
                                closeRefPair();
                            }
                        }
                        else if (cardinalitySatisfied(0))
                        {
                            m_componentManager.registerMissingDependency(DependencyManager.this, serviceReference,
                                trackingCount);
                        }
                        this.refPair = refPair;
                    }
                }
                else if (isTrackerOpened() && cardinalityJustSatisfied(serviceCount))
                {
                    tracked(trackingCount);
                    tracked = true;
                    m_componentManager.activateInternal();
                }
            }
            this.trackingCount = trackingCount;
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic added {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            if (!tracked)
            {
                tracked(trackingCount);
            }
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic modified {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean invokeUpdated;
            synchronized (getTracker().tracked())
            {
                invokeUpdated = isActive() && refPair == this.refPair;
            }
            if (invokeUpdated)
            {
                m_componentManager.invokeUpdatedMethod(DependencyManager.this, refPair, trackingCount);
            }
            this.trackingCount = trackingCount;
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic modified {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            tracked(trackingCount);
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic removed {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            refPair.markDeleted();
            boolean deactivate = false;
            boolean untracked = true;
            RefPair<S, T> oldRefPair = null;
            RefPair<S, T> nextRefPair = null;
            synchronized (getTracker().tracked())
            {
                if (refPair == this.refPair && isActive())
                {
                    if (!getTracker().isEmpty())
                    {
                        AtomicInteger trackingCount2 = new AtomicInteger();
                        SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = getTracker().getTracked(true, //TODO true here looks odd.
                            trackingCount2);
                        nextRefPair = tracked.values().iterator().next();
                    }

                    //n.b. we cannot use cardinalitySatisfied( serviceCount ) here as the call may come from an old tracker during target change.
                    if (isEffectivelyOptional() || nextRefPair != null)
                    {
                        oldRefPair = this.refPair;
                        this.refPair = null;
                    }
                    else
                    {
                        deactivate = true; //required and no replacement service, deactivate
                    }
                }
                else if (!cardinalitySatisfied(getTracker().getServiceCount()) && this.refPair == null)
                {
                    deactivate = true;
                }
            }
            if (nextRefPair != null)
            {
                m_componentManager.invokeBindMethod(DependencyManager.this, nextRefPair, trackingCount);
            }

            if (oldRefPair != null)
            {
                this.trackingCount = trackingCount;
                m_componentManager.invokeUnbindMethod(DependencyManager.this, oldRefPair, trackingCount);
                synchronized (getTracker().tracked())
                {
                    this.refPair = nextRefPair;
                }
                tracked(trackingCount);
                untracked = false;
            }
            else if (deactivate)
            {
                this.trackingCount = trackingCount;
                tracked(trackingCount);
                untracked = false;
                deactivateComponentManager();
            }
            if (oldRefPair != null)
            {
                ungetService(oldRefPair);
            }
            if (untracked) // not ours
            {
                this.trackingCount = trackingCount;
                tracked(trackingCount);
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleDynamic removed {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            RefPair<S, T> refPair = null;
            boolean success = cardinalitySatisfied(0);
            AtomicInteger trackingCount = new AtomicInteger();
            synchronized (getTracker().tracked())
            {
                if (success || !getTracker().isEmpty())
                {
                    SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = getTracker().getTracked(true,
                        trackingCount);
                    if (!tracked.isEmpty())
                    {
                        refPair = tracked.values().iterator().next();
                        this.refPair = refPair;
                    }
                }
            }
            if (refPair != null)
            {
                success |= getServiceObject(key, m_bindMethods.getBind(), refPair);
                if (refPair.isFailed() && cardinalitySatisfied(0))
                {
                    m_componentManager.registerMissingDependency(DependencyManager.this, refPair.getRef(),
                        trackingCount.get());
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
            if (refPair != null)
            {
                ungetService(refPair);
            }
            refPair = null;
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            Object monitor = getTracker() == null ? null : getTracker().tracked();
            if (monitor != null)
            {
                synchronized (monitor)
                {
                    trackingCount.set(this.trackingCount);
                    return refPair == null ? Collections.<RefPair<S, T>> emptyList() : Collections.singleton(refPair);
                }
            }
            else
            {
                return Collections.<RefPair<S, T>> emptyList();
            }
        }
    }

    private class SingleStaticCustomizer extends AbstractCustomizer
    {

        private RefPair<S, T> refPair;
        private int trackingCount;

        public RefPair<S, T> addingService(ServiceReference<T> serviceReference)
        {
            RefPair<S, T> refPair = newRefPair(serviceReference);
            return refPair;
        }

        public void addedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic added {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            this.trackingCount = trackingCount;
            tracked(trackingCount);
            if (isActive())
            {
                boolean reactivate;
                synchronized (getTracker().tracked())
                {
                    reactivate = !isReluctant()
                        && (this.refPair == null || refPair.getRef().compareTo(this.refPair.getRef()) > 0);
                }
                if (reactivate)
                {
                    deactivateComponentManager();
                    if (event != null)
                    {
                        event.addComponentManager(m_componentManager);
                    }
                }
                else
                {
                    m_componentManager.log(LogService.LOG_DEBUG,
                        "dm {0} tracking {1} SingleStatic active but new {2} is worse match than old {3}",
                        new Object[] { getName(), trackingCount, refPair, this.refPair, }, null);
                }
            }
            else if (isTrackerOpened() && cardinalityJustSatisfied(serviceCount))
            {
                m_componentManager.activateInternal();
            }
            else
            {
                m_componentManager.log(LogService.LOG_DEBUG,
                    "dm {0} tracking {1} SingleStatic active: {2} trackerOpened: {3} optional: {4}",
                    new Object[] { getName(), trackingCount, isActive(), isTrackerOpened(), isOptional() }, null);
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic added {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void modifiedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic modified {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            boolean invokeUpdated;
            final Object sync = getTracker().tracked();
            synchronized (sync)
            {
                invokeUpdated = isActive() && refPair == this.refPair;
            }
            boolean reactivate = false;
            if (invokeUpdated)
            {
                reactivate = m_componentManager.invokeUpdatedMethod(DependencyManager.this, refPair, trackingCount);
            }
            this.trackingCount = trackingCount;
            tracked(trackingCount);
            if (reactivate)
            {
                deactivateComponentManager();
                synchronized (sync)
                {
                    if (refPair == this.refPair)
                    {
                        this.refPair = null;
                    }
                }
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic modified {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public void removedService(ServiceReference<T> serviceReference, RefPair<S, T> refPair, int trackingCount,
            ExtendedServiceEvent event)
        {
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic removed {2} (enter)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
            refPair.markDeleted();
            this.trackingCount = trackingCount;
            tracked(trackingCount);
            boolean reactivate;
            final Object sync = getTracker().tracked();
            synchronized (sync)
            {
                reactivate = (isActive() && refPair == this.refPair)
                    || (!cardinalitySatisfied(getTracker().getServiceCount()));
                if (!reactivate && refPair == this.refPair)
                {
                    this.refPair = null;
                }
            }
            if (reactivate)
            {
                deactivateComponentManager();
                synchronized (sync)
                {
                    if (refPair == this.refPair)
                    {
                        this.refPair = null;
                    }
                }
                if (event != null)
                {
                    event.addComponentManager(m_componentManager);
                }
            }
            m_componentManager.log(LogService.LOG_DEBUG, "dm {0} tracking {1} SingleStatic removed {2} (exit)",
                new Object[] { getName(), trackingCount, serviceReference }, null);
        }

        public boolean prebind(ComponentContextImpl<S> key)
        {
            boolean success = cardinalitySatisfied(0);
            if (success || !getTracker().isEmpty())
            {
                RefPair<S, T> refPair = null;
                AtomicInteger trackingCount = new AtomicInteger();
                synchronized (getTracker().tracked())
                {
                    SortedMap<ServiceReference<T>, RefPair<S, T>> tracked = getTracker().getTracked(true,
                        trackingCount);
                    if (!tracked.isEmpty())
                    {
                        refPair = tracked.values().iterator().next();
                        this.refPair = refPair;
                    }
                }
                if (refPair != null)
                {
                    success |= getServiceObject(key, m_bindMethods.getBind(), refPair);
                    if (refPair.isFailed())
                    {
                        m_componentManager.registerMissingDependency(DependencyManager.this, refPair.getRef(),
                            trackingCount.get());
                    }
                }
            }
            return success;
        }

        public void close()
        {
            ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = getTracker();
            if (tracker != null)
            {
                RefPair<S, T> ref;
                synchronized (tracker.tracked())
                {
                    ref = refPair;
                    refPair = null;
                }
                if (ref != null)
                {
                    ungetService(ref);
                }
                tracker.deactivate();
            }
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            Object monitor = getTracker() == null ? null : getTracker().tracked();
            if (monitor != null)
            {
                synchronized (monitor)
                {
                    trackingCount.set(this.trackingCount);
                    return refPair == null ? Collections.<RefPair<S, T>> emptyList() : Collections.singleton(refPair);
                }
            }
            else
            {
                return Collections.<RefPair<S, T>> emptyList();
            }
        }
    }

    private class NoPermissionsCustomizer implements Customizer<S, T>
    {

        public boolean prebind(ComponentContextImpl<S> key)
        {
            return false;
        }

        public void close()
        {
        }

        public Collection<RefPair<S, T>> getRefs(AtomicInteger trackingCount)
        {
            return Collections.emptyList();
        }

        public boolean isSatisfied()
        {
            return isOptional();
        }

        public void setTracker(ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tRefPairServiceTracker)
        {
        }

        public void setTrackerOpened()
        {
        }

        public void setPreviousRefMap(Map<ServiceReference<T>, RefPair<S, T>> previousRefMap)
        {
        }

        public RefPair<S, T> addingService(ServiceReference<T> tServiceReference)
        {
            return null;
        }

        public void addedService(ServiceReference<T> tServiceReference, RefPair<S, T> service, int trackingCount,
            int serviceCount, ExtendedServiceEvent event)
        {
        }

        public void modifiedService(ServiceReference<T> tServiceReference, RefPair<S, T> service, int trackingCount,
            ExtendedServiceEvent event)
        {
        }

        public void removedService(ServiceReference<T> tServiceReference, RefPair<S, T> service, int trackingCount,
            ExtendedServiceEvent event)
        {
        }
    }

    private String getServiceName()
    {
        return m_dependencyMetadata.getInterface();
    }

    boolean isOptional()
    {
        return m_dependencyMetadata.isOptional();
    }

    private boolean isEffectivelyOptional()
    {
        return m_minCardinality == 0;
    }

    private boolean cardinalitySatisfied(int serviceCount)
    {
        return m_minCardinality <= serviceCount;
    }

    private boolean cardinalityJustSatisfied(int serviceCount)
    {
        return m_minCardinality == serviceCount;
    }

    private boolean isMultiple()
    {
        return m_dependencyMetadata.isMultiple();
    }

    private boolean isStatic()
    {
        return m_dependencyMetadata.isStatic();
    }

    private boolean isReluctant()
    {
        return m_dependencyMetadata.isReluctant();
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
        AtomicInteger trackingCount = new AtomicInteger();
        return m_tracker.getTracked(null, trackingCount).size();
    }

    private ServiceReference<T>[] getFrameworkServiceReferences(String targetFilter)
    {
        if (hasGetPermission())
        {
            // component activator may be null if disposed concurrently
            ComponentActivator bca = m_componentManager.getActivator();
            if (bca == null)
            {
                return null;
            }

            // get bundle context, may be null if component deactivated since getting bca
            BundleContext bc = bca.getBundleContext();
            if (bc == null)
            {
                return null;
            }

            try
            {
                return (ServiceReference<T>[]) bc.getServiceReferences(m_dependencyMetadata.getInterface(),
                    targetFilter);
            }
            catch (IllegalStateException ise)
            {
                // bundle context is not valid any longer, cannot log
            }
            catch (InvalidSyntaxException ise)
            {
                m_componentManager.log(LogService.LOG_ERROR, "Unexpected problem with filter ''{0}''",
                    new Object[] { targetFilter }, ise);
                return null;
            }
        }

        m_componentManager.log(LogService.LOG_DEBUG, "No permission to access the services", null);
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
    private RefPair<S, T> getBestRefPair()
    {
        Collection<RefPair<S, T>> refs = m_customizer.getRefs(new AtomicInteger());
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
     * @param key TODO
     */
    T getService(ComponentContextImpl<S> key)
    {
        RefPair<S, T> sr = getBestRefPair();
        return getService(key, sr);
    }

    /**
     * Returns an array of service instances for the service references returned
     * by the customizer. If no services
     * match the criteria configured for this dependency <code>null</code> is
     * returned. All services returned by this method will be considered bound
     * after this method returns.
     * @param key TODO
     */
    T[] getServices(ComponentContextImpl<S> key)
    {
        Collection<RefPair<S, T>> refs = m_customizer.getRefs(new AtomicInteger());
        List<T> services = new ArrayList<T>(refs.size());
        for (RefPair<S, T> ref : refs)
        {
            T service = getService(key, ref);
            if (service != null)
            {
                services.add(service);
            }
        }
        return services.isEmpty() ? null : (T[]) services.toArray(new Object[services.size()]);
    }

    //---------- bound services maintenance -----------------------------------

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.manager.ReferenceManager#getServiceReferences()
     */
    public List<ServiceReference<?>> getServiceReferences()
    {
        Collection<RefPair<S, T>> bound = m_customizer.getRefs(new AtomicInteger());
        List<ServiceReference<?>> result = new ArrayList<ServiceReference<?>>(bound.size());
        for (RefPair<S, T> ref : bound)
        {
            result.add(ref.getRef());
        }
        return result;
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
    private RefPair<S, T> getRefPair(ServiceReference<T> serviceReference)
    {
        final ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = m_tracker;
        if (tracker != null)
        {
            AtomicInteger trackingCount = new AtomicInteger();
            return tracker.getTracked(null, trackingCount).get(serviceReference);
        }
        return null;
    }

    /**
     * Returns the service described by the ServiceReference. If this instance
     * is already bound the given service, that bound service instance is
     * returned. Otherwise the service retrieved from the service registry
     * and kept as a bound service for future use.
     * @param key TODO
     * @param serviceReference The reference to the service to be returned
     *
     * @return The requested service or <code>null</code> if no service is
     *      registered for the service reference (any more).
     */
    T getService(ComponentContextImpl<S> key, ServiceReference<T> serviceReference)
    {
        // check whether we already have the service and return that one
        RefPair<S, T> refPair = getRefPair(serviceReference);
        return getService(key, refPair);
    }

    private T getService(ComponentContextImpl<S> key, RefPair<S, T> refPair)
    {
        if (refPair == null)
        {
            //we don't know about this reference
            return null;
        }
        T serviceObject;
        if ((serviceObject = refPair.getServiceObject(key)) != null)
        {
            return serviceObject;
        }
        // otherwise acquire the service
        final BundleContext bundleContext = m_componentManager.getBundleContext();
        if (bundleContext == null)
        {
            m_componentManager.log(LogService.LOG_ERROR,
                "Bundle shut down while getting service {0} ({1}/{2,number,#})", new Object[] { getName(),
                        m_dependencyMetadata.getInterface(), refPair.getRef().getProperty(Constants.SERVICE_ID) },
                null);
            return null;
        }
        try
        {
            refPair.getServiceObject(key, bundleContext, m_componentManager);
            serviceObject = refPair.getServiceObject(key);
        }
        catch (Exception e)
        {
            // caused by getService() called on invalid bundle context
            // or if there is a service reference cycle involving service
            // factories !
            m_componentManager.log(LogService.LOG_ERROR, "Failed getting service {0} ({1}/{2,number,#})",
                new Object[] { getName(), m_dependencyMetadata.getInterface(),
                        refPair.getRef().getProperty(Constants.SERVICE_ID) },
                e);
            return null;
        }

        // return the acquired service (may be null of course)
        //even if we did not set the service object, all the getService are for the same bundle so will have the same object.
        return serviceObject;
    }

    //---------- DependencyManager core ---------------------------------------

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.manager.ReferenceManager#getName()
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
        if (System.getSecurityManager() != null)
        {
            Permission perm = new ServicePermission(getServiceName(), ServicePermission.GET);
            return m_componentManager.getBundle().hasPermission(perm);
        }

        // no security manager, hence permission given
        return true;
    }

    boolean prebind(ComponentContextImpl<S> key)
    {
        return m_customizer.prebind(key);
    }

    /**
     * initializes a dependency. This method binds all of the service
     * occurrences to the instance object
     * @param edgeInfo Edge info for the combination of this component instance and this dependency manager.
     *
     * @return true if the dependency is satisfied and at least the minimum
     *      number of services could be bound. Otherwise false is returned.
     */
    boolean open(ComponentContextImpl<S> componentContext, EdgeInfo edgeInfo)
    {
        if (!invokeInitMethod(componentContext))
        {
            m_componentManager.log(LogService.LOG_DEBUG, "For dependency {0}, failed to initialize object",
                new Object[] { getName() }, null);
            return false;
        }

        int serviceCount = 0;
        AtomicInteger trackingCount = new AtomicInteger();
        Collection<RefPair<S, T>> refs;
        CountDownLatch openLatch;
        synchronized (m_tracker.tracked())
        {
            refs = m_customizer.getRefs(trackingCount);
            edgeInfo.setOpen(trackingCount.get());
            openLatch = edgeInfo.getOpenLatch();
        }
        m_componentManager.log(LogService.LOG_DEBUG, "For dependency {0}, optional: {1}; to bind: {2}",
            new Object[] { getName(), isOptional(), refs }, null);
        for (RefPair<S, T> refPair : refs)
        {
            if (!refPair.isDeleted() && !refPair.isFailed())
            {
                if (!doInvokeBindMethod(componentContext, refPair, trackingCount.get()))
                {
                    m_componentManager.log(LogService.LOG_DEBUG,
                        "For dependency {0}, failed to invoke bind method on object {1}",
                        new Object[] { getName(), refPair }, null);

                }
                serviceCount++;
            }
        }
        openLatch.countDown();
        return cardinalitySatisfied(serviceCount);
    }

    /**
     * Revoke the given bindings. This method cannot throw an exception since
     * it must try to complete all that it can
     * @param componentContext instance we are unbinding from.
     * @param edgeInfo EdgeInfo for the combination of this component instance and this dependency manager.
     */
    void close(ComponentContextImpl<S> componentContext, EdgeInfo edgeInfo)
    {
        // only invoke the unbind method if there is an instance (might be null
        // in the delayed component situation) and the unbind method is declared.
        boolean doUnbind = componentContext != null && m_dependencyMetadata.getUnbind() != null;

        AtomicInteger trackingCount = new AtomicInteger();
        Collection<RefPair<S, T>> refPairs;
        CountDownLatch latch;
        synchronized (m_tracker.tracked())
        {
            refPairs = m_customizer.getRefs(trackingCount);
            edgeInfo.setClose(trackingCount.get());
            latch = edgeInfo.getCloseLatch();
        }

        m_componentManager.log(LogService.LOG_DEBUG,
            "DependencyManager: {0} close component unbinding from {1} at tracking count {2} refpairs: {3}",
            new Object[] { getName(), componentContext, trackingCount.get(), refPairs }, null);
        m_componentManager.waitForTracked(trackingCount.get());
        for (RefPair<S, T> boundRef : refPairs)
        {
            if (doUnbind && !boundRef.isFailed())
            {
                invokeUnbindMethod(componentContext, boundRef, trackingCount.get(), edgeInfo);
            }

        }
        latch.countDown();
    }

    public void invokeBindMethodLate(final ServiceReference<T> ref, int trackingCount)
    {
        if (!isSatisfied())
        {
            return;
        }
        if (!isMultiple())
        {
            Collection<RefPair<S, T>> refs = m_customizer.getRefs(new AtomicInteger());
            if (refs.isEmpty())
            {
                return;
            }
            RefPair<S, T> test = refs.iterator().next();
            if (ref != test.getRef())
            {
                //another ref is now better
                return;
            }
        }
        //TODO dynamic reluctant
        RefPair<S, T> refPair = m_tracker.getService(ref);
        if (refPair == null)
        {
            return; // The service is no longer available, probably because the tracker has been closed
        }
        //TODO this check is no longer correct, fix it!
        //        if (refPair.getServiceObject(key) != null)
        //        {
        //            m_componentManager.log( LogService.LOG_DEBUG,
        //                    "DependencyManager : late binding of service reference {1} skipped as service has already been located",
        //                    new Object[] {ref}, null );
        //            //something else got the reference and may be binding it.
        //            return;
        //        }
        m_componentManager.invokeBindMethod(this, refPair, trackingCount);
    }

    /**
     * Calls the optional init reference method.
     */
    boolean invokeInitMethod(final ComponentContextImpl<S> componentContext)
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if (m_bindMethods.getInit() != null)
        {
            final Object componentInstance = componentContext.getImplementationObject(false);
            if (componentInstance != null)
            {
                return m_bindMethods.getInit().init(componentInstance, m_componentManager);
            }
        }
        return true;
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
     * @param componentContext instance we are binding to
     * @param refPair the service reference, service object tuple.
     * @param trackingCount service event counter for this service.
     * @param edgeInfo EdgeInfo for the combination of this instance and this dependency manager.
     * @return true if the service should be considered bound. If no bind
     *      method is found or the method call fails, <code>true</code> is
     *      returned. <code>false</code> is only returned if the service must
     *      be handed over to the bind method but the service cannot be
     *      retrieved using the service reference.
     */
    boolean invokeBindMethod(ComponentContextImpl<S> componentContext, RefPair<S, T> refPair, int trackingCount,
        EdgeInfo info)
    {
        // The bind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if (componentContext.getImplementationObject(false) != null)
        {
            synchronized (m_tracker.tracked())
            {
                if (info.outOfRange(trackingCount))
                {
                    //ignore events before open started or we will have duplicate binds.
                    return true;
                }
            }
            //edgeInfo open has been set, so binding has started.
            return doInvokeBindMethod(componentContext, refPair, trackingCount);

        }
        else
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "DependencyManager : component not yet created, assuming bind method call succeeded", null);

            return true;
        }
    }

    private boolean doInvokeBindMethod(ComponentContextImpl<S> componentContext, RefPair<S, T> refPair,
        int trackingCount)
    {
        if (!getServiceObject(componentContext, m_bindMethods.getBind(), refPair))
        {
            m_componentManager.log(LogService.LOG_WARNING,
                "DependencyManager : invokeBindMethod : Service not available from service registry for ServiceReference {0} for reference {1}",
                new Object[] { refPair.getRef(), getName() }, null);
            return false;

        }
        MethodResult result = m_bindMethods.getBind().invoke(componentContext.getImplementationObject(false),
            componentContext, refPair, MethodResult.VOID, m_componentManager);
        if (result == null)
        {
            return false;
        }
        m_componentManager.setServiceProperties(result, trackingCount);
        return true;
    }

    /**
     * Calls the updated method.
     *
     * @param componentContext instance we are calling updated on.
     * @param refPair A service reference corresponding to the service whose service
     * @param edgeInfo EdgeInfo for the combination of this instance and this dependency manager.
     * @return {@code true} if reactivation is required.
     */
    boolean invokeUpdatedMethod(ComponentContextImpl<S> componentContext, final RefPair<S, T> refPair,
        int trackingCount, EdgeInfo info)
    {
        if (m_dependencyMetadata.getUpdated() == null && m_dependencyMetadata.getField() == null)
        {
            return false;
        }
        // The updated method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if (componentContext != null)
        {
            synchronized (m_tracker.tracked())
            {
                if (info.outOfRange(trackingCount))
                {
                    //ignore events after close started or we will have duplicate unbinds.
                    return false;
                }
            }
            info.waitForOpen(m_componentManager, getName(), "invokeUpdatedMethod");
            if (!getServiceObject(componentContext, m_bindMethods.getUpdated(), refPair))
            {
                m_componentManager.log(LogService.LOG_WARNING,
                    "DependencyManager : invokeUpdatedMethod : Service not available from service registry for ServiceReference {0} for reference {1}",
                    new Object[] { refPair.getRef(), getName() }, null);
                return false;

            }
            final MethodResult methodResult = m_bindMethods.getUpdated().invoke(
                componentContext.getImplementationObject(false), componentContext, refPair, MethodResult.VOID,
                m_componentManager);
            if (methodResult != null)
            {
                m_componentManager.setServiceProperties(methodResult, trackingCount);
            }
            return methodResult == MethodResult.REACTIVATE;
        }
        else
        {
            // don't care whether we can or cannot call the updated method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log(LogService.LOG_DEBUG,
                "DependencyManager : Component not set, no need to call updated method", null);
        }
        return false;
    }

    /**
     * Calls the unbind method.
     * <p>
     * If the reference is singular and the given service is not the one bound
     * to the component this method has no effect and just returns
     * <code>true</code>.
     *
     * @param componentContext instance we are unbinding from
     * @param refPair A service reference, service pair that will be unbound
     * @param trackingCount service event count for this reference
     * @param info EdgeInfo for the combination of this instance and this dependency manager
     */
    void invokeUnbindMethod(ComponentContextImpl<S> componentContext, final RefPair<S, T> refPair, int trackingCount,
        EdgeInfo info)
    {
        // The unbind method is only invoked if the implementation object is not
        // null. This is valid for both immediate and delayed components
        if (componentContext != null)
        {
            synchronized (m_tracker.tracked())
            {
                if (info.beforeRange(trackingCount))
                {
                    //never bound
                    return;
                }
            }
            info.waitForOpen(m_componentManager, getName(), "invokeUnbindMethod");
            boolean outOfRange;
            synchronized (m_tracker.tracked())
            {
                outOfRange = info.afterRange(trackingCount);
            }
            if (outOfRange)
            {
                //wait for unbinds to complete
                info.waitForClose(m_componentManager, getName(), "invokeUnbindMethod");
                //ignore events after close started or we will have duplicate unbinds.
                return;
            }

            if (!getServiceObject(componentContext, m_bindMethods.getUnbind(), refPair))
            {
                m_componentManager.log(LogService.LOG_WARNING,
                    "DependencyManager : invokeUnbindMethod : Service not available from service registry for ServiceReference {0} for reference {1}",
                    new Object[] { refPair.getRef(), getName() }, null);
                return;

            }
            MethodResult methodResult = m_bindMethods.getUnbind().invoke(
                componentContext.getImplementationObject(false), componentContext, refPair, MethodResult.VOID,
                m_componentManager);
            if (methodResult != null)
            {
                m_componentManager.setServiceProperties(methodResult, trackingCount);
            }
            componentContext.getComponentServiceObjectsHelper().closeServiceObjects(refPair.getRef());
        }
        else
        {
            // don't care whether we can or cannot call the unbind method
            // if the component instance has already been cleared by the
            // close() method
            m_componentManager.log(LogService.LOG_DEBUG,
                "DependencyManager : Component not set, no need to call unbind method", null);
        }
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
    boolean canUpdateDynamically(Map<String, Object> properties)
    {
        // 1. no target filter change
        final String newTarget = (String) properties.get(m_dependencyMetadata.getTargetPropertyName());
        final String currentTarget = getTarget();
        int newMinimumCardinality = getMinimumCardinality(properties);
        if (m_minCardinality == newMinimumCardinality && ((currentTarget == null && newTarget == null)
            || (currentTarget != null && currentTarget.equals(newTarget))))
        {
            // can update if target filter is not changed, since there is
            // no change is service binding
            return true;
        }
        // invariant: target filter change

        // 2. if static policy, cannot update dynamically
        // (for simplicity assuming change in target service binding)
        if (m_dependencyMetadata.isStatic())
        {
            // cannot update if services are statically bound and the target
            // filter is modified, since there is (potentially at least)
            // a change is service bindings
            return false;
        }
        // invariant: target filter change + dynamic policy

        // 3. check optionality
        if (newMinimumCardinality == 0)
        {
            // can update since even if no service matches the new filter, this
            // makes no difference because the dependency is optional
            return true;
        }
        // invariant: target filter change + mandatory + dynamic policy

        // 4. check target services matching the new filter
        ServiceReference<T>[] refs = getFrameworkServiceReferences(newTarget);
        if (refs != null)
        {
            // Return whether there are enough target services
            return newMinimumCardinality <= refs.length;
        }
        // invariant: target filter change + dynamic policy + no more matching service + required

        // 5. There are no services, and some are required.
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
    void setTargetFilter(Map<String, Object> properties)
    {
        Integer minimumCardinality = getMinimumCardinality(properties);
        setTargetFilter((String) properties.get(m_dependencyMetadata.getTargetPropertyName()), minimumCardinality);
    }

    private int getMinimumCardinality(Map<String, Object> properties)
    {
        Integer minimumCardinality = null;
        try
        {
            minimumCardinality = Coercions.coerceToInteger(
                properties.get(m_dependencyMetadata.getMinCardinalityName()));
        }
        catch (ComponentException e)
        {
            m_componentManager.log(LogService.LOG_WARNING,
                "Invalid minimum cardinality property for dependency {0}: {1}",
                new Object[] { getName(), e.getMessage() }, null);
        }
        if (minimumCardinality != null && (minimumCardinality < defaultMinimumCardinality(m_dependencyMetadata)
            || (!m_dependencyMetadata.isMultiple() && minimumCardinality > 1)))
        {
            minimumCardinality = null;
        }
        if (minimumCardinality == null)
        {
            minimumCardinality = defaultMinimumCardinality(m_dependencyMetadata);
        }
        return minimumCardinality;
    }

    private static final String OBJECTCLASS_CLAUSE = "(" + Constants.OBJECTCLASS + "=";
    private static final String PROTOTYPE_SCOPE_CLAUSE = "(" + Constants.SERVICE_SCOPE + "=" + Constants.SCOPE_PROTOTYPE
        + ")";

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
    private void setTargetFilter(String target, int minimumCardinality)
    {
        // if configuration does not set filter, use the value from metadata
        if (target == null)
        {
            target = m_dependencyMetadata.getTarget();
        }
        // do nothing if target filter does not change
        if ((m_target == null && target == null) || (m_target != null && m_target.equals(target)))
        {
            m_componentManager.log(LogService.LOG_DEBUG,
                "No change in target property for dependency {0}: currently registered: {1}",
                new Object[] { getName(), m_tracker != null }, null);
            if (m_tracker != null)
            {
                m_minCardinality = minimumCardinality;
                return;
            }
        }
        m_target = target;
        final boolean multipleExpr = m_target != null
            || m_dependencyMetadata.getScope() == ReferenceScope.prototype_required;
        final boolean allExpr = m_target != null
            && m_dependencyMetadata.getScope() == ReferenceScope.prototype_required;
        final StringBuilder initialReferenceFilterSB = new StringBuilder();
        if (multipleExpr)
        {
            initialReferenceFilterSB.append("(&");
        }
        // "(" + Constants.OBJECTCLASS + "=" + m_dependencyMetadata.getInterface() + ")"
        String classFilterString = OBJECTCLASS_CLAUSE + m_dependencyMetadata.getInterface() + ")";
        initialReferenceFilterSB.append(classFilterString);

        // if reference scope is prototype_required, we simply add
        // (service.scope=prototype) to the filter
        if (m_dependencyMetadata.getScope() == ReferenceScope.prototype_required)
        {
            initialReferenceFilterSB.append(PROTOTYPE_SCOPE_CLAUSE);
        }

        // append target
        if (m_target != null)
        {
            initialReferenceFilterSB.append(m_target);
        }
        if (multipleExpr)
        {
            initialReferenceFilterSB.append(')');
        }
        String initialReferenceFilterString = initialReferenceFilterSB.toString();
        String eventFilterString;
        if (allExpr)
        {
            StringBuilder sb = new StringBuilder("(&").append(PROTOTYPE_SCOPE_CLAUSE).append(m_target).append(")");
            eventFilterString = sb.toString();
        }
        else
        {
            eventFilterString = m_target;
        }

        final ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> oldTracker = m_tracker;
        AtomicInteger trackingCount = new AtomicInteger();
        SortedMap<ServiceReference<T>, RefPair<S, T>> refMap = unregisterServiceListener(trackingCount);
        if (trackingCount.get() != -1)
        {
            //wait for service events to complete before processing initial set from new tracker.
            m_componentManager.waitForTracked(trackingCount.get());
        }
        m_componentManager.log(LogService.LOG_DEBUG, "Setting target property for dependency {0} to {1}",
            new Object[] { getName(), target }, null);
        BundleContext bundleContext = m_componentManager.getBundleContext();
        Filter eventFilter = null;
        if (bundleContext != null)
        {
            if (eventFilterString != null)
            {
                try
                {
                    eventFilter = bundleContext.createFilter(eventFilterString);
                }
                catch (InvalidSyntaxException ise)
                {
                    m_componentManager.log(LogService.LOG_ERROR,
                        "Invalid syntax in target property for dependency {0} to {1}",
                        new Object[] { getName(), target }, null);

                    //create a filter that will never be satisfied
                    eventFilterString = "(component.id=-1)";
                    try
                    {
                        eventFilter = bundleContext.createFilter(eventFilterString);
                    }
                    catch (InvalidSyntaxException e)
                    {
                        //this should not happen
                        return;
                    }

                }
            }
        }
        else
        {
            m_componentManager.log(LogService.LOG_ERROR, "Bundle is shut down for dependency {0} to {1}",
                new Object[] { getName(), target }, null);
            return;
        }

        m_customizer.setPreviousRefMap(refMap);
        boolean initialActive = oldTracker != null && oldTracker.isActive();
        m_componentManager.log(LogService.LOG_DEBUG,
            "New service tracker for {0}, initial active: {1}, previous references: {2}, classFilter: {3}, eventFilter {4}, initialReferenceFilter {5}",
            new Object[] { getName(), initialActive, refMap, classFilterString, eventFilter,
                    initialReferenceFilterString },
            null);
        ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = new ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent>(
            bundleContext, m_customizer, initialActive, m_componentManager.getActivator(), eventFilter,
            classFilterString, initialReferenceFilterString);
        m_customizer.setTracker(tracker);
        //set minimum cardinality
        m_minCardinality = minimumCardinality;

        tracker.open(m_componentManager.getTrackingCount());
        m_customizer.setTrackerOpened();
        if (oldTracker != null)
        {
            oldTracker.completeClose(refMap);
        }
        m_componentManager.log(LogService.LOG_DEBUG, "registering service listener for dependency {0}",
            new Object[] { getName() }, null);
    }

    private Customizer<S, T> newCustomizer()
    {
        Customizer<S, T> customizer;
        if (!hasGetPermission())
        {
            customizer = new NoPermissionsCustomizer();
            m_componentManager.log(LogService.LOG_INFO, "No permission to get services for {0}",
                new Object[] { getName() }, null);
        }
        else if (m_componentManager.isFactory())
        {
            customizer = new FactoryCustomizer();
        }
        else if (isMultiple())
        {
            if (isStatic())
            {
                if (isReluctant())
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
            if (isStatic())
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

    SortedMap<ServiceReference<T>, RefPair<S, T>> unregisterServiceListener(AtomicInteger trackingCount)
    {
        SortedMap<ServiceReference<T>, RefPair<S, T>> refMap;
        ServiceTracker<T, RefPair<S, T>, ExtendedServiceEvent> tracker = m_tracker;
        if (tracker != null)
        {
            refMap = tracker.close(trackingCount);
            m_tracker = null;
            m_componentManager.log(LogService.LOG_DEBUG, "unregistering service listener for dependency {0}",
                new Object[] { getName() }, null);
        }
        else
        {
            refMap = new TreeMap<ServiceReference<T>, RefPair<S, T>>(Collections.reverseOrder());
            m_componentManager.log(LogService.LOG_DEBUG,
                " No existing service listener to unregister for dependency {0}", new Object[] { getName() }, null);
            trackingCount.set(-1);
        }
        //        m_registered = false;
        return refMap;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.manager.ReferenceManager#getTarget()
     */
    public String getTarget()
    {
        return m_target;
    }

    @Override
    public String toString()
    {
        return "DependencyManager: Component [" + m_componentManager + "] reference [" + getName() + "]";
    }

    boolean getServiceObject(ComponentContextImpl<S> key, ReferenceMethod bindMethod, RefPair<S, T> refPair)
    {
        BundleContext bundleContext = m_componentManager.getBundleContext();
        if (bundleContext != null)
        {
            return bindMethod.getServiceObject(key, refPair, bundleContext, m_componentManager);
        }
        else
        {
            refPair.setFailed();
            return false;
        }
    }

    RefPair<S, T> newRefPair(ServiceReference<T> serviceReference)
    {
        if (m_dependencyMetadata.getScope() == ReferenceScope.bundle)
        {
            return new SingleRefPair<S, T>(serviceReference);
        }
        if (m_componentManager.getComponentMetadata().getServiceScope() == Scope.singleton)
        {
            return new SinglePrototypeRefPair<S, T>(m_componentManager.getBundleContext(), serviceReference);
        }
        return new MultiplePrototypeRefPair<S, T>(m_componentManager.getBundleContext(), serviceReference);
    }

    private void deactivateComponentManager()
    {
        m_componentManager.deactivateInternal(ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false);
    }

}
