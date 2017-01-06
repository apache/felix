/*
 * Copyright (c) OSGi Alliance (2000, 2012). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.felix.scr.impl.manager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * The {@code ServiceTracker} class simplifies using services from the
 * Framework's service registry.
 * <p>
 * A {@code ServiceTracker} object is constructed with search criteria and a
 * {@code ServiceTrackerCustomizer} object. A {@code ServiceTracker} can use a
 * {@code ServiceTrackerCustomizer} to customize the service objects to be
 * tracked. The {@code ServiceTracker} can then be opened to begin tracking all
 * services in the Framework's service registry that match the specified search
 * criteria. The {@code ServiceTracker} correctly handles all of the details of
 * listening to {@code ServiceEvent}s and getting and ungetting services.
 * <p>
 * The {@code getServiceReferences} method can be called to get references to
 * the services being tracked. The {@code getService} and {@code getServices}
 * methods can be called to get the service objects for the tracked service.
 * <p>
 * The {@code ServiceTracker} class is thread-safe. It does not call a
 * {@code ServiceTrackerCustomizer} while holding any locks.
 * {@code ServiceTrackerCustomizer} implementations must also be thread-safe.
 * 
 * @param <S> The type of the service being tracked.
 * @param <T> The type of the tracked object.
 * @ThreadSafe
 * @version $Id: 21926ad8717a91633face6bbf570febfcd23b1c7 $
 */

/**
 * changes from osgi service tracker:
 *
 * - included AbstractTracked as an inner class.
 * - addedService method on customizer called after the object is tracked.
 * - we always track all matching services.
 *
 * @param <S>
 * @param <T>
 */
public class ServiceTracker<S, T, U extends ServiceEvent> {
	/* set this to true to compile in debug messages */
	static final boolean					DEBUG	= false;
	/**
	 * The Bundle Context used by this {@code ServiceTracker}.
	 */
	protected final BundleContext			context;
	/**
	 * The Filter used by this {@code ServiceTracker} which specifies the search
	 * criteria for the services to track.
	 * 
	 * @since 1.1
	 */
	protected final Filter					eventFilter;
    /**
     * The {@code ServiceTrackerCustomizer} for this tracker.
     */
    final ServiceTrackerCustomizer<S, T, U> customizer;
	/**
	 * Filter string for use when adding the ServiceListener. If this field is
	 * set, then certain optimizations can be taken since we don't have a user
	 * supplied filter.
	 */
	final String							initialReferenceFilterString;

	/**
	 * Tracked services: {@code ServiceReference} -> customized Object and
	 * {@code ServiceListener} object
	 */
	private volatile Tracked				tracked;


    /**
     * whether the DependencyManager is getting the service immediately.
     */
    private boolean active;

	/**
	 * Accessor method for the current Tracked object. This method is only
	 * intended to be used by the unsynchronized methods which do not modify the
	 * tracked field.
	 * 
	 * @return The current Tracked object.
	 */
	public Tracked tracked() {
		return tracked;
	}

    private ExtendedServiceListenerContext<U> extendedServiceListenerContext;
    private String classFilterString;


	/**
	 * Create a {@code ServiceTracker} on the specified {@code Filter} object.
	 * 
	 * <p>
	 * Services which match the specified {@code Filter} object will be tracked
	 * by this {@code ServiceTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this {@code ServiceTracker}. If customizer
	 *        is null, then this {@code ServiceTracker} will be used as the
	 *        {@code ServiceTrackerCustomizer} and this {@code ServiceTracker}
	 *        will call the {@code ServiceTrackerCustomizer} methods on itself.
	 * @param initialActive Initial active state of the tracker.
	 * @param bundleComponentActivator TODO
	 * @param eventFilter The {@code Filter} to select the services to be tracked.
	 * @param classFilterString TODO
	 * @param initialReferenceFilterString TODO
	 * @since 1.1
	 */
	public ServiceTracker(final BundleContext context,
	    final ServiceTrackerCustomizer<S, T, U> customizer,
	    boolean initialActive,
	    ExtendedServiceListenerContext<U> bundleComponentActivator,
	    final Filter eventFilter,
	    final String classFilterString,
	    final String initialReferenceFilterString) {
        if ((context == null)) {
            /*
             * we throw a NPE here to be consistent with the other constructors
             */
            throw new NullPointerException( "BundleContext");
        }
		this.context = context;
		this.initialReferenceFilterString = initialReferenceFilterString;
		this.eventFilter = eventFilter;
		this.customizer = customizer;
		this.active = initialActive;
		this.extendedServiceListenerContext = bundleComponentActivator;
		this.classFilterString = classFilterString;
	}

