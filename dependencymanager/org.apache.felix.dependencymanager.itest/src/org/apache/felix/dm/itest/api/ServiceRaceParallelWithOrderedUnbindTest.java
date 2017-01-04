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
package org.apache.felix.dm.itest.api;

/**
 * This test class simulates a client having many dependencies being registered concurrently using DM concurrent mode.
 * (that is: a ComponentExecutorFactory is registered in the osgi registry and the configured threadpool is then used
 * to schedule component creations).
 * Once the services are created, injected, and fully started, then they are unregistered from a single thread (like it is the case when the osgi 
 * framework is stopped where bundle are stopped synchronously).
 * So, when unbind methods are called, we verify that unbound services are still started at the time unbind callbacks are invoked.
 * 
 * NOTICE: when using DM and a ComponentExecutorFactory, component removal is done synchronously if possible. 
 * So if you remove components after all components have been fully started, then the component removal are done synchronously.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceRaceParallelWithOrderedUnbindTest extends ServiceRaceWithOrderedUnbindTest {
    public ServiceRaceParallelWithOrderedUnbindTest() {
        setParallel(); // Configure DM to use a threadpool
    }
}
