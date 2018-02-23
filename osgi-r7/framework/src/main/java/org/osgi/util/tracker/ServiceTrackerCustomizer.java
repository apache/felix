/*
 * Copyright (c) OSGi Alliance (2000, 2013). All Rights Reserved.
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
import org.osgi.framework.ServiceReference;

/**
 * The {@code ServiceTrackerCustomizer} interface allows a
 * {@code ServiceTracker} to customize the service objects that are tracked. A
 * {@code ServiceTrackerCustomizer} is called when a service is being added to a
 * {@code ServiceTracker}. The {@code ServiceTrackerCustomizer} can then return
 * an object for the tracked service. A {@code ServiceTrackerCustomizer} is also
 * called when a tracked service is modified or has been removed from a
 * {@code ServiceTracker}.
 * 
 * <p>
 * The methods in this interface may be called as the result of a
 * {@code ServiceEvent} being received by a {@code ServiceTracker}. Since
 * {@code ServiceEvent}s are synchronously delivered by the Framework, it is
 * highly recommended that implementations of these methods do not register (
 * {@code BundleContext.registerService}), modify (
 * {@code ServiceRegistration.setProperties}) or unregister (
 * {@code ServiceRegistration.unregister}) a service while being synchronized on
 * any object.
 * 
 * <p>
 * The {@code ServiceTracker} class is thread-safe. It does not call a
 * {@code ServiceTrackerCustomizer} while holding any locks.
 * {@code ServiceTrackerCustomizer} implementations must also be thread-safe.
 * 
 * @param <S> The type of the service being tracked.
 * @param <T> The type of the tracked object.
 * @ThreadSafe
 * @author $Id: 0c3333455f7d80a7793c77ac9671baa4a02a89b9 $
 */
@ConsumerType
public interface ServiceTrackerCustomizer<S, T> {
	/**
	 * A service is being added to the {@code ServiceTracker}.
	 * 
	 * <p>
	 * This method is called before a service which matched the search
	 * parameters of the {@code ServiceTracker} is added to the
	 * {@code ServiceTracker}. This method should return the service object to
	 * be tracked for the specified {@code ServiceReference}. The returned
	 * service object is stored in the {@code ServiceTracker} and is available
	 * from the {@code getService} and {@code getServices} methods.
	 * 
	 * @param reference The reference to the service being added to the
	 *        {@code ServiceTracker}.
	 * @return The service object to be tracked for the specified referenced
	 *         service or {@code null} if the specified referenced service
	 *         should not be tracked.
	 */
	public T addingService(ServiceReference<S> reference);

	/**
	 * A service tracked by the {@code ServiceTracker} has been modified.
	 * 
	 * <p>
	 * This method is called when a service being tracked by the
	 * {@code ServiceTracker} has had it properties modified.
	 * 
	 * @param reference The reference to the service that has been modified.
	 * @param service The service object for the specified referenced service.
	 */
	public void modifiedService(ServiceReference<S> reference, T service);

	/**
	 * A service tracked by the {@code ServiceTracker} has been removed.
	 * 
	 * <p>
	 * This method is called after a service is no longer being tracked by the
	 * {@code ServiceTracker}.
	 * 
	 * @param reference The reference to the service that has been removed.
	 * @param service The service object for the specified referenced service.
	 */
	public void removedService(ServiceReference<S> reference, T service);
}
