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

package org.apache.felix.ipojo.dependency.interceptors;

import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A service to modify / monitor the service bindings.
 * This service is notified every time the dependency is weaving a new service bindings on un-weaves an existing one.
 *
 * Several binding interceptors can be plugged to the same service dependency. In this case,
 * a chain is created where all interceptor are called in a sequence.
 *
 * A binding interceptor cannot modify the existing bindings.
 *
 * Obviously an interceptor can be plugged to several dependencies.
 *
 * @since 1.11.0
 */
public interface ServiceBindingInterceptor extends DependencyInterceptor {

    /**
     * Notification method when a dependency is weaving a new service binding.
     * The interceptor can modify the service object. It must <strong>never</strong> return a {@code null} object,
     * but the receive service object if it does not want to do anything with the service object.
     *
     * When the interceptor <em>modifies</em> the service object, the returned object <strong>must</strong> be
     * compatible with the dependency specification.
     *
     * The received service object may already have been <em>wrapped</em> by binding interceptors called before the
     * current one.
     *
     * @param dependency the dependency
     * @param reference the service reference bound
     * @param service the service object
     * @param <S> the service specification
     * @return the service object to be injected within the component. Must never be {@code null}.
     */
    public <S> S getService(DependencyModel dependency, ServiceReference<S> reference, S service);

    /**
     * Notification method when a dependency is un-weaving a service binding.
     * The interceptor must released all objects related to this service binding.
     *
     * @param dependency the dependency
     * @param reference the unbound service reference
     * @param <S> the service specification
     */
    public <S> void ungetService(DependencyModel dependency, ServiceReference<S> reference);

}