	/**
	 * Open this {@code ServiceTracker} and begin tracking services.
	 * 
	 * <p>
	 * This implementation calls {@code open(false)}.
	 * 
	 * @throws java.lang.IllegalStateException If the {@code BundleContext} with
	 *         which this {@code ServiceTracker} was created is no longer valid.
	 * @see #open(boolean, java.util.concurrent.atomic.AtomicInteger)
     * @param trackingCount
	 */
	public void open( AtomicInteger trackingCount ) {
		open(false, trackingCount );
	}

	/**
	 * Open this {@code ServiceTracker} and begin tracking services.
	 * 
	 * <p>
	 * Services which match the search criteria specified when this
	 * {@code ServiceTracker} was created are now tracked by this
	 * {@code ServiceTracker}.
	 * 
	 *
     * @param trackAllServices If {@code true}, then this {@code ServiceTracker}
     *        will track all matching services regardless of class loader
     *        accessibility. If {@code false}, then this {@code ServiceTracker}
     *        will only track matching services which are class loader
     *        accessible to the bundle whose {@code BundleContext} is used by
     *        this {@code ServiceTracker}.
     * @param trackingCount
     * @throws java.lang.IllegalStateException If the {@code BundleContext} with
	 *         which this {@code ServiceTracker} was created is no longer valid.
	 * @since 1.3
	 */
	public void open( boolean trackAllServices, AtomicInteger trackingCount ) {
		final Tracked t;
		synchronized (this) {
			if (tracked != null) {
				return;
			}
			if (DEBUG) {
				System.out.println("ServiceTracker.open: " + eventFilter);
			}
			t = new Tracked( trackingCount );
			synchronized (t) {
				try {
					extendedServiceListenerContext.addServiceListener(classFilterString, eventFilter, t);
					ServiceReference<S>[] references = getInitialReferences(null, initialReferenceFilterString);
					/* set tracked with the initial references */
					t.setInitial(references);
				} catch (InvalidSyntaxException e) {
					throw new RuntimeException("unexpected InvalidSyntaxException: " + e.getMessage(), e);
				}
			}
			tracked = t;
		}
		/* Call tracked outside of synchronized region */
		t.trackInitial(); /* process the initial references */
	}

	/**
	 * Returns the list of initial {@code ServiceReference}s that will be
	 * tracked by this {@code ServiceTracker}.
	 * @param className The class name with which the service was registered, or
	 *        {@code null} for all services.
	 * @param filterString The filter criteria or {@code null} for all services.
	 * 
	 * @return The list of initial {@code ServiceReference}s.
	 * @throws InvalidSyntaxException If the specified filterString has an
	 *         invalid syntax.
	 */
	private ServiceReference<S>[] getInitialReferences(String className, String filterString) throws InvalidSyntaxException {
		ServiceReference<S>[] result = (ServiceReference<S>[]) context.getServiceReferences(className, filterString);
		return result;
	}

	/**
	 * Close this {@code ServiceTracker}.
	 * 
	 * <p>
	 * This method should be called when this {@code ServiceTracker} should end
	 * the tracking of services.
	 * 
	 * <p>
	 * This implementation calls {@link #getServiceReferences()} to get the list
	 * of tracked services to remove.
     * @param trackingCount
     */
	public SortedMap<ServiceReference<S>, T> close( AtomicInteger trackingCount ) {
		final Tracked outgoing;
//		final ServiceReference<S>[] references;
        SortedMap<ServiceReference<S>, T> map = new TreeMap<ServiceReference<S>, T>(Collections.reverseOrder());
		synchronized (this) {
			outgoing = tracked;
			if (outgoing == null) {
				return map;
			}
			if (DEBUG) {
				System.out.println("ServiceTracker.close: " + eventFilter);
			}
			outgoing.close();
            synchronized ( outgoing )
            {
                trackingCount.set( outgoing.getTrackingCount() );
                outgoing.copyEntries( map );
            }
//			references = getServiceReferences();
//			tracked = null;
			try {
				extendedServiceListenerContext.removeServiceListener(classFilterString, eventFilter, outgoing);
			} catch (IllegalStateException e) {
				/* In case the context was stopped. */
			}
		}
		modified(); /* clear the cache */
		synchronized (outgoing) {
			outgoing.notifyAll(); /* wake up any waiters */
		}
//		if (references != null) {
//			for (int i = 0; i < references.length; i++) {
//				outgoing.untrack(references[i], null);
//			}
//		}
		if (DEBUG) {
				System.out.println("ServiceTracker.close[cached cleared]: " + eventFilter);
		}
        return map;
	}

