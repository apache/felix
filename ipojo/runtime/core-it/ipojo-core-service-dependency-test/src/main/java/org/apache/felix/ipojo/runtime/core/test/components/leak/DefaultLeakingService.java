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

package org.apache.felix.ipojo.runtime.core.test.components.leak;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.runtime.core.test.services.HelloService;
import org.apache.felix.ipojo.runtime.core.test.services.LeakingService;
import org.apache.felix.ipojo.runtime.core.test.services.Listener;

/**
 * A component reproducing the leak (FELIX-4247 Memory leak with ServiceUsage and inner class (Listener style)).
 */
@Component
@Provides
@Instantiate
public class DefaultLeakingService implements LeakingService {

    @Requires(optional = true)
    HelloService service;

    private Listener listener;

    public DefaultLeakingService() {
        listener = new Listener() {
            // When the Hello Service will become unavailable, calling doSomething should use a nullable object.
            // Therefore, it should return 'null'.
            @Override
            public String doSomething() {
                return service.hello("iPOJO");
            }
        };
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    @Override
    public String executeListener() {
        return listener.doSomething();
    }
}
