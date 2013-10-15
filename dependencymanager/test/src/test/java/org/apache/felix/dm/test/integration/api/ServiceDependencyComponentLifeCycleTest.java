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
package org.apache.felix.dm.test.integration.api;
//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(PaxExam.class)
public class ServiceDependencyComponentLifeCycleTest extends TestBase {
    @Test
    public void testComponentLifeCycleWhenAddingAndRemovingDependencies() throws Exception {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a resource provider
        
        Component component = m.createComponent().setInterface(MyService2.class.getName(), null).setImplementation(new MyComponent(e));
        ServiceDependency dependency = m.createServiceDependency().setService(MyService.class).setRequired(true);
        ServiceDependency dependency2 = m.createServiceDependency().setService(MyService.class).setRequired(true);
        ServiceTracker st = new ServiceTracker(context, MyService2.class.getName(), null);
        st.open();
        Component component2 = m.createComponent().setInterface(MyService.class.getName(), null).setImplementation(new MyImpl(e));
        
        // add the component: it has no dependencies so it should be activated immediately
        m.add(component);
        Assert.assertNotNull("service should be available", st.getService());
                
        // add a required dependency that is not available, so the component should be deactivated
        component.add(dependency);
        Assert.assertNull("service should no longer be available", st.getService());
        // remove the dependency again, so the component should be activated again
        component.remove(dependency);
        Assert.assertNotNull("service should be available", st.getService());
        // make the dependency instance bound
        dependency.setInstanceBound(true);
        
        // add it again, the component was already active so even though the dependency
        // is required, the component will *NOT* go through the destroy life cycle methods
        component.add(dependency);
        Assert.assertNull("service should no longer be available", st.getService());
        component.remove(dependency);
        Assert.assertNotNull("service should be available", st.getService());
        
        // make the second dependency instance bound too
        dependency2.setInstanceBound(true);
        
        // activate the service we depend on
        m.add(component2);
        // init and start should be invoked here, so wait for them to complete
        e.waitForStep(10, 5000);
        
        component.add(dependency);
        Assert.assertNotNull("service should be available", st.getService());
        component.add(dependency2);
        Assert.assertNotNull("service should be available", st.getService());
        component.remove(dependency);
        Assert.assertNotNull("service should be available", st.getService());
        
        e.step(11);
        
        // remove the service again, the component still has an instance bound
        // dependency on it, so stop() should be invoked, but the component
        // should not be destroyed
        m.remove(component2);
        e.step(15);
        Assert.assertNull("service should no longer be available", st.getService());
        component.remove(dependency2);
        Assert.assertNotNull("service should be available", st.getService());
        m.remove(component);
        e.step(19);
    }
    
    public static class MyComponent implements MyService2 {
        private final Ensure m_ensure;
        private final Ensure.Steps m_initSteps = new Ensure.Steps(1, 5);
        private final Ensure.Steps m_startSteps = new Ensure.Steps(2, 6, 8, 16);
        private final Ensure.Steps m_stopSteps = new Ensure.Steps(3, 7, 12, 17);
        private final Ensure.Steps m_destroySteps = new Ensure.Steps(4, 18);

        public MyComponent(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            System.out.println("init");
            m_ensure.steps(m_initSteps);
        }
        
        public void start() {
            System.out.println("start");
            m_ensure.steps(m_startSteps);
        }
        
        public void stop() {
            System.out.println("stop");
            m_ensure.steps(m_stopSteps);
        }

        public void destroy() {
            System.out.println("destroy");
            m_ensure.steps(m_destroySteps);
        }
    }
    
    public static interface MyService2 {
    }
    
    public static interface MyService {
    }
    
    public static class MyImpl implements MyService {
        private final Ensure m_ensure;

        public MyImpl(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(9);
        }
        public void start() {
            m_ensure.step(10);
        }
        public void stop() {
            m_ensure.step(13);
        }
        public void destroy() {
            m_ensure.step(14);
        }
    }
}
