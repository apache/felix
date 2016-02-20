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
package org.apache.felix.dm.lambda;

/**
 * Builds a Dependency Manager Service Adapter Component.
 * <p> The adapter will be applied to any service that matches the specified interface and filter. For each matching service an adapter will be created 
 * based on the adapter implementation class. The adapter will be registered with the specified interface and existing properties from the original 
 * service plus any extra properties you supply here.<p>
 * 
 * Code example that adapts a "Device" service to an HttpServlet service. The adapter is created using a ServiceAdapterBuilder that is passed to the lambda. 
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *        adapter(Device.class, adapt -> adapt.impl(DeviceServlet.class).provides(HttpServlet.class).properties(alias -> "/device");                    
 *    }
 * }}</pre>
 * 
 * @param <T> the adaptee service
 */
public interface ServiceAdapterBuilder<T> extends ComponentBuilder<ServiceAdapterBuilder<T>>, ServiceCallbacksBuilder<T, ServiceAdapterBuilder<T>> {
    /**
     * Specifies the filter used to match a given adapted service.
     * 
     * @param adapteeFilter the filter used to match a given adapted service
     * @return this builder
     */
    ServiceAdapterBuilder<T> filter(String adapteeFilter);
    
    /**
     * Specifies whether or not the adapted service properties must be propagated to the adapter service (true by default). 
     * 
     * @param propagate true if the adapted service properties must be propagated to the adapter service (true by default). 
     * @return this builder
     */
    ServiceAdapterBuilder<T> propagate(boolean propagate);
    
    /**
     * Injects this adapted service in all fields matching the adapted service type.
     * 
     * @return this builder
     */
    ServiceAdapterBuilder<T> autoConfig();
    
    /**
     * Configures whether or not the adapted service can be injected in all fields matching the adapted service type. 
     * 
     * @param autoConfig true if the adapted service can be injected in all fields matching the adapted service type
     * @return this builder
     */
    ServiceAdapterBuilder<T> autoConfig(boolean autoConfig);
    
    /**
     * Injects this adapted service on the field matching the given name
     * 
     * @param field the field name where the adapted service must be injected to.
     * @return this builder
     */
    ServiceAdapterBuilder<T> autoConfig(String field);        
}
