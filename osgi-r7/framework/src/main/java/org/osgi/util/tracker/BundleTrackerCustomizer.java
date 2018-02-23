/*
 * Copyright (c) OSGi Alliance (2007, 2013). All Rights Reserved.
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

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

/**
 * The {@code BundleTrackerCustomizer} interface allows a {@code BundleTracker}
 * to customize the {@code Bundle}s that are tracked. A
 * {@code BundleTrackerCustomizer} is called when a bundle is being added to a
 * {@code BundleTracker}. The {@code BundleTrackerCustomizer} can then return an
 * object for the tracked bundle. A {@code BundleTrackerCustomizer} is also
 * called when a tracked bundle is modified or has been removed from a
 * {@code BundleTracker}.
 * 
 * <p>
 * The methods in this interface may be called as the result of a
 * {@code BundleEvent} being received by a {@code BundleTracker}. Since
 * {@code BundleEvent}s are received synchronously by the {@code BundleTracker},
 * it is highly recommended that implementations of these methods do not alter
 * bundle states while being synchronized on any object.
 * 
 * <p>
 * The {@code BundleTracker} class is thread-safe. It does not call a
 * {@code BundleTrackerCustomizer} while holding any locks.
 * {@code BundleTrackerCustomizer} implementations must also be thread-safe.
 * 
 * @param <T> The type of the tracked object.
 * @ThreadSafe
 * @author $Id: 031b2979522768150d23ee70dfe62528432c19f7 $
 * @since 1.4
 */
@ConsumerType
public interface BundleTrackerCustomizer<T> {
	/**
	 * A bundle is being added to the {@code BundleTracker}.
	 * 
	 * <p>
	 * This method is called before a bundle which matched the search parameters
	 * of the {@code BundleTracker} is added to the {@code BundleTracker}. This
	 * method should return the object to be tracked for the specified
	 * {@code Bundle}. The returned object is stored in the
	 * {@code BundleTracker} and is available from the
	 * {@link BundleTracker#getObject(Bundle) getObject} method.
	 * 
	 * @param bundle The {@code Bundle} being added to the {@code BundleTracker}
	 *        .
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @return The object to be tracked for the specified {@code Bundle} object
	 *         or {@code null} if the specified {@code Bundle} object should not
	 *         be tracked.
	 */
	public T addingBundle(Bundle bundle, BundleEvent event);

	/**
	 * A bundle tracked by the {@code BundleTracker} has been modified.
	 * 
	 * <p>
	 * This method is called when a bundle being tracked by the
	 * {@code BundleTracker} has had its state modified.
	 * 
	 * @param bundle The {@code Bundle} whose state has been modified.
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @param object The tracked object for the specified bundle.
	 */
	public void modifiedBundle(Bundle bundle, BundleEvent event, T object);

	/**
	 * A bundle tracked by the {@code BundleTracker} has been removed.
	 * 
	 * <p>
	 * This method is called after a bundle is no longer being tracked by the
	 * {@code BundleTracker}.
	 * 
	 * @param bundle The {@code Bundle} that has been removed.
	 * @param event The bundle event which caused this customizer method to be
	 *        called or {@code null} if there is no bundle event associated with
	 *        the call to this method.
	 * @param object The tracked object for the specified bundle.
	 */
	public void removedBundle(Bundle bundle, BundleEvent event, T object);
}
