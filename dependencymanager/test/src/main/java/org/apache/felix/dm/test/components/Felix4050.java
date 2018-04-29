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
package org.apache.felix.dm.test.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

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
                    ServiceRegistration sr = _ctx.registerService(B.class.getName(), B2.this, new Hashtable() {
                        {
                            put("type", "b2");
                        }
                    });

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
        Map init() {
            m_sequencer.step(1);

            List l = new ArrayList();
            l.add(_component.getDependencyManager().createServiceDependency().setService(A.class).setRequired(true)
                    .setCallbacks("bind", null).setInstanceBound(true));
            _component.add(l);

            return new HashMap() {
                {
                    put("B.required", "true");
                    put("B.filter", "(type=b2)");
                }
            };
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
