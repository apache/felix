/*
 * Copyright (c) OSGi Alliance (2000, 2017). All Rights Reserved.
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

package org.osgi.util.tracker;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
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
 * @author $Id: 3c9016c43c6289259f97470eff4c9986b6fb887a $
 */
@ConsumerType
public class ServiceTracker<S, T> implements ServiceTrackerCustomizer<S, T> {
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
	protected final Filter					filter;
	/**
	 * The {@code ServiceTrackerCustomizer} for this tracker.
	 */
	final ServiceTrackerCustomizer<S, T>	customizer;
	/**
	 * Filter string for use when adding the ServiceListener. If this field is
	 * set, then certain optimizations can be taken since we don't have a user
	 * supplied filter.
	 */
	final String							listenerFilter;
	/**
	 * Class name to be tracked. If this field is set, then we are tracking by
	 * class name.
	 */
	private final String					trackClass;
	/**
	 * Reference to be tracked. If this field is set, then we are tracking a
	 * single ServiceReference.
	 */
	private final ServiceReference<S>		trackReference;
	/**
	 * Tracked services: {@code ServiceReference} -> customized Object and
	 * {@code ServiceListener} object
	 */
	private volatile Tracked				tracked;

	/**
	 * Accessor method for the current Tracked object. This method is only
	 * intended to be used by the unsynchronized methods which do not modify the
	 * tracked field.
	 * 
	 * @return The current Tracked object.
	 */
	private Tracked tracked() {
		return tracked;
	}

	/**
	 * Cached ServiceReference for getServiceReference.
	 * 
	 * This field is volatile since it is accessed by multiple threads.
	 */
	private volatile ServiceReference<S>	cachedReference;
	/**
	 * Cached service object for getService.
	 * 
	 * This field is volatile since it is accessed by multiple threads.
	 */
	private volatile T						cachedService;

