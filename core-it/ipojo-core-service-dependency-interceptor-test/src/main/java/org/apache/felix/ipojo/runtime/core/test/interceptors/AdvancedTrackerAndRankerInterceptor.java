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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.dependency.interceptors.DefaultDependencyInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceRankingInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceTrackingInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.TransformedServiceReference;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationListener;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An interceptor implements both ranking and tracking interfaces.
 * It first adds the 'intercepted' property to the services, and then select only the provider having a grade
 * matching the instance's 'grade' property.
 */
@Component(immediate = true)
@Provides
public class AdvancedTrackerAndRankerInterceptor extends DefaultDependencyInterceptor implements
        ServiceTrackingInterceptor, ServiceRankingInterceptor, ConfigurationListener {

    @ServiceProperty
    private String target;

    @Override
    public void close(DependencyModel dependency) {
        super.close(dependency);
        ConfigurationHandlerDescription handler = (ConfigurationHandlerDescription) dependency.getComponentInstance().getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:properties");
        handler.removeListener(this);
    }

    @Override
    public void open(DependencyModel dependency) {
        super.open(dependency);
        ConfigurationHandlerDescription handler = (ConfigurationHandlerDescription) dependency.getComponentInstance().getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:properties");
        handler.addListener(this);
    }

    @Override
    public List<ServiceReference> getServiceReferences(DependencyModel dependency, List<ServiceReference> matching) {
       List<ServiceReference> list = new ArrayList<ServiceReference>();
       Integer grade = getInstanceGrade(dependency);
       for (ServiceReference ref : matching) {
           if (grade.equals(ref.getProperty("grade"))) {
               list.add(ref);
           }
       }
        return list;
    }

    private Integer getInstanceGrade(DependencyModel dependency) {
        ConfigurationHandlerDescription handler = (ConfigurationHandlerDescription) dependency.getComponentInstance().getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:properties");
        return (Integer) handler.getPropertyByName("grade").getCurrentValue();
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
       return ref.addProperty("intercepted", true);
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

    @Override
    public void configurationChanged(ComponentInstance instance, Map<String, Object> configuration) {
        invalidateSelectedServices();
    }
}
