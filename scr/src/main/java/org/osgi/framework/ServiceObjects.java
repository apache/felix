/*
 * Copyright (c) OSGi Alliance (2012, 2014). All Rights Reserved.
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

package org.osgi.framework;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Allows multiple service objects for a service to be obtained.
 * 
 * <p>
 * For services with {@link Constants#SCOPE_PROTOTYPE prototype} scope, multiple
 * service objects for the service can be obtained. For services with
 * {@link Constants#SCOPE_SINGLETON singleton} or {@link Constants#SCOPE_BUNDLE
 * bundle} scope, only one, use-counted service object is available to a
 * requesting bundle.
 * 
 * <p>
 * Any unreleased service objects obtained from this {@code ServiceObjects}
 * object are automatically released by the framework when the bundle associated
 * with the BundleContext used to create this {@code ServiceObjects} object is
 * stopped.
 * 
 * @param <S> Type of Service
 * @see BundleContext#getServiceObjects(ServiceReference)
 * @see PrototypeServiceFactory
 * @ThreadSafe
 * @since 1.8
 * @author $Id: 99314fe285a227cd63a21814a2300b109845125f $
 */
@ProviderType
public interface ServiceObjects<S> {
	/**
	 * Returns a service object for the {@link #getServiceReference()
	 * associated} service.
	 * 
	 * <p>
	 * This {@code ServiceObjects} object can be used to obtain multiple service
	 * objects for the associated service if the service has
	 * {@link Constants#SCOPE_PROTOTYPE prototype} scope.
	 * 
	 * <p>
	 * If the associated service has {@link Constants#SCOPE_SINGLETON singleton}
	 * or {@link Constants#SCOPE_BUNDLE bundle} scope, this method behaves the
	 * same as calling the {@link BundleContext#getService(ServiceReference)}
	 * method for the associated service. That is, only one, use-counted service
	 * object is available from this {@link ServiceObjects} object.
	 * 
	 * <p>
	 * This method will always return {@code null} when the associated service
	 * has been unregistered.
	 * 
	 * <p>
	 * For a prototype scope service, the following steps are required to obtain
	 * a service object:
	 * <ol>
	 * <li>If the associated service has been unregistered, {@code null} is
	 * returned.</li>
	 * <li>The
	 * {@link PrototypeServiceFactory#getService(Bundle, ServiceRegistration)}
	 * method is called to supply a customized service object for the caller.</li>
	 * <li>If the service object returned by the {@code PrototypeServiceFactory}
	 * object is {@code null}, not an {@code instanceof} all the classes named
	 * when the service was registered or the {@code PrototypeServiceFactory}
	 * object throws an exception, {@code null} is returned and a Framework
	 * event of type {@link FrameworkEvent#ERROR} containing a
	 * {@link ServiceException} describing the error is fired.</li>
	 * <li>The customized service object is returned.</li>
	 * </ol>
	 * 
	 * @return A service object for the associated service or {@code null} if
	 *         the service is not registered, the customized service object
	 *         returned by a {@code ServiceFactory} does not implement the
	 *         classes under which it was registered or the
	 *         {@code ServiceFactory} threw an exception.
	 * @throws IllegalStateException If the BundleContext used to create this
	 *         {@code ServiceObjects} object is no longer valid.
	 * @see #ungetService(Object)
	 */
	public S getService();

	/**
	 * Releases a service object for the {@link #getServiceReference()
	 * associated} service.
	 * 
	 * <p>
	 * This {@code ServiceObjects} object can be used to obtain multiple service
	 * objects for the associated service if the service has
	 * {@link Constants#SCOPE_PROTOTYPE prototype} scope. If the associated
	 * service has {@link Constants#SCOPE_SINGLETON singleton} or
	 * {@link Constants#SCOPE_BUNDLE bundle} scope, this method behaves the same
	 * as calling the {@link BundleContext#ungetService(ServiceReference)}
	 * method for the associated service. That is, only one, use-counted service
	 * object is available from this {@link ServiceObjects} object.
	 * 
	 * <p>
	 * For a prototype scope service, the following steps are required to
	 * release a service object:
	 * <ol>
	 * <li>If the associated service has been unregistered, this method returns
	 * without doing anything.</li>
	 * <li>The
	 * {@link PrototypeServiceFactory#ungetService(Bundle, ServiceRegistration, Object)}
	 * method is called to release the specified service object.</li>
	 * </ol>
	 * 
	 * <p>
	 * The specified service object must no longer be used and all references to
	 * it should be destroyed after calling this method.
	 * 
	 * @param service A service object previously provided by this
	 *        {@code ServiceObjects} object.
	 * @throws IllegalStateException If the BundleContext used to create this
	 *         {@code ServiceObjects} object is no longer valid.
	 * @throws IllegalArgumentException If the specified service object was not
	 *         provided by this {@code ServiceObjects} object.
	 * @see #getService()
	 */
	public void ungetService(S service);

	/**
	 * Returns the {@link ServiceReference} for the service associated with this
	 * {@code ServiceObjects} object.
	 * 
	 * @return The {@link ServiceReference} for the service associated with this
	 *         {@code ServiceObjects} object.
	 */
	public ServiceReference<S> getServiceReference();
}
