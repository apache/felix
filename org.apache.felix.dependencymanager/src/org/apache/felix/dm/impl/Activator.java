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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * DependencyManager Activator. We are using this activator in order to track and use a threadpool, which can be 
 * optionally registered by any management agent bundle.
 * The management agent can just register a <code>java.util.Executor</code> service in the service registry
 * using the "target=org.apache.felix.dependencymanager" property. 
 *    
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager mgr) throws Exception {
        mgr.add(createComponent()
               .setImplementation(ComponentScheduler.instance())
               .add(createServiceDependency()
                   .setService(Executor.class, "(target=" + DependencyManager.THREADPOOL + ")")
                   .setRequired(true)
                   .setCallbacks("bind", "unbind")));
    }
}
