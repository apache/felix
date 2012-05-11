/*
 * Copyright (c) OSGi Alliance (2007, 2012). All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * The {@code BundleTracker} class simplifies tracking bundles much like the
 * {@code ServiceTracker} simplifies tracking services.
 * <p>
 * A {@code BundleTracker} is constructed with state criteria and a
 * {@code BundleTrackerCustomizer} object. A {@code BundleTracker} can use the
 * {@code BundleTrackerCustomizer} to select which bundles are tracked and to
 * create a customized object to be tracked with the bundle. The
 * {@code BundleTracker} can then be opened to begin tracking all bundles whose
 * state matches the specified state criteria.
 * <p>
 * The {@code getBundles} method can be called to get the {@code Bundle} objects
 * of the bundles being tracked. The {@code getObject} method can be called to
 * get the customized object for a tracked bundle.
 * <p>
 * The {@code BundleTracker} class is thread-safe. It does not call a
 * {@code BundleTrackerCustomizer} while holding any locks.
 * {@code BundleTrackerCustomizer} implementations must also be thread-safe.
 * 
 * @param <T> The type of the tracked object.
 * @ThreadSafe
 * @version $Id: f21db4fe54284d4810bd9b5fa2528957804e3a21 $
 * @since 1.4
 */
public class BundleTracker<T> implements BundleTrackerCustomizer<T> {
	/* set this to true to compile in debug messages */
	static final boolean				DEBUG	= false;

	/**
	 * The Bundle Context used by this {@code BundleTracker}.
	 */
	protected final BundleContext		context;

	/**
	 * The {@code BundleTrackerCustomizer} object for this tracker.
	 */
	final BundleTrackerCustomizer<T>	customizer;

	/**
	 * Tracked bundles: {@code Bundle} object -> customized Object and
	 * {@code BundleListener} object
	 */
	private volatile Tracked			tracked;

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
	 * State mask for bundles being tracked. This field contains the ORed values
	 * of the bundle states being tracked.
	 */
	final int	mask;

