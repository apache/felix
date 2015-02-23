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

import java.util.Hashtable;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MultipleExtraDependenciesTest extends TestBase {
    /**
     * Check that list of extra dependencies (defined from init method) are handled properly.
     * The extra dependencies are added using a List object (Component.add(List)).
     * A component c1 will define two extra dependencies over *available* c4/c5 services.
     */
     public void testWithTwoAvailableExtraDependency() {   
         DependencyManager m = getDM();
         // Helper class that ensures certain steps get executed in sequence
         Ensure e = new Ensure();
         Component c1 = m.createComponent()
                         .setInterface(Service1.class.getName(), null)
                         .setImplementation(new MyComponent1(e))
                         .add(m.createServiceDependency()
                              .setService(Service2.class)
                              .setRequired(true)
                              .setAutoConfig("m_service2"));
         
         Component c2 = m.createComponent()
                         .setImplementation(new MyComponent2(e))
                         .add(m.createServiceDependency()
                              .setService(Service1.class)
                              .setRequired(false)
                              .setAutoConfig(false)
                              .setCallbacks("added", null, null));
              
         Component c3 = m.createComponent()
                         .setInterface(Service2.class.getName(), null)
                         .setImplementation(Service2Impl.class);
         
         Hashtable h = new Hashtable();
         h.put("type", "xx");
         Component c4 = m.createComponent()
                         .setInterface(Service3.class.getName(), h)
                         .setImplementation(Service3Impl1.class);

         h = new Hashtable();
         h.put("type", "yy");
         Component c5 = m.createComponent()
                         .setInterface(Service3.class.getName(), h)
                         .setImplementation(Service3Impl2.class);


         System.out.println("\n+++ Adding c2 / MyComponent2");
         m.add(c2);
         System.out.println("\n+++ Adding c3 / Service2");
         m.add(c3);
         System.out.println("\n+++ Adding c4 / Service3(xx)");
         m.add(c4);
         System.out.println("\n+++ Adding c5 / Service3(yy)");
         m.add(c5);
         System.out.println("\n+++ Adding c1 / MyComponent1");
         // c1 have declared two extra dependency on Service3 (xx/yy).
         // both extra dependencies are available, so the c1 component should be started immediately.
         m.add(c1);
         e.waitForStep(3, 3000);
         m.clear();
     }

    /**
     * Check that list of extra dependencies (defined from init method) are handled properly.
     * The extra dependencies are added using a List object (Component.add(List)).
     * A component c1 will define two extra dependencies over c4/c5. At the point c1.init()
     * is adding the two extra dependencies from its init method, c4 is available, but not c5.
     * So, c1 is not yet activated.
     * Then c5 is added, and it triggers the c1 activation ...
     */
    public void testWithOneAvailableExtraDependency() {  
        DependencyManager m = getDM();
        // Helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component c1 = m.createComponent()
                        .setInterface(Service1.class.getName(), null)
                        .setImplementation(new MyComponent1(e))
                        .add(m.createServiceDependency()
                             .setService(Service2.class)
                             .setRequired(true)
                             .setAutoConfig("m_service2"));
        
        Component c2 = m.createComponent()
                        .setImplementation(new MyComponent2(e))
                        .add(m.createServiceDependency()
                             .setService(Service1.class)
                             .setRequired(false)
                             .setAutoConfig(false)
                             .setCallbacks("added", null, null));
             
        Component c3 = m.createComponent()
                        .setInterface(Service2.class.getName(), null)
                        .setImplementation(Service2Impl.class);
        
        Hashtable h = new Hashtable();
        h.put("type", "xx");
        Component c4 = m.createComponent()
                        .setInterface(Service3.class.getName(), h)
                        .setImplementation(Service3Impl1.class);

        h = new Hashtable();
        h.put("type", "yy");
        Component c5 = m.createComponent()
                        .setInterface(Service3.class.getName(), h)
                        .setImplementation(Service3Impl2.class);


        System.out.println("\n+++ Adding c2 / MyComponent2");
        m.add(c2);
        System.out.println("\n+++ Adding c3 / Service2");
        m.add(c3);
        System.out.println("\n+++ Adding c4 / Service3(xx)");
        m.add(c4);
        System.out.println("\n+++ Adding c1 / MyComponent1");
        m.add(c1);

        // c1 have declared two extra dependency on Service3 (xx/yy).
        // So, because we have not yet added c5 (yy), c1 should not be started currently.
        // But, now, we'll add c5 (Service3/yy) and c1 should then be started ...
        System.out.println("\n+++ Adding c5 / Service3(yy)");
        m.add(c5);
        e.waitForStep(3, 3000);
        m.clear();
    }


    public interface Service1 {}
    public interface Service2 {}
    public interface Service3 {}

    public static class Service2Impl implements Service2 {}
    public static class Service3Impl1 implements Service3 {}
    public static class Service3Impl2 implements Service3 {}

    public static class MyComponent1 implements Service1 {
        Service2 m_service2;
        Service3 m_service3_xx;
        Service3 m_service3_yy;
        Ensure m_ensure;
        
        public MyComponent1(Ensure e) {
            m_ensure = e;
        }

        void init(Component c) {
            m_ensure.step(1);
            DependencyManager dm = c.getDependencyManager();
            // Service3/xx currently available
            Dependency d1 = 
            		dm.createServiceDependency()
                     .setService(Service3.class, "(type=xx)")
                     .setRequired(true)
                     .setAutoConfig("m_service3_xx");
            
            // Service3/yy not yet available
            Dependency d2 = 
            	dm.createServiceDependency()
                  .setService(Service3.class, "(type=yy)")
                  .setRequired(true)
                  .setAutoConfig("m_service3_yy");
            c.add(d1, d2);
        }
        
        void start() {
            System.out.println("MyComponent1.start");
            Assert.assertNotNull(m_service2);
            Assert.assertNotNull(m_service3_xx);
            Assert.assertNotNull(m_service3_yy);
            m_ensure.step(2);
        }
    }
    
    public static class MyComponent2 {
        Ensure m_ensure;
        
        public MyComponent2(Ensure e) {
            m_ensure = e;
        }

        void added(Service1 s1) {
            System.out.println("MyComponent2.bind(" + s1 + ")");
            Assert.assertNotNull(s1);
            m_ensure.step(3);
        }
        
        void start() {
            System.out.println("MyComponent2.start");
        }
    }
}