	/**
	 * Create a {@code ServiceTracker} on the specified {@code ServiceReference}
	 * .
	 * 
	 * <p>
	 * The service referenced by the specified {@code ServiceReference} will be
	 * tracked by this {@code ServiceTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param reference The {@code ServiceReference} for the service to be
	 *        tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this {@code ServiceTracker}. If customizer
	 *        is {@code null}, then this {@code ServiceTracker} will be used as
	 *        the {@code ServiceTrackerCustomizer} and this
	 *        {@code ServiceTracker} will call the
	 *        {@code ServiceTrackerCustomizer} methods on itself.
	 */
	public ServiceTracker(final BundleContext context, final ServiceReference<S> reference, final ServiceTrackerCustomizer<S, T> customizer) {
		this.context = context;
		this.trackReference = reference;
		this.trackClass = null;
		this.customizer = (customizer == null) ? this : customizer;
		this.listenerFilter = "(" + Constants.SERVICE_ID + "=" + reference.getProperty(Constants.SERVICE_ID).toString() + ")";
		try {
			this.filter = context.createFilter(listenerFilter);
		} catch (InvalidSyntaxException e) {
			/*
			 * we could only get this exception if the ServiceReference was
			 * invalid
			 */
			IllegalArgumentException iae = new IllegalArgumentException("unexpected InvalidSyntaxException: " + e.getMessage());
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Create a {@code ServiceTracker} on the specified class name.
	 * 
	 * <p>
	 * Services registered under the specified class name will be tracked by
	 * this {@code ServiceTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param clazz The class name of the services to be tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this {@code ServiceTracker}. If customizer
	 *        is {@code null}, then this {@code ServiceTracker} will be used as
	 *        the {@code ServiceTrackerCustomizer} and this
	 *        {@code ServiceTracker} will call the
	 *        {@code ServiceTrackerCustomizer} methods on itself.
	 */
	public ServiceTracker(final BundleContext context, final String clazz, final ServiceTrackerCustomizer<S, T> customizer) {
		this.context = context;
		this.trackReference = null;
		this.trackClass = clazz;
		this.customizer = (customizer == null) ? this : customizer;
		// we call clazz.toString to verify clazz is non-null!
		this.listenerFilter = "(" + Constants.OBJECTCLASS + "=" + clazz.toString() + ")";
		try {
			this.filter = context.createFilter(listenerFilter);
		} catch (InvalidSyntaxException e) {
			/*
			 * we could only get this exception if the clazz argument was
			 * malformed
			 */
			IllegalArgumentException iae = new IllegalArgumentException("unexpected InvalidSyntaxException: " + e.getMessage());
			iae.initCause(e);
			throw iae;
		}
	}

	/**
	 * Create a {@code ServiceTracker} on the specified {@code Filter} object.
	 * 
	 * <p>
	 * Services which match the specified {@code Filter} object will be tracked
	 * by this {@code ServiceTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param filter The {@code Filter} to select the services to be tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this {@code ServiceTracker}. If customizer
	 *        is null, then this {@code ServiceTracker} will be used as the
	 *        {@code ServiceTrackerCustomizer} and this {@code ServiceTracker}
	 *        will call the {@code ServiceTrackerCustomizer} methods on itself.
	 * @since 1.1
	 */
	public ServiceTracker(final BundleContext context, final Filter filter, final ServiceTrackerCustomizer<S, T> customizer) {
		this.context = context;
		this.trackReference = null;
		this.trackClass = null;
		this.listenerFilter = filter.toString();
		this.filter = filter;
		this.customizer = (customizer == null) ? this : customizer;
		if ((context == null) || (filter == null)) {
			/*
			 * we throw a NPE here to be consistent with the other constructors
			 */
			throw new NullPointerException();
		}
	}

	/**
	 * Create a {@code ServiceTracker} on the specified class.
	 * 
	 * <p>
	 * Services registered under the name of the specified class will be tracked
	 * by this {@code ServiceTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param clazz The class of the services to be tracked.
	 * @param customizer The customizer object to call when services are added,
	 *        modified, or removed in this {@code ServiceTracker}. If customizer
	 *        is {@code null}, then this {@code ServiceTracker} will be used as
	 *        the {@code ServiceTrackerCustomizer} and this
	 *        {@code ServiceTracker} will call the
	 *        {@code ServiceTrackerCustomizer} methods on itself.
	 * @since 1.5
	 */
	public ServiceTracker(final BundleContext context, final Class<S> clazz, final ServiceTrackerCustomizer<S, T> customizer) {
		this(context, clazz.getName(), customizer);
	}

	/**
	 * Open this {@code ServiceTracker} and begin tracking services.
	 * 
	 * <p>
	 * This implementation calls {@code open(false)}.
	 * 
	 * @throws java.lang.IllegalStateException If the {@code BundleContext} with
	 *         which this {@code ServiceTracker} was created is no longer valid.
	 * @see #open(boolean)
	 */
	public void open() {
		open(false);
	}

	/**
	 * Open this {@code ServiceTracker} and begin tracking services.
	 * 
	 * <p>
	 * Services which match the search criteria specified when this
	 * {@code ServiceTracker} was created are now tracked by this
	 * {@code ServiceTracker}.
	 * 
	 * @param trackAllServices If {@code true}, then this {@code ServiceTracker}
	 *        will track all matching services regardless of class loader
	 *        accessibility. If {@code false}, then this {@code ServiceTracker}
	 *        will only track matching services which are class loader
	 *        accessible to the bundle whose {@code BundleContext} is used by
	 *        this {@code ServiceTracker}.
	 * @throws java.lang.IllegalStateException If the {@code BundleContext} with
	 *         which this {@code ServiceTracker} was created is no longer valid.
	 * @since 1.3
	 */
	public void open(boolean trackAllServices) {
		final Tracked t;
		synchronized (this) {
			if (tracked != null) {
				return;
			}
			if (DEBUG) {
				System.out.println("ServiceTracker.open: " + filter);
			}
			t = trackAllServices ? new AllTracked() : new Tracked();
			synchronized (t) {
				try {
					context.addServiceListener(t, listenerFilter);
					ServiceReference<S>[] references = null;
					if (trackClass != null) {
						references = getInitialReferences(trackAllServices, trackClass, null);
					} else {
						if (trackReference != null) {
							if (trackReference.getBundle() != null) {
								@SuppressWarnings("unchecked")
								ServiceReference<S>[] single = new ServiceReference[] {trackReference};
								references = single;
							}
						} else { /* user supplied filter */
							references = getInitialReferences(trackAllServices, null, listenerFilter);
						}
					}
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
	 * 
	 * @param trackAllServices If {@code true}, use
	 *        {@code getAllServiceReferences}.
	 * @param className The class name with which the service was registered, or
	 *        {@code null} for all services.
	 * @param filterString The filter criteria or {@code null} for all services.
	 * @return The list of initial {@code ServiceReference}s.
	 * @throws InvalidSyntaxException If the specified filterString has an
	 *         invalid syntax.
	 */
	private ServiceReference<S>[] getInitialReferences(boolean trackAllServices, String className, String filterString) throws InvalidSyntaxException {
		@SuppressWarnings("unchecked")
		ServiceReference<S>[] result = (ServiceReference<S>[]) ((trackAllServices) ? context.getAllServiceReferences(className, filterString) : context.getServiceReferences(className, filterString));
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
	 */
	public void close() {
		final Tracked outgoing;
		final ServiceReference<S>[] references;
		synchronized (this) {
			outgoing = tracked;
			if (outgoing == null) {
				return;
			}
			if (DEBUG) {
				System.out.println("ServiceTracker.close: " + filter);
			}
			outgoing.close();
			references = getServiceReferences();
			tracked = null;
			try {
				context.removeServiceListener(outgoing);
			} catch (IllegalStateException e) {
				/* In case the context was stopped. */
			}
		}
		modified(); /* clear the cache */
		synchronized (outgoing) {
			outgoing.notifyAll(); /* wake up any waiters */
		}
		if (references != null) {
			for (int i = 0; i < references.length; i++) {
				outgoing.untrack(references[i], null);
			}
		}
		if (DEBUG) {
			if ((cachedReference == null) && (cachedService == null)) {
				System.out.println("ServiceTracker.close[cached cleared]: " + filter);
			}
		}
	}

	/**
	 * Default implementation of the
	 * {@code ServiceTrackerCustomizer.addingService} method.
	 * <p>
	 * This method is only called when this {@code ServiceTracker} has been
	 * constructed with a {@code null ServiceTrackerCustomizer} argument.
	 * <p>
	 * This implementation returns the result of calling {@code getService}, on
	 * the {@code BundleContext} with which this {@code ServiceTracker} was
	 * created, passing the specified {@code ServiceReference}.
	 * <p>
	 * This method can be overridden in a subclass to customize the service
	 * object to be tracked for the service being added. In that case, take care
	 * not to rely on the default implementation of
	 * {@link #removedService(ServiceReference, Object) removedService} to unget
	 * the service.
	 * 
	 * @param reference The reference to the service being added to this
	 *            {@code ServiceTracker}.
	 * @return The service object to be tracked for the service added to this
	 *         {@code ServiceTracker}.
	 * @see ServiceTrackerCustomizer#addingService(ServiceReference)
	 */
	@Override
	public T addingService(ServiceReference<S> reference) {
		@SuppressWarnings("unchecked")
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
	 * @see ServiceTrackerCustomizer#modifiedService(ServiceReference, Object)
	 */
	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		/* do nothing */
	}

	/**
	 * Default implementation of the
	 * {@code ServiceTrackerCustomizer.removedService} method.
	 * <p>
	 * This method is only called when this {@code ServiceTracker} has been
	 * constructed with a {@code null ServiceTrackerCustomizer} argument.
	 * <p>
	 * This implementation calls {@code ungetService}, on the
	 * {@code BundleContext} with which this {@code ServiceTracker} was created,
	 * passing the specified {@code ServiceReference}.
	 * <p>
	 * This method can be overridden in a subclass. If the default
	 * implementation of {@link #addingService(ServiceReference) addingService}
	 * method was used, this method must unget the service.
	 * 
	 * @param reference The reference to removed service.
	 * @param service The service object for the removed service.
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
	 */
	@Override
	public void removedService(ServiceReference<S> reference, T service) {
		context.ungetService(reference);
	}

	/**
	 * Wait for at least one service to be tracked by this
	 * {@code ServiceTracker}. This method will also return when this
	 * {@code ServiceTracker} is closed.
	 * 
	 * <p>
	 * It is strongly recommended that {@code waitForService} is not used during
	 * the calling of the {@code BundleActivator} methods.
	 * {@code BundleActivator} methods are expected to complete in a short
	 * period of time.
	 * 
	 * <p>
	 * This implementation calls {@link #getService()} to determine if a service
	 * is being tracked.
	 * 
	 * @param timeout The time interval in milliseconds to wait. If zero, the
	 *        method will wait indefinitely.
	 * @return Returns the result of {@link #getService()}.
	 * @throws InterruptedException If another thread has interrupted the
	 *         current thread.
	 * @throws IllegalArgumentException If the value of timeout is negative.
	 */
	public T waitForService(long timeout) throws InterruptedException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}

		T object = getService();
		if (object != null) {
			return object;
		}

		final long endTime = (timeout == 0) ? 0 : (System.currentTimeMillis() + timeout);
		do {
			final Tracked t = tracked();
			if (t == null) { /* if ServiceTracker is not open */
				return null;
			}
			synchronized (t) {
				if (t.size() == 0) {
					t.wait(timeout);
				}
			}
			object = getService();
			if (endTime > 0) { // if we have a timeout
				timeout = endTime - System.currentTimeMillis();
				if (timeout <= 0) { // that has expired
					break;
				}
			}
		} while (object == null);
		return object;
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
			if (t.isEmpty()) {
				return null;
			}
			@SuppressWarnings("unchecked")
			ServiceReference<S>[] result = new ServiceReference[0];
			return t.copyKeys(result);
		}
	}

	/**
	 * Returns a {@code ServiceReference} for one of the services being tracked
	 * by this {@code ServiceTracker}.
	 * 
	 * <p>
	 * If multiple services are being tracked, the service with the highest
	 * ranking (as specified in its {@code service.ranking} property) is
	 * returned. If there is a tie in ranking, the service with the lowest
	 * service id (as specified in its {@code service.id} property); that is,
	 * the service that was registered first is returned. This is the same
	 * algorithm used by {@code BundleContext.getServiceReference}.
	 * 
	 * <p>
	 * This implementation calls {@link #getServiceReferences()} to get the list
	 * of references for the tracked services.
	 * 
	 * @return A {@code ServiceReference} or {@code null} if no services are
	 *         being tracked.
	 * @since 1.1
	 */
	public ServiceReference<S> getServiceReference() {
		ServiceReference<S> reference = cachedReference;
		if (reference != null) {
			if (DEBUG) {
				System.out.println("ServiceTracker.getServiceReference[cached]: " + filter);
			}
			return reference;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.getServiceReference: " + filter);
		}
		ServiceReference<S>[] references = getServiceReferences();
		int length = (references == null) ? 0 : references.length;
		if (length == 0) { /* if no service is being tracked */
			return null;
		}
		int index = 0;
		if (length > 1) { /* if more than one service, select highest ranking */
			int rankings[] = new int[length];
			int count = 0;
			int maxRanking = Integer.MIN_VALUE;
			for (int i = 0; i < length; i++) {
				Object property = references[i].getProperty(Constants.SERVICE_RANKING);
				int ranking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
				rankings[i] = ranking;
				if (ranking > maxRanking) {
					index = i;
					maxRanking = ranking;
					count = 1;
				} else {
					if (ranking == maxRanking) {
						count++;
					}
				}
			}
			if (count > 1) { /* if still more than one service, select lowest id */
				long minId = Long.MAX_VALUE;
				for (int i = 0; i < length; i++) {
					if (rankings[i] == maxRanking) {
						long id = ((Long) (references[i].getProperty(Constants.SERVICE_ID))).longValue();
						if (id < minId) {
							index = i;
							minId = id;
						}
					}
				}
			}
		}
		return cachedReference = references[index];
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
	 * Returns a service object for one of the services being tracked by this
	 * {@code ServiceTracker}.
	 * 
	 * <p>
	 * If any services are being tracked, this implementation returns the result
	 * of calling {@code getService(getServiceReference())}.
	 * 
	 * @return A service object or {@code null} if no services are being
	 *         tracked.
	 */
	public T getService() {
		T service = cachedService;
		if (service != null) {
			if (DEBUG) {
				System.out.println("ServiceTracker.getService[cached]: " + filter);
			}
			return service;
		}
		if (DEBUG) {
			System.out.println("ServiceTracker.getService: " + filter);
		}
		ServiceReference<S> reference = getServiceReference();
		if (reference == null) {
			return null;
		}
		return cachedService = getService(reference);
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
		cachedReference = null; /* clear cached value */
		cachedService = null; /* clear cached value */
		if (DEBUG) {
			System.out.println("ServiceTracker.modified: " + filter);
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
	 */
	public SortedMap<ServiceReference<S>, T> getTracked() {
		SortedMap<ServiceReference<S>, T> map = new TreeMap<ServiceReference<S>, T>(Collections.reverseOrder());
		final Tracked t = tracked();
		if (t == null) { /* if ServiceTracker is not open */
			return map;
		}
		synchronized (t) {
			return t.copyEntries(map);
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
				@SuppressWarnings("unchecked")
				T[] newInstance = (T[]) Array.newInstance(array.getClass().getComponentType(), length);
				array = newInstance;
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
	 * Inner class which subclasses AbstractTracked. This class is the
	 * {@code ServiceListener} object for the tracker.
	 * 
	 * @ThreadSafe
	 */
	private class Tracked extends AbstractTracked<ServiceReference<S>, T, ServiceEvent> implements ServiceListener {
		/**
		 * Tracked constructor.
		 */
		Tracked() {
			super();
		}

		/**
		 * {@code ServiceListener} method for the {@code ServiceTracker} class.
		 * This method must NOT be synchronized to avoid deadlock potential.
		 * 
		 * @param event {@code ServiceEvent} object from the framework.
		 */
		@Override
		final public void serviceChanged(final ServiceEvent event) {
			/*
			 * Check if we had a delayed call (which could happen when we
			 * close).
			 */
			if (closed) {
				return;
			}
			@SuppressWarnings("unchecked")
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
			}
		}

		/**
		 * Increment the tracking count and tell the tracker there was a
		 * modification.
		 * 
		 * @GuardedBy this
		 */
		@Override
		final void modified() {
			super.modified(); /* increment the modification count */
			ServiceTracker.this.modified();
		}

		/**
		 * Call the specific customizer adding method. This method must not be
		 * called while synchronized on this object.
		 * 
		 * @param item Item to be tracked.
		 * @param related Action related object.
		 * @return Customized object for the tracked item or {@code null} if the
		 *         item is not to be tracked.
		 */
		@Override
		final T customizerAdding(final ServiceReference<S> item, final ServiceEvent related) {
			return customizer.addingService(item);
		}

		/**
		 * Call the specific customizer modified method. This method must not be
		 * called while synchronized on this object.
		 * 
		 * @param item Tracked item.
		 * @param related Action related object.
		 * @param object Customized object for the tracked item.
		 */
		@Override
		final void customizerModified(final ServiceReference<S> item, final ServiceEvent related, final T object) {
			customizer.modifiedService(item, object);
		}

		/**
		 * Call the specific customizer removed method. This method must not be
		 * called while synchronized on this object.
		 * 
		 * @param item Tracked item.
		 * @param related Action related object.
		 * @param object Customized object for the tracked item.
		 */
		@Override
		final void customizerRemoved(final ServiceReference<S> item, final ServiceEvent related, final T object) {
			customizer.removedService(item, object);
		}
	}

	/**
	 * Subclass of Tracked which implements the AllServiceListener interface.
	 * This class is used by the ServiceTracker if open is called with true.
	 * 
	 * @since 1.3
	 * @ThreadSafe
	 */
	private class AllTracked extends Tracked implements AllServiceListener {
		/**
		 * AllTracked constructor.
		 */
		AllTracked() {
			super();
		}
	}
}