	/**
	 * Create a {@code BundleTracker} for bundles whose state is present in the
	 * specified state mask.
	 * 
	 * <p>
	 * Bundles whose state is present on the specified state mask will be
	 * tracked by this {@code BundleTracker}.
	 * 
	 * @param context The {@code BundleContext} against which the tracking is
	 *        done.
	 * @param stateMask The bit mask of the {@code OR}ing of the bundle states
	 *        to be tracked.
	 * @param customizer The customizer object to call when bundles are added,
	 *        modified, or removed in this {@code BundleTracker}. If customizer
	 *        is {@code null}, then this {@code BundleTracker} will be used as
	 *        the {@code BundleTrackerCustomizer} and this {@code BundleTracker}
	 *        will call the {@code BundleTrackerCustomizer} methods on itself.
	 * @see Bundle#getState()
	 */
	public BundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer<T> customizer) {
		this.context = context;
		this.mask = stateMask;
		this.customizer = (customizer == null) ? this : customizer;
	}

	/**
	 * Open this {@code BundleTracker} and begin tracking bundles.
	 * 
	 * <p>
	 * Bundle which match the state criteria specified when this
	 * {@code BundleTracker} was created are now tracked by this
	 * {@code BundleTracker}.
	 * 
	 * @throws java.lang.IllegalStateException If the {@code BundleContext} with
	 *         which this {@code BundleTracker} was created is no longer valid.
	 * @throws java.lang.SecurityException If the caller and this class do not
	 *         have the appropriate
	 *         {@code AdminPermission[context bundle,LISTENER]}, and the Java
	 *         Runtime Environment supports permissions.
	 */
	public void open() {
		final Tracked t;
		synchronized (this) {
			if (tracked != null) {
				return;
			}
			if (DEBUG) {
				System.out.println("BundleTracker.open"); //$NON-NLS-1$
			}
			t = new Tracked();
			synchronized (t) {
				context.addBundleListener(t);
				Bundle[] bundles = context.getBundles();
				if (bundles != null) {
					int length = bundles.length;
					for (int i = 0; i < length; i++) {
						int state = bundles[i].getState();
						if ((state & mask) == 0) {
							/* null out bundles whose states are not interesting */
							bundles[i] = null;
						}
					}
					/* set tracked with the initial bundles */
					t.setInitial(bundles);
				}
			}
			tracked = t;
		}
		/* Call tracked outside of synchronized region */
		t.trackInitial(); /* process the initial references */
	}

	/**
	 * Close this {@code BundleTracker}.
	 * 
	 * <p>
	 * This method should be called when this {@code BundleTracker} should end
	 * the tracking of bundles.
	 * 
	 * <p>
	 * This implementation calls {@link #getBundles()} to get the list of
	 * tracked bundles to remove.
	 */
	public void close() {
		final Bundle[] bundles;
		final Tracked outgoing;
		synchronized (this) {
			outgoing = tracked;
			if (outgoing == null) {
				return;
			}
			if (DEBUG) {
				System.out.println("BundleTracker.close"); //$NON-NLS-1$
			}
			outgoing.close();
			bundles = getBundles();
			tracked = null;
			try {
				context.removeBundleListener(outgoing);
			} catch (IllegalStateException e) {
				/* In case the context was stopped. */
			}
		}
		if (bundles != null) {
			for (int i = 0; i < bundles.length; i++) {
				outgoing.untrack(bundles[i], null);
			}
		}
	}

	/**
	 * Default implementation of the
	 * {@code BundleTrackerCustomizer.addingBundle} method.
	 * 
	 * <p>
	 * This method is only called when this {@code BundleTracker} has been
	 * constructed with a {@code null BundleTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation simply returns the specified {@code Bundle}.
	 * 
	 * <p>
	 * This method can be overridden in a subclass to customize the object to be
	 * tracked for the bundle being added.
	 * 
	 * @param bundle The {@code Bundle} being added to this
	 *        {@code BundleTracker} object.
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @return The specified bundle.
	 * @see BundleTrackerCustomizer#addingBundle(Bundle, BundleEvent)
	 */
	public T addingBundle(Bundle bundle, BundleEvent event) {
		T result = (T) bundle;
		return result;
	}

	/**
	 * Default implementation of the
	 * {@code BundleTrackerCustomizer.modifiedBundle} method.
	 * 
	 * <p>
	 * This method is only called when this {@code BundleTracker} has been
	 * constructed with a {@code null BundleTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation does nothing.
	 * 
	 * @param bundle The {@code Bundle} whose state has been modified.
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @param object The customized object for the specified Bundle.
	 * @see BundleTrackerCustomizer#modifiedBundle(Bundle, BundleEvent, Object)
	 */
	public void modifiedBundle(Bundle bundle, BundleEvent event, T object) {
		/* do nothing */
	}

	/**
	 * Default implementation of the
	 * {@code BundleTrackerCustomizer.removedBundle} method.
	 * 
	 * <p>
	 * This method is only called when this {@code BundleTracker} has been
	 * constructed with a {@code null BundleTrackerCustomizer} argument.
	 * 
	 * <p>
	 * This implementation does nothing.
	 * 
	 * @param bundle The {@code Bundle} being removed.
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @param object The customized object for the specified bundle.
	 * @see BundleTrackerCustomizer#removedBundle(Bundle, BundleEvent, Object)
	 */
	public void removedBundle(Bundle bundle, BundleEvent event, T object) {
		/* do nothing */
	}

	/**
	 * Return an array of {@code Bundle}s for all bundles being tracked by this
	 * {@code BundleTracker}.
	 * 
	 * @return An array of {@code Bundle}s or {@code null} if no bundles are
	 *         being tracked.
	 */
	public Bundle[] getBundles() {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return null;
		}
		synchronized (t) {
			int length = t.size();
			if (length == 0) {
				return null;
			}
			return t.copyKeys(new Bundle[length]);
		}
	}

	/**
	 * Returns the customized object for the specified {@code Bundle} if the
	 * specified bundle is being tracked by this {@code BundleTracker}.
	 * 
	 * @param bundle The {@code Bundle} being tracked.
	 * @return The customized object for the specified {@code Bundle} or
	 *         {@code null} if the specified {@code Bundle} is not being
	 *         tracked.
	 */
	public T getObject(Bundle bundle) {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return null;
		}
		synchronized (t) {
			return t.getCustomizedObject(bundle);
		}
	}

	/**
	 * Remove a bundle from this {@code BundleTracker}.
	 * 
	 * The specified bundle will be removed from this {@code BundleTracker} . If
	 * the specified bundle was being tracked then the
	 * {@code BundleTrackerCustomizer.removedBundle} method will be called for
	 * that bundle.
	 * 
	 * @param bundle The {@code Bundle} to be removed.
	 */
	public void remove(Bundle bundle) {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return;
		}
		t.untrack(bundle, null);
	}

	/**
	 * Return the number of bundles being tracked by this {@code BundleTracker}.
	 * 
	 * @return The number of bundles being tracked.
	 */
	public int size() {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return 0;
		}
		synchronized (t) {
			return t.size();
		}
	}

	/**
	 * Returns the tracking count for this {@code BundleTracker}.
	 * 
	 * The tracking count is initialized to 0 when this {@code BundleTracker} is
	 * opened. Every time a bundle is added, modified or removed from this
	 * {@code BundleTracker} the tracking count is incremented.
	 * 
	 * <p>
	 * The tracking count can be used to determine if this {@code BundleTracker}
	 * has added, modified or removed a bundle by comparing a tracking count
	 * value previously collected with the current tracking count value. If the
	 * value has not changed, then no bundle has been added, modified or removed
	 * from this {@code BundleTracker} since the previous tracking count was
	 * collected.
	 * 
	 * @return The tracking count for this {@code BundleTracker} or -1 if this
	 *         {@code BundleTracker} is not open.
	 */
	public int getTrackingCount() {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return -1;
		}
		synchronized (t) {
			return t.getTrackingCount();
		}
	}

	/**
	 * Return a {@code Map} with the {@code Bundle}s and customized objects for
	 * all bundles being tracked by this {@code BundleTracker}.
	 * 
	 * @return A {@code Map} with the {@code Bundle}s and customized objects for
	 *         all services being tracked by this {@code BundleTracker}. If no
	 *         bundles are being tracked, then the returned map is empty.
	 * @since 1.5
	 */
	public Map<Bundle, T> getTracked() {
		Map<Bundle, T> map = new HashMap<Bundle, T>();
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return map;
		}
		synchronized (t) {
			return t.copyEntries(map);
		}
	}

	/**
	 * Return if this {@code BundleTracker} is empty.
	 * 
	 * @return {@code true} if this {@code BundleTracker} is not tracking any
	 *         bundles.
	 * @since 1.5
	 */
	public boolean isEmpty() {
		final Tracked t = tracked();
		if (t == null) { /* if BundleTracker is not open */
			return true;
		}
		synchronized (t) {
			return t.isEmpty();
		}
	}

	/**
	 * Inner class which subclasses AbstractTracked. This class is the
	 * {@code SynchronousBundleListener} object for the tracker.
	 * 
	 * @ThreadSafe
	 * @since 1.4
	 */
	private final class Tracked extends AbstractTracked<Bundle, T, BundleEvent> implements SynchronousBundleListener {
		/**
		 * Tracked constructor.
		 */
		Tracked() {
			super();
		}

		/**
		 * {@code BundleListener} method for the {@code BundleTracker} class.
		 * This method must NOT be synchronized to avoid deadlock potential.
		 * 
		 * @param event {@code BundleEvent} object from the framework.
		 */
		public void bundleChanged(final BundleEvent event) {
			/*
			 * Check if we had a delayed call (which could happen when we
			 * close).
			 */
			if (closed) {
				return;
			}
			final Bundle bundle = event.getBundle();
			final int state = bundle.getState();
			if (DEBUG) {
				System.out.println("BundleTracker.Tracked.bundleChanged[" + state + "]: " + bundle); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if ((state & mask) != 0) {
				track(bundle, event);
				/*
				 * If the customizer throws an unchecked exception, it is safe
				 * to let it propagate
				 */
			} else {
				untrack(bundle, event);
				/*
				 * If the customizer throws an unchecked exception, it is safe
				 * to let it propagate
				 */
			}
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
		T customizerAdding(final Bundle item, final BundleEvent related) {
			return customizer.addingBundle(item, related);
		}

		/**
		 * Call the specific customizer modified method. This method must not be
		 * called while synchronized on this object.
		 * 
		 * @param item Tracked item.
		 * @param related Action related object.
		 * @param object Customized object for the tracked item.
		 */
		void customizerModified(final Bundle item, final BundleEvent related, final T object) {
			customizer.modifiedBundle(item, related, object);
		}

		/**
		 * Call the specific customizer removed method. This method must not be
		 * called while synchronized on this object.
		 * 
		 * @param item Tracked item.
		 * @param related Action related object.
		 * @param object Customized object for the tracked item.
		 */
		void customizerRemoved(final Bundle item, final BundleEvent related, final T object) {
			customizer.removedBundle(item, related, object);
		}
	}
}
