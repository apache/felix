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
import org.osgi.framework.ServiceReference;

import java.util.List;

/**
 * A service to influence the sorting of services on a service dependency.
 *
 * Only one ranking interceptor can be plugged on a dependency, but an interceptor can handle several dependencies.
 *
 * This interceptors is called to compute the selected set of services from the matching set,
 * i.e. the set of services that matching the filter (actually accepted by the tracking interceptors).
 *
 * @since 1.10.1
 */
public interface ServiceRankingInterceptor extends DependencyInterceptor {

    /**
     * Gets the sorted set of selected reference.
     * @param dependency the dependency
     * @param matching the set of service to sort
     * @return the sorted set of selected reference. This set is a sub-set potentially empty of the given list of
     * references.
     */
    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching);

    /**
     * A new service arrives in the matching set. This method is called to retrieve the new sorted set of selected
     * services.
     * @param dependency the dependency
     * @param matching the set of matching service
     * @param reference the arriving reference
     * @return the new sorted set of service
     */
    public List<ServiceReference> onServiceArrival(DependencyModel dependency, List<ServiceReference> matching,
                                                   ServiceReference<?> reference);

    /**
     * A service leaves the matching set. This method is called to retrieve the new sorted set of selected
     * services.
     * @param dependency the dependency
     * @param matching the set of matching service
     * @param reference the leaving reference
     * @return the new sorted set of service
     */
    public List<ServiceReference> onServiceDeparture(DependencyModel dependency, List<ServiceReference> matching,
                                                     ServiceReference<?> reference);

    /**
     * A service from the matching set was modified. This method is called to retrieve the new sorted set of selected
     * services.
     * @param dependency the dependency
     * @param matching the set of matching service
     * @param reference the modified service
     * @return the new sorted set of service
     */
    public List<ServiceReference> onServiceModified(DependencyModel dependency, List<ServiceReference> matching,
                                              ServiceReference<?> reference);

}
