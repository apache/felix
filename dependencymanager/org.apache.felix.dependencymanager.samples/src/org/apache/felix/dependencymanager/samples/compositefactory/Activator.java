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
package org.apache.felix.dependencymanager.samples.compositefactory;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Defines a composite service using a composition manager that is used to
 * instantiate component instances, depending of what is found from the configuration.
 * The configuration is created from the org.apache.felix.dependencymanager.samples.conf.Configurator component.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager m) throws Exception {
        CompositionManager factory = new CompositionManager();
        m.add(createComponent()
            .setFactory(factory, "create")
            .setComposition(factory, "getComposition")
            .add(createConfigurationDependency().setPid(CompositionManager.class.getName()).setCallback(factory, "updated"))
            .add(createServiceDependency().setService(LogService.class).setRequired(true)));
    }
}
