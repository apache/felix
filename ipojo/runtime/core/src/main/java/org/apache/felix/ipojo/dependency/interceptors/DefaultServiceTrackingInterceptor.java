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

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the default service tracking interceptor.
 * It accepts all references and keeps the dependencies in the `dependencies` list. This list is guarded by the
 * monitor lock.
 *
 * It also provides an `invalidateMatchingServices` method notifying all managed dependencies of a change in the
 * matching service set.
 */
public class DefaultServiceTrackingInterceptor extends DefaultDependencyInterceptor implements ServiceTrackingInterceptor {

    /**
     * Default implementation of the accept method.
     * The default behavior is to accept all services as they are (no transformation).
     * @param dependency the dependency the dependency
     * @param context the context of the dependency the bundle context used by the dependency
     * @param ref the reference the reference to accept, transform or reject
     * @param <S> the type of service
     * @return the reference as it is.
     */
    public <S> TransformedServiceReference<S> accept(DependencyModel dependency, BundleContext context, TransformedServiceReference<S> ref) {
        return ref;
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
