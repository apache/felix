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
package org.apache.felix.dm.impl;

import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * DependencyManager Activator. We are using this activator in order to use a threadpool, which can be 
 * optionally provided  by any management agent bundle.
 * The management agent can just register a <code>java.util.Executor</code> service in the osgi registry
 * using the "target=org.apache.felix.dependencymanager" system property.
 * 
 * There are two ways to ensure that all DM components are handled in parallel using the threadpool:
 * 
 * 1- the management agent bundle can simply be started before any bundles, using the start-level service.
 * 2- if the start-level service can't be used, then you can configure the org.apache.felix.dependendencymanager.parallel
 *    system property, in order to ask DM to wait for the threadpool, before creating any DM components.
 *    
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        boolean waitForThreadPool = Boolean.valueOf(context.getProperty(DependencyManager.PARALLEL));
        Component c = createComponent().setImplementation(ComponentScheduler.instance());
        
        if (waitForThreadPool) {
            c.add(createTemporalServiceDependency(10000)
                .setService(Executor.class, "(target=" + DependencyManager.THREADPOOL + ")")
                .setRequired(true)
                .setAutoConfig("m_threadPool"));
        } else {
            c.add(createServiceDependency()
                .setService(Executor.class, "(target=" + DependencyManager.THREADPOOL + ")")
                .setRequired(false)
                .setAutoConfig("m_threadPool")
                .setDefaultImplementation(ComponentScheduler.NullExecutor.class));
        }
        manager.add(c);
    }
}
