/*
 * Copyright (c) OSGi Alliance (2012, 2016). All Rights Reserved.
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

package org.osgi.service.component;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Allows multiple service objects for a service to be obtained.
 * 
 * <p>
 * A component instance can receive a {@code ComponentServiceObjects} object via
 * a reference that is typed {@code ComponentServiceObjects}.
 * 
 * <p>
 * For services with {@link Constants#SCOPE_PROTOTYPE prototype} scope, multiple
 * service objects for the service can be obtained. For services with
 * {@link Constants#SCOPE_SINGLETON singleton} or {@link Constants#SCOPE_BUNDLE
 * bundle} scope, only one, use-counted service object is available.
 * 
 * <p>
 * Any unreleased service objects obtained from this
 * {@code ComponentServiceObjects} object are automatically released by Service
 * Component Runtime when the service becomes unbound.
 * 
 * @param <S> Type of Service
 * @ThreadSafe
 * @since 1.3
 * @see ServiceObjects
 * @author $Id$
 */
@ProviderType
public interface ComponentServiceObjects<S> {
	/**
	 * Returns a service object for the {@link #getServiceReference()
	 * associated} service.
	 * <p>
	 * This method will always return {@code null} when the associated service
	 * has been become unbound.
	 * 
	 * @return A service object for the associated service or {@code null} if
	 *         the service is unbound, the customized service object returned by
	 *         a {@code ServiceFactory} does not implement the classes under
	 *         which it was registered or the {@code ServiceFactory} threw an
	 *         exception.
	 * @throws IllegalStateException If the component instance that received
	 *             this {@code ComponentServiceObjects} object has been
	 *             deactivated.
	 * @see #ungetService(Object)
	 */
	public S getService();

	/**
	 * Releases a service object for the {@link #getServiceReference()
	 * associated} service.
	 * <p>
	 * The specified service object must no longer be used and all references to
	 * it should be destroyed after calling this method.
	 * 
	 * @param service A service object previously provided by this
	 *        {@code ComponentServiceObjects} object.
	 * @throws IllegalStateException If the component instance that received
	 *             this {@code ComponentServiceObjects} object has been
	 *             deactivated.
	 * @throws IllegalArgumentException If the specified service object was not
	 *         provided by this {@code ComponentServiceObjects} object.
	 * @see #getService()
	 */
	public void ungetService(S service);

	/**
	 * Returns the {@link ServiceReference} for the service associated with this
	 * {@code ComponentServiceObjects} object.
	 * 
	 * @return The {@link ServiceReference} for the service associated with this
	 *         {@code ComponentServiceObjects} object.
	 */
	public ServiceReference<S> getServiceReference();
}
