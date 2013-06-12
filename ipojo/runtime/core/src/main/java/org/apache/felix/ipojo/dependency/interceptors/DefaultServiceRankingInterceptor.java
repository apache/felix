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

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the default service ranking interceptor.
 * This implementation does not sort the given set, so returns it as it is.
 *
 * It also provides an `invalidateSelectedServices` method notifying all managed dependencies of a change in the
 * selection service set.
 *
 * onDeparture, onArrival and onModified methods delegates to the getServiceReferences method.
 */
public class DefaultServiceRankingInterceptor extends DefaultDependencyInterceptor implements
        ServiceRankingInterceptor {

    /**
     * Notifies the managed dependencies of a change in the set of services selected by this interceptor.
     * The dependency will call the getServiceReferences method to recompute the set of selected services.
     */
    public void invalidateSelectedServices() {
        List<DependencyModel> list = new ArrayList<DependencyModel>();
        synchronized (this) {
            list.addAll(dependencies);
        }

        for (DependencyModel dep : list) {
            dep.invalidateSelectedServices();
        }
    }


    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching) {
        return matching;
    }

    public List<ServiceReference> onServiceArrival(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    public List<ServiceReference> onServiceDeparture(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    public List<ServiceReference> onServiceModified(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }
}
