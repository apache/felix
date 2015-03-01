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
import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MultipleAnnotations {
    public final static String ENSURE = "MultipleAnnotations";
    
    public static class Composite {
        void bind(Ensure seq) {
            seq.step(2);
        }
    }

    @Component
    public static class ServiceConsumer {
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @ServiceDependency(filter = "(foo=bar)")
        volatile ServiceInterface m_service;

        @Start
        void start() {
            m_sequencer.step(6);
            m_service.doService();
        }

        @Stop
        void stop() {
            m_sequencer.step(8);
        }
    }

    public interface ServiceInterface {
        public void doService();
    }

    @Component(properties = {@Property(name = "foo", value = "bar")})
    public static class ServiceProvider implements ServiceInterface {
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        volatile ServiceProvider2 m_serviceProvider2;

        @ServiceDependency(removed = "unbind")
        void bind(ServiceProvider2 provider2) {
            m_serviceProvider2 = provider2;
        }

        @Start
        void start() {
            m_serviceProvider2.step(4);
            m_sequencer.step(5);
        }

        @Stop
        void stop() {
            m_sequencer.step(9);
        }

        void unbind(ServiceProvider2 provider2) {
            m_sequencer.step(10);
        }

        public void doService() {
            m_sequencer.step(7);
        }
    }

    @Component(provides = {ServiceProvider2.class}, factoryMethod = "create")
    public static class ServiceProvider2 {
        final Composite m_composite = new Composite();
        volatile Ensure m_sequencer;

        static ServiceProvider2 create() {
            return new ServiceProvider2();
        }

        @ServiceDependency(required = false, filter = "(foo=bar)") // NullObject
        volatile Runnable m_runnable;

        @ServiceDependency(service = Ensure.class, filter="(name=" + ENSURE + ")")
        void bind(Ensure seq) {
            m_sequencer = seq;
            m_sequencer.step(1);
        }

        @Start
        void start() {
            m_sequencer.step(3);
            m_runnable.run(); // NullObject
        }

        public void step(int step) { // called by ServiceProvider.start() method
            m_sequencer.step(step);
        }

        @Stop
        void stop() {
            m_sequencer.step(11);
        }

        @Composition
        Object[] getComposition() {
            return new Object[]{this, m_composite};
        }
    }
}
