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
import org.apache.felix.ipojo.dependency.interceptors.DefaultServiceRankingInterceptor;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An implementation of the ranking interceptor accepting only services with a 'grade' property and sorting them by
 * value.
 */
@Component(immediate = true)
@Provides
public class FilterRankingInterceptor extends DefaultServiceRankingInterceptor implements Setter {

    @ServiceProperty
    private String target;

    private Comparator<ServiceReference> comparator;

    private boolean inverse = false;

    public FilterRankingInterceptor() {
        comparator = new GradeComparator();
    }

    @Override
    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching) {
        List<ServiceReference> references = new ArrayList<ServiceReference>();
        for (ServiceReference ref : matching) {
            if (ref.getProperty("grade") != null) {
                references.add(ref);
            }
        }

        Collections.sort(references, comparator);
        if (inverse) {
            Collections.reverse(references);
        }
        return references;
    }

    @Override
    public void set(String newValue) {
        inverse = Boolean.parseBoolean(newValue);
        invalidateSelectedServices();
    }
}