    public void completeClose(Map<ServiceReference<S>, T> toUntrack) {
        final Tracked outgoing;
        synchronized (this) {
            outgoing = tracked;
            if (outgoing == null) {
                return;
            }
            if (DEBUG) {
                System.out.println("ServiceTracker.close: " + eventFilter);
            }
        }
        for (ServiceReference<S> ref: toUntrack.keySet()) {
            outgoing.untrack( ref, null );
        }
        tracked = null;
    }

	/**
	 * Default implementation of the
	 * {@code ServiceTrackerCustomizer.addingService} method.
	 * 
	 * <p>
	 * This method is only called when this {@code ServiceTracker} has been
	 * constructed with a {@code null ServiceTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation returns the result of calling {@code getService} on
	 * the {@code BundleContext} with which this {@code ServiceTracker} was
	 * created passing the specified {@code ServiceReference}.
	 * <p>
	 * This method can be overridden in a subclass to customize the service
	 * object to be tracked for the service being added. In that case, take care
	 * not to rely on the default implementation of
	 * {@link #removedService(ServiceReference, Object, int) removedService} to unget
	 * the service.
	 * 
	 * @param reference The reference to the service being added to this
	 *        {@code ServiceTracker}.
	 * @return The service object to be tracked for the service added to this
	 *         {@code ServiceTracker}.
	 * @see ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference
	 */
	public T addingService(ServiceReference<S> reference, int trackingCount) {
		T result = (T) context.getService(reference);
		return result;
	}

	/**
	 * Default implementation of the
	 * {@code ServiceTrackerCustomizer.modifiedService} method.
	 * 
	 * <p>
	 * This method is only called when this {@code ServiceTracker} has been
	 * constructed with a {@code null ServiceTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation does nothing.
	 * 
	 * @param reference The reference to modified service.
	 * @param service The service object for the modified service.
	 * @see ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, Object, int, ServiceEvent)
	 */
	public void modifiedService(ServiceReference<S> reference, T service, int trackingCount) {
		/* do nothing */
	}

	/**
	 * Default implementation of the
	 * {@code ServiceTrackerCustomizer.removedService} method.
	 * 
	 * <p>
	 * This method is only called when this {@code ServiceTracker} has been
	 * constructed with a {@code null ServiceTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation calls {@code ungetService}, on the
	 * {@code BundleContext} with which this {@code ServiceTracker} was created,
	 * passing the specified {@code ServiceReference}.
	 * <p>
	 * This method can be overridden in a subclass. If the default
	 * implementation of {@link #addingService(ServiceReference, int) addingService}
	 * method was used, this method must unget the service.
	 * 
	 * @param reference The reference to removed service.
	 * @param service The service object for the removed service.
	 * @see ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, Object, int, ServiceEvent)
	 */
	public void removedService(ServiceReference<S> reference, T service, int trackingCount) {
		context.ungetService(reference);
	}

