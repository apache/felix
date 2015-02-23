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
//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX3008_FilterIndexStartupTest extends TestBase {
    public void testNormalStart() throws Exception {
        System.setProperty("org.apache.felix.dependencymanager.filterindex", "objectClass");
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a provider
        Provider provider = new Provider();
        // activate it
        Component p = m.createComponent()
            .setInterface(Service.class.getName(), null)
            .setImplementation(provider);
        
        Consumer consumer = new Consumer(e);
        Component c = m.createComponent()
            .setImplementation(consumer)
            .add(m.createServiceDependency()
                .setService(Service.class)
                .setRequired(true)
                );
        
        m.add(p);
        m.add(c);
        e.waitForStep(1, 5000);
        m.remove(p);
        e.waitForStep(2, 5000);
        m.remove(c);
        
        Assert.assertEquals("Dependency manager bundle should be active.", Bundle.ACTIVE, context.getBundle().getState());
    }

    public static class Consumer {
        volatile Service m_service;
        private final Ensure m_ensure;
        
        public Consumer(Ensure e) {
            m_ensure = e;
        }

        public void start() {
            System.out.println("start");
            m_ensure.step(1);
        }
        
        public void stop() {
            System.out.println("stop");
            m_ensure.step(2);
        }
    }
    
    public static interface Service {
    }
    
    public static class Provider implements Service {
    }
}
