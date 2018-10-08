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
package org.apache.felix.dm.runtime.itest.components;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.LifecycleController;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.Unregistered;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.ServiceRegistration;

/**
 * Check if a lifecycle controller runnable method works when called synchronously from init method.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class FELIX5956 {
	
    public final static String ENSURE = "FELIX5956";
    
    public interface MyService {
        public void deactivate();
    }

    @Component
    public static class MyServiceImpl implements MyService {
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;

        @LifecycleController
        volatile Runnable m_start;
        
        @LifecycleController(start=false)
        volatile Runnable m_stop;

        @Init
        protected void init() {
        	m_ensure.step(1);
        	m_start.run();
        	m_ensure.step(2);
        }

        @Start
        protected void start() {
            m_ensure.step(3);
        }

        public void deactivate() {
        	m_stop.run();
        }
        
        @Stop
        protected void stop() {
        	m_ensure.step(5);
        }
    }

    /**
     * Consumes a service which is provided by the {@link MyServiceImpl} class.
     */
    @Component
    public static class Consumer {        
        @ServiceDependency
        volatile MyService m_myService;
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;


        @Start
        protected void start() {
            if (m_myService != null) {
            	m_ensure.step(4);
            }
            m_myService.deactivate();
        }
    }
}