	/**
	 * Return an array of {@code ServiceReference}s for all services being
	 * tracked by this {@code ServiceTracker}.
	 * 
	 * @return Array of {@code ServiceReference}s or {@code null} if no services
	 *         are being tracked.
	 */
	public ServiceReference<S>[] getServiceReferences() {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return null;
		}
		synchronized (t) {
			int length = t.size();
			if (length == 0) {
				return null;
			}
			ServiceReference<S>[] result = new ServiceReference[length];
			return t.copyKeys(result);
		}
	}


	/**
	 * Returns the service object for the specified {@code ServiceReference} if
	 * the specified referenced service is being tracked by this
	 * {@code ServiceTracker}.
	 * 
	 * @param reference The reference to the desired service.
	 * @return A service object or {@code null} if the service referenced by the
	 *         specified {@code ServiceReference} is not being tracked.
	 */
	public T getService(ServiceReference<S> reference) {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return null;
		}
		synchronized (t) {
			return t.getCustomizedObject(reference);
		}
	}

	/**
	 * Return an array of service objects for all services being tracked by this
	 * {@code ServiceTracker}.
	 * 
	 * <p>
	 * This implementation calls {@link #getServiceReferences()} to get the list
	 * of references for the tracked services and then calls
	 * {@link #getService(ServiceReference)} for each reference to get the
	 * tracked service object.
	 * 
	 * @return An array of service objects or {@code null} if no services are
	 *         being tracked.
	 */
	public Object[] getServices() {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return null;
		}
		synchronized (t) {
			ServiceReference<S>[] references = getServiceReferences();
			int length = (references == null) ? 0 : references.length;
			if (length == 0) {
				return null;
			}
			Object[] objects = new Object[length];
			for (int i = 0; i < length; i++) {
				objects[i] = getService(references[i]);
			}
			return objects;
		}
	}


	/**
	 * Remove a service from this {@code ServiceTracker}.
	 * 
	 * The specified service will be removed from this {@code ServiceTracker}.
	 * If the specified service was being tracked then the
	 * {@code ServiceTrackerCustomizer.removedService} method will be called for
	 * that service.
	 * 
	 * @param reference The reference to the service to be removed.
	 */
	public void remove(ServiceReference<S> reference) {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return;
		}
		t.untrack(reference, null);
	}

	/**
	 * Return the number of services being tracked by this
	 * {@code ServiceTracker}.
	 * 
	 * @return The number of services being tracked.
	 */
	public int size() {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return 0;
		}
		synchronized (t) {
			return t.size();
		}
	}

	/**
	 * Returns the tracking count for this {@code ServiceTracker}.
	 * 
	 * The tracking count is initialized to 0 when this {@code ServiceTracker}
	 * is opened. Every time a service is added, modified or removed from this
	 * {@code ServiceTracker}, the tracking count is incremented.
	 * 
	 * <p>
	 * The tracking count can be used to determine if this
	 * {@code ServiceTracker} has added, modified or removed a service by
	 * comparing a tracking count value previously collected with the current
	 * tracking count value. If the value has not changed, then no service has
	 * been added, modified or removed from this {@code ServiceTracker} since
	 * the previous tracking count was collected.
	 * 
	 * @since 1.2
	 * @return The tracking count for this {@code ServiceTracker} or -1 if this
	 *         {@code ServiceTracker} is not open.
	 */
	public int getTrackingCount() {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return -1;
		}
		synchronized (t) {
			return t.getTrackingCount();
		}
	}

	/**
	 * Called by the Tracked object whenever the set of tracked services is
	 * modified. Clears the cache.
	 */
	/*
	 * This method must not be synchronized since it is called by Tracked while
	 * Tracked is synchronized. We don't want synchronization interactions
	 * between the listener thread and the user thread.
	 */
	void modified() {
		if (DEBUG) {
			System.out.println("ServiceTracker.modified: " + eventFilter);
		}
	}

	/**
	 * Return a {@code SortedMap} of the {@code ServiceReference}s and service
	 * objects for all services being tracked by this {@code ServiceTracker}.
	 * The map is sorted in reverse natural order of {@code ServiceReference}.
	 * That is, the first entry is the service with the highest ranking and the
	 * lowest service id.
	 * 
	 * @return A {@code SortedMap} with the {@code ServiceReference}s and
	 *         service objects for all services being tracked by this
	 *         {@code ServiceTracker}. If no services are being tracked, then
	 *         the returned map is empty.
	 * @since 1.5
     * @param activate
     * @param trackingCount
     */
	public SortedMap<ServiceReference<S>, T> getTracked( Boolean activate, AtomicInteger trackingCount ) {
		SortedMap<ServiceReference<S>, T> map = new TreeMap<ServiceReference<S>, T>(Collections.reverseOrder());
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return map;
		}
		synchronized (t) {
            if ( activate != null )
            {
                active = activate;
            }
            trackingCount.set( t.getTrackingCount() );
            return t.copyEntries(map);
		}
	}

    void deactivate() {
        final Tracked t = tracked();
        if (t == null) { /* if ServiceTracker is not open */
            return;
        }
        synchronized (t) {
            active = false;
        }
    }

	/**
	 * Return if this {@code ServiceTracker} is empty.
	 * 
	 * @return {@code true} if this {@code ServiceTracker} is not tracking any
	 *         services.
	 * @since 1.5
	 */
    public boolean isEmpty() {
        final Tracked t = tracked();
        if (t == null) { /* if ServiceTracker is not open */
            return true;
        }
        synchronized (t) {
            return t.isEmpty();
        }
    }

    public int getServiceCount() {
        final Tracked t = tracked();
        if (t == null) { /* if ServiceTracker is not open */
            return 0;
        }
        synchronized (t) {
            return t.size();
        }
    }

    public boolean isActive() {
        final Tracked t = tracked();
        if (t == null) { /* if ServiceTracker is not open */
            return false;
        }
        synchronized (t) {
            return active;
        }

    }

	/**
	 * Return an array of service objects for all services being tracked by this
	 * {@code ServiceTracker}. The runtime type of the returned array is that of
	 * the specified array.
	 * 
	 * <p>
	 * This implementation calls {@link #getServiceReferences()} to get the list
	 * of references for the tracked services and then calls
	 * {@link #getService(ServiceReference)} for each reference to get the
	 * tracked service object.
	 * 
	 * @param array An array into which the tracked service objects will be
	 *        stored, if the array is large enough.
	 * @return An array of service objects being tracked. If the specified array
	 *         is large enough to hold the result, then the specified array is
	 *         returned. If the specified array is longer then necessary to hold
	 *         the result, the array element after the last service object is
	 *         set to {@code null}. If the specified array is not large enough
	 *         to hold the result, a new array is created and returned.
	 * @since 1.5
	 */
	public T[] getServices(T[] array) {
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			if (array.length > 0) {
				array[0] = null;
			}
			return array;
		}
		synchronized (t) {
			ServiceReference<S>[] references = getServiceReferences();
			int length = (references == null) ? 0 : references.length;
			if (length == 0) {
				if (array.length > 0) {
					array[0] = null;
				}
				return array;
			}
			if (length > array.length) {
				array = (T[]) Array.newInstance(array.getClass().getComponentType(), length);
			}
			for (int i = 0; i < length; i++) {
				array[i] = getService(references[i]);
			}
			if (array.length > length) {
				array[length] = null;
			}
			return array;
		}
	}

    /**
     * Abstract class to track items. If a Tracker is reused (closed then reopened),
     * then a new AbstractTracked object is used. This class acts a map of tracked
     * item -> customized object. Subclasses of this class will act as the listener
     * object for the tracker. This class is used to synchronize access to the
     * tracked items. This is not a public class. It is only for use by the
     * implementation of the Tracker class.
     *
     * @param <S> The tracked item. It is the key.
     * @param <T> The value mapped to the tracked item.
     * @param <R> The reason the tracked item is being tracked or untracked.
     * @ThreadSafe
     * @version $Id: 16340086b98d308c2d12f13bcd87fc6467a5a367 $
     * @since 1.4
     */
    abstract class AbstractTracked<S, T, R> {
        /* set this to true to compile in debug messages */
        static final boolean		DEBUG	= false;

        /**
         * Map of tracked items to customized objects.
         *
         * @GuardedBy this
         */
        private final Map<S, T> tracked;

        /**
         * Modification count. This field is initialized to zero and incremented by
         * modified.
         *
         * @GuardedBy this
         */
        private final AtomicInteger					trackingCount;

        /**
         * List of items in the process of being added. This is used to deal with
         * nesting of events. Since events may be synchronously delivered, events
         * can be nested. For example, when processing the adding of a service and
         * the customizer causes the service to be unregistered, notification to the
         * nested call to untrack that the service was unregistered can be made to
         * the track method.
         *
         * Since the ArrayList implementation is not synchronized, all access to
         * this list must be protected by the same synchronized object for
         * thread-safety.
         *
         * @GuardedBy this
         */
        private final List<S> adding;

        /**
         * true if the tracked object is closed.
         *
         * This field is volatile because it is set by one thread and read by
         * another.
         */
        volatile boolean			closed;

        /**
         * Initial list of items for the tracker. This is used to correctly process
         * the initial items which could be modified before they are tracked. This
         * is necessary since the initial set of tracked items are not "announced"
         * by events and therefore the event which makes the item untracked could be
         * delivered before we track the item.
         *
         * An item must not be in both the initial and adding lists at the same
         * time. An item must be moved from the initial list to the adding list
         * "atomically" before we begin tracking it.
         *
         * Since the LinkedList implementation is not synchronized, all access to
         * this list must be protected by the same synchronized object for
         * thread-safety.
         *
         * @GuardedBy this
         */
        private final LinkedList<S> initial;

        /**
         * AbstractTracked constructor.
         * @param trackingCount
         */
        AbstractTracked( AtomicInteger trackingCount ) {
            tracked = new HashMap<S, T>();
            this.trackingCount = trackingCount;
            adding = new ArrayList<S>(6);
            initial = new LinkedList<S>();
            closed = false;
        }

        /**
         * Set initial list of items into tracker before events begin to be
         * received.
         *
         * This method must be called from Tracker's open method while synchronized
         * on this object in the same synchronized block as the add listener call.
         *
         * @param list The initial list of items to be tracked. {@code null} entries
         *        in the list are ignored.
         * @GuardedBy this
         */
        void setInitial(S[] list) {
            if (list == null) {
                return;
            }
            for (S item : list) {
                if (item == null) {
                    continue;
                }
                if (DEBUG) {
                    System.out.println("AbstractTracked.setInitial: " + item); //$NON-NLS-1$
                }
                initial.add(item);
            }
        }

        /**
         * Track the initial list of items. This is called after events can begin to
         * be received.
         *
         * This method must be called from Tracker's open method while not
         * synchronized on this object after the add listener call.
         *
         */
        void trackInitial() {
            while (true) {
                S item;
                synchronized (this) {
                    if (closed || (initial.size() == 0)) {
                        /*
                         * if there are no more initial items
                         */
                        return; /* we are done */
                    }
                    /*
                     * move the first item from the initial list to the adding list
                     * within this synchronized block.
                     */
                    item = initial.removeFirst();
                    if (tracked.get(item) != null) {
                        /* if we are already tracking this item */
                        if (DEBUG) {
                            System.out.println("AbstractTracked.trackInitial[already tracked]: " + item); //$NON-NLS-1$
                        }
                        continue; /* skip this item */
                    }
                    if (adding.contains(item)) {
                        /*
                         * if this item is already in the process of being added.
                         */
                        if (DEBUG) {
                            System.out.println("AbstractTracked.trackInitial[already adding]: " + item); //$NON-NLS-1$
                        }
                        continue; /* skip this item */
                    }
                    adding.add(item);
                }
                if (DEBUG) {
                    System.out.println("AbstractTracked.trackInitial: " + item); //$NON-NLS-1$
                }
                /*
                 * Begin tracking it. We call trackAdding
                 * since we have already put the item in the
                 * adding list.
                 */
                trackAdding(item, null); 
            }
        }

        /**
         * Called by the owning Tracker object when it is closed.
         */
        void close() {
            closed = true;
        }

        /**
         * Begin to track an item.
         *
         * @param item Item to be tracked.
         * @param related Action related object.
         */
        void track(final S item, final R related) {
            boolean tracking;
            final T object;
            int trackingCount = -1;
            synchronized (this) {
                if (closed) {
                    return;
                }
                tracking = tracked.containsKey( item );
                object = tracked.get(item);
                if (!tracking) { /* we are not tracking the item */
                    if (adding.contains(item)) {
                        /* if this item is already in the process of being added. */
                        if (DEBUG) {
                            System.out.println("AbstractTracked.track[already adding]: " + item); //$NON-NLS-1$
                        }
                        return;
                    }
                    adding.add(item); /* mark this item is being added */
                } else { /* we are currently tracking this item */
                    if (DEBUG) {
                        System.out.println("AbstractTracked.track[modified]: " + item); //$NON-NLS-1$
                    }
                    trackingCount = modified(); /* increment modification count */
                }
            }

            if (!tracking) { /* we are not tracking the item */
                trackAdding(item, related);
            } else {
                /* Call customizer outside of synchronized region */
                customizerModified(item, related, object, trackingCount );
                /*
                 * If the customizer throws an unchecked exception, it is safe to
                 * let it propagate
                 */
            }
        }

        /**
         * Common logic to add an item to the tracker used by track and
         * trackInitial. The specified item must have been placed in the adding list
         * before calling this method.
         *
         * @param item Item to be tracked.
         * @param related Action related object.
         */
        private void trackAdding(final S item, final R related) {
            if (DEBUG) {
                System.out.println("AbstractTracked.trackAdding: " + item); //$NON-NLS-1$
            }
            T object = null;
            boolean becameUntracked = false;
            int trackingCount = -1;
            int serviceCount = -1;
            /* Call customizer outside of synchronized region */
            try {
                object = customizerAdding(item, related);
                /*
                 * If the customizer throws an unchecked exception, it will
                 * propagate after the finally
                 */
            } finally {
                synchronized (this) {
                    if (adding.remove(item) && !closed) {
                        /*
                         * if the item was not untracked during the customizer
                         * callback
                         */
                        tracked.put( item, object );
                        trackingCount = modified(); /* increment modification count */
                        serviceCount = tracked.size();
                        notifyAll(); /* notify any waiters */
                    } else {
                        becameUntracked = true;
                    }
                }
            }
            /*
             * The item became untracked during the customizer callback.
             */
            if (becameUntracked) {
                if (DEBUG) {
                    System.out.println("AbstractTracked.trackAdding[removed]: " + item); //$NON-NLS-1$
                }
                /* Call customizer outside of synchronized region */
                customizerRemoved(item, related, object, trackingCount );
                /*
                 * If the customizer throws an unchecked exception, it is safe to
                 * let it propagate
                 */
            } else {
                customizerAdded( item, related, object, trackingCount, serviceCount );
            }
        }

        /**
         * Discontinue tracking the item.
         *
         * @param item Item to be untracked.
         * @param related Action related object.
         */
        void untrack(final S item, final R related) {
            final T object;
            int trackingCount;
            synchronized (this) {
                if (initial.remove(item)) { /*
                                             * if this item is already in the list
                                             * of initial references to process
                                             */
                    if (DEBUG) {
                        System.out.println("AbstractTracked.untrack[removed from initial]: " + item); //$NON-NLS-1$
                    }
                    return; /*
                             * we have removed it from the list and it will not be
                             * processed
                             */
                }

                if (adding.remove(item)) { /*
                                             * if the item is in the process of
                                             * being added
                                             */
                    if (DEBUG) {
                        System.out.println("AbstractTracked.untrack[being added]: " + item); //$NON-NLS-1$
                    }
                    return; /*
                             * in case the item is untracked while in the process of
                             * adding
                             */
                }
                object = tracked.remove(item); /*                                                 * must remove from tracker before
                                                 * calling customizer callback
                                                 */
                if (object == null) { /* are we actually tracking the item */
                    return;
                }
                trackingCount = modified(); /* increment modification count */
            }
            if (DEBUG) {
                System.out.println("AbstractTracked.untrack[removed]: " + item); //$NON-NLS-1$
            }
            /* Call customizer outside of synchronized region */
            customizerRemoved(item, related, object, trackingCount );
            /*
             * If the customizer throws an unchecked exception, it is safe to let it
             * propagate
             */
        }

        /**
         * Returns the number of tracked items.
         *
         * @return The number of tracked items.
         *
         * @GuardedBy this
         */
        int size() {
            return tracked.size();
        }

        /**
         * Returns if the tracker is empty.
         *
         * @return Whether the tracker is empty.
         *
         * @GuardedBy this
         * @since 1.5
         */
        boolean isEmpty() {
            return tracked.isEmpty();
        }

        /**
         * Return the customized object for the specified item
         *
         * @param item The item to lookup in the map
         * @return The customized object for the specified item.
         *
         * @GuardedBy this
         */
        T getCustomizedObject(final S item) {
            return tracked.get( item );
        }

        /**
         * Copy the tracked items into an array.
         *
         * @param list An array to contain the tracked items.
         * @return The specified list if it is large enough to hold the tracked
         *         items or a new array large enough to hold the tracked items.
         * @GuardedBy this
         */
        S[] copyKeys(final S[] list) {
            return tracked.keySet().toArray(list);
        }

        /**
         * Increment the modification count. If this method is overridden, the
         * overriding method MUST call this method to increment the tracking count.
         *
         * @GuardedBy this
         */
        int modified() {
            return trackingCount.incrementAndGet();
        }

        /**
         * Returns the tracking count for this {@code ServiceTracker} object.
         *
         * The tracking count is initialized to 0 when this object is opened. Every
         * time an item is added, modified or removed from this object the tracking
         * count is incremented.
         *
         * @GuardedBy this
         * @return The tracking count for this object.
         */
        int getTrackingCount() {
            return trackingCount.get();
        }

        /**
         * Copy the tracked items and associated values into the specified map.
         *
         * @param <M> Type of {@code Map} to hold the tracked items and associated
         *        values.
         * @param map The map into which to copy the tracked items and associated
         *        values. This map must not be a user provided map so that user code
         *        is not executed while synchronized on this.
         * @return The specified map.
         * @GuardedBy this
         * @since 1.5
         */
        <M extends Map<? super S, ? super T>> M copyEntries(final M map) {
            map.putAll(tracked);
            return map;
        }

        /**
         * Call the specific customizer adding method. This method must not be
         * called while synchronized on this object.
         *
         *
         *
         * @param item Item to be tracked.
         * @param related Action related object.
         * @return Customized object for the tracked item or {@code null} if the
         *         item is not to be tracked.
         */
        abstract T customizerAdding( final S item, final R related );

        abstract void customizerAdded( final S item, final R related, final T object, int trackingCount, int serviceCount );

        /**
         * Call the specific customizer modified method. This method must not be
         * called while synchronized on this object.
         *
         * @param item Tracked item.
         * @param related Action related object.
         * @param object Customized object for the tracked item.
         * @param trackingCount
         */
        abstract void customizerModified( final S item, final R related, final T object, int trackingCount );

        /**
         * Call the specific customizer removed method. This method must not be
         * called while synchronized on this object.
         *
         * @param item Tracked item.
         * @param related Action related object.
         * @param object Customized object for the tracked item.
         * @param trackingCount
         */
        abstract void customizerRemoved( final S item, final R related, final T object, int trackingCount );
    }


	/**
	 * Inner class which subclasses AbstractTracked. This class is the
	 * {@code ServiceListener} object for the tracker.
	 * 
	 * @ThreadSafe
	 */
	private class Tracked extends AbstractTracked<ServiceReference<S>, T, U> implements ExtendedServiceListener<U> {
		/**
		 * Tracked constructor.
         * @param trackingCount
         */
		Tracked( AtomicInteger trackingCount ) {
			super( trackingCount );
		}

		/**
		 * {@code ServiceListener} method for the {@code ServiceTracker} class.
		 * This method must NOT be synchronized to avoid deadlock potential.
		 * 
		 * @param event {@code ServiceEvent} object from the framework.
		 */
		final public void serviceChanged(final U event) {
			/*
			 * Check if we had a delayed call (which could happen when we
			 * close).
			 */
			if (closed) {
				return;
			}
			final ServiceReference<S> reference = (ServiceReference<S>) event.getServiceReference();
			if (DEBUG) {
				System.out.println("ServiceTracker.Tracked.serviceChanged[" + event.getType() + "]: " + reference);
			}

			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
				case ServiceEvent.MODIFIED :
					track(reference, event);
					/*
					 * If the customizer throws an unchecked exception, it is
					 * safe to let it propagate
					 */
					break;
				case ServiceEvent.MODIFIED_ENDMATCH :
				case ServiceEvent.UNREGISTERING :
					untrack(reference, event);
					/*
					 * If the customizer throws an unchecked exception, it is
					 * safe to let it propagate
					 */
					break;
				default :
	                System.out.println("Unrecognized event type: ServiceTracker.Tracked.serviceChanged[" + event.getType() + "]: " + reference);
				    
			}
		}

		/**
		 * Increment the tracking count and tell the tracker there was a
		 * modification.
		 * 
		 * @GuardedBy this
		 */
		final int modified() {
			int trackingCount = super.modified(); /* increment the modification count */
			ServiceTracker.this.modified();
            return trackingCount;
		}

		/**
		 * Call the specific customizer adding method. This method must not be
		 * called while synchronized on this object.
		 * 
		 *
         *
         * @param item Item to be tracked.
         * @param related Action related object.
         * @return Customized object for the tracked item or {@code null} if the
		 *         item is not to be tracked.
		 */
		final T customizerAdding( final ServiceReference<S> item, final U related ) {
			return customizer.addingService( item );
		}

		final void customizerAdded( final ServiceReference<S> item, final U related, final T object, int trackingCount, int serviceCount ) {
		    customizer.addedService( item, object, trackingCount, serviceCount, related );
		}

		/**
		 * Call the specific customizer modified method. This method must not be
		 * called while synchronized on this object.
		 *
         * @param item Tracked item.
         * @param related Action related object.
         * @param object Customized object for the tracked item.
         * @param trackingCount
         */
		final void customizerModified( final ServiceReference<S> item, final U related, final T object, int trackingCount ) {
			customizer.modifiedService( item, object, trackingCount, related );
		}

        /**
		 * Call the specific customizer removed method. This method must not be
		 * called while synchronized on this object.
		 *
         * @param item Tracked item.
         * @param related Action related object.
         * @param object Customized object for the tracked item.
         * @param trackingCount
         */
		final void customizerRemoved( final ServiceReference<S> item, final U related, final T object, int trackingCount ) {
			customizer.removedService(item, object, trackingCount, related );
		}
	}

	/**
	 * Subclass of Tracked which implements the AllServiceListener interface.
	 * This class is used by the ServiceTracker if open is called with true.
	 * 
	 * @since 1.3
	 * @ThreadSafe
	 */
//	private class AllTracked extends Tracked<U> implements AllServiceListener {
//		/**
//		 * AllTracked constructor.
//         * @param trackingCount
//         */
//		AllTracked( AtomicInteger trackingCount ) {
//			super( trackingCount );
//		}
//	}
}
