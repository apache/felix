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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ComponentFactoryAnnotation {
    public final static String FACTORY = "ComponentFactoryAnnotation.Factory";
    public final static String ENSURE = "ComponentFactoryAnnotation.Ensure";

    public interface MyServiceInterface {
        public void added(String instanceId);

        public void changed(String modified);

        public void removed();
    }

    @Component(properties = @Property(name = "foo", value = "bar"))
    public static class ExtraDependency1 implements Runnable {
        public void run() {
        }
    }

    @Component(properties = @Property(name = "foo", value = "bar2"))
    public static class ExtraDependency2 implements Runnable {
        public void run() {
            System.out.println("ExtraDependency2.run()");
        }
    }

    /**
     * This service is instantiated using a "factory set" from the
     * ServiceFactoryAnnotationTest class.
     * 
     * @see org.apache.felix.dm.test.annotation.ServiceFactoryAnnotationTest
     */
    @Component(factoryName = FACTORY, factoryConfigure = "configure", properties = {@Property(name = "foo", value = "bar")})
    public static class MyService implements MyServiceInterface {
        /**
         * The configuration provided by MyServiceFactory
         */
        volatile Dictionary m_configuration;

        /**
         * Our sequencer.
         */
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        /**
         * An extra dependency (we'll dynamically configure the filter from our
         * init() method).
         */
        @ServiceDependency(name = "extra")
        Runnable m_extra;

        /**
         * This is the first method called: we are provided with the
         * MyServiceFactory configuration.
         */
        public void configure(Dictionary<?, ?> configuration) {
            if (m_configuration == null) {
                m_configuration = configuration;
            } else {
                m_sequencer.step(5);
                m_configuration = configuration;
            }
        }

        /**
         * Initialize our Service: we'll dynamically configure our dependency whose
         * name is "extra".
         */
        @SuppressWarnings("serial")
        @Init
        Map init() {
            return new HashMap() {
                {
                    put("extra.filter", "(foo=bar2)");
                    put("extra.required", "true");
                }
            };
        }

        /**
         * our Service is starting: at this point, all required dependencies have
         * been injected.
         */
        @Start
        public void start() {
            Assert.assertNotNull("Extra dependency not injected", m_extra);
            m_extra.run();
            m_sequencer.step(2);
        }

        /**
         * Our service is stopping.
         */
        @Stop
        public void stop() {
            m_sequencer.step(10);
        }

        public void added(String instanceId) {
            if (instanceId.equals(m_configuration.get("instance.id"))) {
                m_sequencer.step(4);
            }
        }

        public void changed(String modified) {
            if (modified.equals(m_configuration.get("instance.modified"))) {
                m_sequencer.step(7);
            }
        }

        public void removed() {
            m_sequencer.step(9);
        }
    }

    @Component
    public static class MyServiceClient {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @Start
        void start() {
            m_sequencer.step(1);
        }
        
        @ServiceDependency(required = false, changed = "update", removed = "removed")
        void bind(Map serviceProperties, MyServiceInterface service) {
            m_sequencer.step(3);
            Assert.assertEquals("bar", serviceProperties.get("foo"));
            Assert.assertNull(serviceProperties.get(".private.param"));
            service.added((String) serviceProperties.get("instance.id"));
        }

        void update(Map serviceProperties, MyServiceInterface service) {
            m_sequencer.step(6);
            service.changed((String) serviceProperties.get("instance.modified"));
        }

        void removed(MyServiceInterface service) {
            m_sequencer.step(8);
            service.removed();
        }
    }
}
