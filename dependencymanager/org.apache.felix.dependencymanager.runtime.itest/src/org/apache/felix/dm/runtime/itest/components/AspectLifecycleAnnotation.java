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

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectLifecycleAnnotation {
    public interface ServiceInterface {
        public void run();
    }

    /**
     * Tests an aspect service, and ensure that its lifecycle methods are properly invoked (init/start/stop/destroy)
     */
    @Component
    public static class AspectLifecycleTest {
        @ServiceDependency
        void bind(ServiceInterface service) {
            service.run();
        }
    }

    @Component
    public static class ServiceProvider implements ServiceInterface {
        public final static String ENSURE = "AspectLifecycleAnnotation.ServiceProvider";
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        public void run() {
            m_sequencer.step();
        }
    }

    @AspectService(ranking = 10)
    public static class ServiceProviderAspect implements ServiceInterface {
        public final static String ENSURE = "AspectLifecycleAnnotation.ServiceProviderAspect";

        protected volatile boolean m_initCalled;
        protected volatile Ensure m_sequencer;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected void bind(Ensure sequencer) {
            m_sequencer = sequencer;
            m_sequencer.step(2);
        }

        @Init
        void init() {
            m_initCalled = true;
        }

        @Start
        void start() {
            if (!m_initCalled) {
                throw new IllegalStateException("start method called, but init method was not called");
            }
            m_sequencer.step(3);
        }

        public void run() {
            m_sequencer.step(4);
        }

        @Stop()
        void stop() {
            // At this point, the AspectLifecycleTest class has been rebound to the original ServiceProvider.
            m_sequencer.step(6);
        }

        @Destroy
        void destroy() {
            m_sequencer.step(7);
        }
    }
}
