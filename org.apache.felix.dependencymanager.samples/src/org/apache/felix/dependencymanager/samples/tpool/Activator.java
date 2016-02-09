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
package org.apache.felix.dependencymanager.samples.tpool;

import java.util.Properties;

import org.apache.felix.dependencymanager.samples.tpool.executor.ComponentExecutorFactoryImpl;
import org.apache.felix.dm.ComponentExecutorFactory;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * See README file describing this Activator.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {  
    @Override
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        mgr.add(createComponent()
           .setInterface(ComponentExecutorFactory.class.getName(), null)
           .setImplementation(ComponentExecutorFactoryImpl.class));
        
        // Create two synchronous components
        mgr.add(createComponent().setImplementation(new MyComponent("Component 1")));
        mgr.add(createComponent().setImplementation(new MyComponent("Component 2")));

        // And two components which will be managed and started concurrently.
        Properties properties = new Properties();
        properties.put("parallel", "true");

        mgr.add(createComponent().setImplementation(new MyComponent("Parallel Component 3")).setServiceProperties(properties));
        mgr.add(createComponent().setImplementation(new MyComponent("Parallel Component 4")).setServiceProperties(properties));
    }
}
