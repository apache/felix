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
package org.apache.felix.dependencymanager.test2.integration.api;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyActivation;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.DependencyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class CustomDependencyTest extends TestBase {
    @Test
    public void testCustomDependency() {
        Ensure e = new Ensure();
        DependencyManager dm = new DependencyManager(context);
        
        // create a toggle that can be used to turn on/off our custom dependency
        Toggle toggle = new Toggle();
        
        // create a service that has our custom dependency as its only dependency
        dm.add(dm.createComponent()
            .setImplementation(new ServiceImpl(e))
            .add(new CustomDependency(toggle))
            );
        
        // make the toggle, therefore the dependency, therefore the service available
        toggle.setAvailable(true);
        e.waitForStep(1, 1000);
        
        // make the toggle unavailable again
        toggle.setAvailable(false);
        e.waitForStep(2, 1000);
    }
    
    /** A toggle implementation that invokes a callback on every change. */
    public static class Toggle {
        private boolean m_isAvailable;
        private Runnable m_runnable;
        
        public boolean isAvailable() {
            return m_isAvailable;
        }
        public void setAvailable(boolean isAvailable) {
            boolean changed = m_isAvailable != isAvailable;
            m_isAvailable = isAvailable;
            Runnable r = m_runnable;
            if (r != null && changed) {
                r.run();
            }
        }
        public void setRunnable(Runnable runnable) {
            m_runnable = runnable;
        }
    }
    
    /** Our custom dependency, which is less configurable than most, but that's okay for this test. */
    public static class CustomDependency implements Dependency, DependencyActivation, Runnable {
        private final Toggle m_toggle;
        private final List m_services = new ArrayList();

        public CustomDependency(Toggle toggle) {
            m_toggle = toggle;
        }
        
        public Dependency createCopy() {
            return new CustomDependency(m_toggle);
        }

        public Object getAutoConfigInstance() {
            return "" + m_toggle.isAvailable();
        }

        public String getAutoConfigName() {
            return null;
        }

        public Class getAutoConfigType() {
            return String.class;
        }

        public Dictionary getProperties() {
            return null;
        }
        
        public void run() {
            // invoked on every change
            if (m_toggle.isAvailable()) {
                Object[] services = m_services.toArray();
                for (int i = 0; i < services.length; i++) {
                    DependencyService ds = (DependencyService) services[i];
                    ds.dependencyAvailable(this);
                    if (!isRequired()) {
                        invokeAdded(ds);
                    }
                }
            }
            else {
                Object[] services = m_services.toArray();
                for (int i = 0; i < services.length; i++) {
                    DependencyService ds = (DependencyService) services[i];
                    ds.dependencyUnavailable(this);
                    if (!isRequired()) {
                        invokeRemoved(ds);
                    }
                }
            }
        }

        public void invokeAdded(DependencyService service) {
            invoke(service, "added");
        }

        public void invokeRemoved(DependencyService service) {
            invoke(service, "removed");
        }
        
        public void invoke(DependencyService dependencyService, String name) {
            if (name != null) {
                dependencyService.invokeCallbackMethod(getCallbackInstances(dependencyService), name,
                  new Class[][] {{String.class}, {Object.class}, {}},
                  new Object[][] {{getAutoConfigInstance()}, {getAutoConfigInstance()}, {}}
                );
            }
        }
        
        private synchronized Object[] getCallbackInstances(DependencyService dependencyService) {
            return dependencyService.getCompositionInstances();
        }

        public boolean isAutoConfig() {
            return true;
        }

        public boolean isAvailable() {
            return m_toggle.isAvailable();
        }

        public boolean isInstanceBound() {
            return false;
        }

        public boolean isPropagated() {
            return false;
        }

        public boolean isRequired() {
            return true;
        }

        public void start(DependencyService service) {
            synchronized (this) {
                m_services.add(service);
            }
            m_toggle.setRunnable(this);
        }

        public void stop(DependencyService service) {
            synchronized (this) {
                m_services.remove(service);
            }
            m_toggle.setRunnable(null);
        }
    }
    
    public static class ServiceImpl {
        private final Ensure m_e;
        public ServiceImpl(Ensure e) {
            m_e = e;
        }
        public void init() {
            System.out.println("init");
            m_e.step(1);
        }
        public void destroy() {
            System.out.println("destroy");
            m_e.step(2);
        }
    }
}
