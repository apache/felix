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

package org.apache.felix.ipojo.dependency.impl;

import org.apache.felix.ipojo.dependency.interceptors.ServiceRankingInterceptor;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A comparator based version of the ranking interceptor.
 */
public class ComparatorBasedServiceRankingInterceptor implements ServiceRankingInterceptor {

    private final Comparator<ServiceReference> m_comparator;

    public ComparatorBasedServiceRankingInterceptor(Comparator<ServiceReference> cmp) {
        this.m_comparator = cmp;
    }


    public void open(DependencyModel dependency) {    }

    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching) {
        List<ServiceReference> copy = new ArrayList<ServiceReference>(matching);
        Collections.sort(copy, m_comparator);
        return copy;
    }

    public List<ServiceReference> onServiceArrival(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    public List<ServiceReference> onServiceDeparture(DependencyModel dependency, List<ServiceReference> matching,
                                                     ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    public List<ServiceReference> onServiceModified(DependencyModel dependency, List<ServiceReference> matching, ServiceReference<?> reference) {
        return getServiceReferences(dependency, matching);
    }

    public void close(DependencyModel dependency) {  }
}
