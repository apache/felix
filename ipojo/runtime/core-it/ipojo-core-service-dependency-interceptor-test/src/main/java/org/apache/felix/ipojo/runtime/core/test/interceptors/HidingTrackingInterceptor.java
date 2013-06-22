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
import org.apache.felix.ipojo.dependency.interceptors.DefaultServiceTrackingInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.TransformedServiceReference;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;

/**
 * An interceptor adding a property (location) and hiding another property (hidden)
 * Not instantiated by default.
 */
@Component(immediate = true)
@Provides
public class HidingTrackingInterceptor extends DefaultServiceTrackingInterceptor implements Setter {

    @ServiceProperty
    private String target;

    private String prop = "hidden";


    @Override
    public <S> TransformedServiceReference<S> accept(DependencyModel dependency, BundleContext context,
                                          TransformedServiceReference<S> ref) {
        if (ref.contains(prop)) {
            return null;
        } else {
            return ref;
        }
    }

    @Override
    public void set(String newValue) {
        prop = newValue;
        invalidateMatchingServices();
    }
}
