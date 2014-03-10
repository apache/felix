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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util;

import org.apache.felix.ipojo.manipulator.spi.ModuleProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.DefaultBindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.IgnoreAllBindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.LegacyGenericBindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.MetaAnnotationBindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.manipulator.spi.Module;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Bindings {

    public static BindingRegistry newBindingRegistry(final Reporter reporter,
                                                     final ResourceStore store,
                                                     final ModuleProvider provider) {

        // Build the registry by aggregation of the features we want
        // TODO We can enable/disable the legacy support easily here
        BindingRegistry registry = new DefaultBindingRegistry(reporter);
        registry = new MetaAnnotationBindingRegistry(registry, reporter, store);
        registry = new LegacyGenericBindingRegistry(registry, reporter);
        registry = new IgnoreAllBindingRegistry(registry, reporter);

        // Build each Module and add its contributed Bindings in the registry
        for (Module module : provider.findModules()) {
            module.load();
            registry.addBindings(module);
        }

        return registry;
    }
}
