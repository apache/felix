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
package org.apache.felix.scr.impl.manager;

import org.apache.felix.scr.impl.helper.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public interface ComponentActivator extends Logger, ExtendedServiceListenerContext<ExtendedServiceEvent> {

    BundleContext getBundleContext();

    boolean isActive();

    ScrConfiguration getConfiguration();

    void schedule(Runnable runnable);

    long registerComponentId(AbstractComponentManager<?> sAbstractComponentManager);

    void unregisterComponentId(AbstractComponentManager<?> sAbstractComponentManager);

    <T> boolean enterCreate(ServiceReference<T> reference);

    <T> void leaveCreate(ServiceReference<T> reference);

    <S, T> void registerMissingDependency(DependencyManager<S, T> dependencyManager,
                                              ServiceReference<T> serviceReference, int trackingCount);

    <T> void missingServicePresent(ServiceReference<T> serviceReference);

    void enableComponent(String name);

    void disableComponent(String name);

    RegionConfigurationSupport setRegionConfigurationSupport(ServiceReference<ConfigurationAdmin> reference);

    void unsetRegionConfigurationSupport(RegionConfigurationSupport rcs);

}
