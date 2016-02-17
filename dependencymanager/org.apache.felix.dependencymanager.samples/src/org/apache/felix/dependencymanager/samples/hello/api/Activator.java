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
package org.apache.felix.dependencymanager.samples.hello.api;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        dm.add(createComponent()
            .setImplementation(ServiceProviderImpl.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .setInterface(ServiceProvider.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(ServiceConsumer.class)            
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(createConfigurationDependency().setCallback("updated", ServiceConsumerConf.class))
            .add(createServiceDependency().setService(ServiceProvider.class).setRequired(true)));
    }
}
