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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Felix4050 {
    public final static String ENSURE = "Felix4050";
    
    @Component(provides = {A.class})
    public static class A {

    }

    public interface B {
        void run();
    }

    @Component(properties = {@Property(name = "type", value = "b1")})
    public static class B1 implements B {
        public void run() {
        }
    }

    @Component(provides = {})
    public static class B2 implements B {
        @Inject
        volatile BundleContext _ctx;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @Start
        void start() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    System.out.println("Registering B2");
                    Properties props = new Properties();
                    props.put("type", "b2");
                    _ctx.registerService(B.class.getName(), B2.this, props);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            });
            t.start();
        }

        public void run() {
            m_sequencer.step(3);
        }
    }

    @Component
    public static class S {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @Inject
        volatile org.apache.felix.dm.Component _component;

        volatile A m_a;
        volatile B m_b;

        void bind(A a) {
            System.out.println("bind(A): " + a);
            m_a = a;
        }

        @ServiceDependency(name = "B")
        void bind(B b) {
            System.out.println("bind(B): " + b);
            m_b = b;
        }

        @Init
        Map<?,?> init() {
            m_sequencer.step(1);
            _component.add(_component.getDependencyManager().createServiceDependency()
                .setService(A.class).setRequired(true)
                .setCallbacks("bind", null));
            Map<String, String> props = new HashMap<>();
            props.put("B.required", "true");
            props.put("B.filter", "(type=b2)");
            return props;
        }

        @Start
        void start() {
            if (m_a == null) {
                throw new RuntimeException("A not injected");
            }
            if (m_b == null) {
                throw new RuntimeException("B not injected");
            }
            m_sequencer.step(2);
            m_b.run(); // step(3)
        }

        @Stop
        void stop() {
            m_sequencer.step(4);
        }

        @Destroy
        void destroy() {
            m_sequencer.step(5);
        }
    }
}
