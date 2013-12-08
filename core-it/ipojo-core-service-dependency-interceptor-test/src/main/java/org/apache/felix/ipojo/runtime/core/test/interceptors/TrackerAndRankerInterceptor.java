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
package org.apache.felix.ipojo.runtime.core.test.interceptors;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.dependency.interceptors.DefaultDependencyInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceRankingInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceTrackingInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.TransformedServiceReference;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An interceptor implements both ranking and tracking interfaces.
 */
@Component(immediate = true)
@Provides
public class TrackerAndRankerInterceptor extends DefaultDependencyInterceptor implements Setter,
        ServiceTrackingInterceptor, ServiceRankingInterceptor {

    private static final int HIGH = 1;
    private static final int LOW = 0;

    @ServiceProperty
    private String target;

    GradeComparator comparator = new GradeComparator();
    private int mode = HIGH;
    private boolean reverse = false;

    @Override
    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching) {
        List<ServiceReference> references = new ArrayList<ServiceReference>();
        for (ServiceReference ref : matching) {
            if (ref.getProperty("grade") != null) {
                references.add(ref);
            }
        }

        Collections.sort(references, comparator);
        if (reverse) {
            Collections.reverse(references);
        }
        return references;
    }

    @Override
    public List<ServiceReference> onServiceArrival(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    @Override
    public List<ServiceReference> onServiceDeparture(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    @Override
    public List<ServiceReference> onServiceModified(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    @Override
    public <S> TransformedServiceReference<S> accept(DependencyModel dependency, BundleContext context, TransformedServiceReference<S> ref) {
        // Only accept services having a grade in LOW or HIGH according to the mode.
        if (mode == HIGH) {
           if (((Integer) ref.get("grade")) > 3) {
               return ref;
           }
        } else {
            if (((Integer) ref.get("grade")) <= 3) {
                return ref;
            }
        }
        return null;
    }

    @Override
    public void set(String newValue) {
        if (newValue.contains("HIGH")) {
            mode = HIGH;
            invalidateMatchingServices();
        }
        if (newValue.contains("LOW")) {
            mode = LOW;
            invalidateMatchingServices();
        }
        if (newValue.contains("REVERSE")) {
            reverse = true;
            invalidateSelectedServices();
        }
    }

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

    /**
     * Notifies the managed dependencies of a change in the set of services accepted by this interceptor.
     * The dependency will call the accept method to recompute the set of matching services.
     */
    public void invalidateMatchingServices() {
        List<DependencyModel> list = new ArrayList<DependencyModel>();
        synchronized (this) {
            list.addAll(dependencies);
        }

        for (DependencyModel dep : list) {
            dep.invalidateMatchingServices();
        }
    }
}
