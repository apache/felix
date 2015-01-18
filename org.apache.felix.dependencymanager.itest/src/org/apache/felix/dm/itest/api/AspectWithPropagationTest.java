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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.ServiceUtil;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Test for aspects with service properties propagations.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class AspectWithPropagationTest extends TestBase {
    private final static int ASPECTS = 3;
    private final Set<Integer> _randoms = new HashSet<Integer>();
    private final Random _rnd = new Random();
    private static Ensure m_invokeStep;
    private static Ensure m_changeStep;
    
    /**
     * This test does the following:
     * 
     * - Create S service with property "p=s"
     * - Create SA (aspect of S) with property "p=aspect"
     * - Create Client, depending on S (actually, on SA).
     * - Client should see SA with properties p=aspect
     * - Change S service property with "p=smodified": the Client should be changed with SA(p=aspect)
     * - Change aspect service property with "p=aspectmodified": The client should be changed with SA(p=aspectmodified)
     */
    public void testAspectsWithPropagationNotOverriding() {
        System.out.println("----------- Running testAspectsWithPropagationNotOverriding ...");
        DependencyManager m = getDM();
        m_invokeStep = new Ensure(); 
        
        // Create our original "S" service.
        S s = new S() {
			public void invoke() {
			}
        };
		Dictionary props = new Hashtable();
		props.put("p", "s");
		Component sComp = m.createComponent()
                .setImplementation(s)
                .setInterface(S.class.getName(), props);

        // Create SA (aspect of S)
        S sa = new S() {
        	volatile S m_s;
			public void invoke() {
			}
        };
        Component saComp = m.createAspectService(S.class, null, 1).setImplementation(sa);        		
        props = new Hashtable();
        props.put("p", "aspect");
        saComp.setServiceProperties(props);
        
        // Create client depending on S
        Object client = new Object() {
        	int m_changeCount;
        	void add(Map props, S s) {
        		Assert.assertEquals("aspect", props.get("p"));
        		m_invokeStep.step(1);
        	}
        	
            void change(Map props, S s) {
        		switch (++m_changeCount) {
        		case 1:
        			Assert.assertEquals("aspect", props.get("p"));
            		m_invokeStep.step(2);
            		break;
        		case 2:
        			Assert.assertEquals("aspectmodified", props.get("p"));
            		m_invokeStep.step(3);
        		}
        	}
        };
        Component clientComp = m.createComponent()
                .add(m.createServiceDependency()
                     .setService(S.class)
                     .setRequired(true)
                     .setCallbacks("add", "change", null))
                .setImplementation(client);
        
        // Add components in dependency manager
        m.add(sComp);
        m.add(saComp);
        m.add(clientComp);
        
        // client should have been added with SA aspect
        m_invokeStep.waitForStep(1, 5000);
        
        // now change s "p=s" to "p=smodified": client should not see it
        props = new Hashtable();
        props.put("p", "smodified");
        sComp.setServiceProperties(props);
        m_invokeStep.waitForStep(2, 5000);
        
        // now change sa aspect "p=aspect" to "p=aspectmodified": client should see it
        props = new Hashtable();
        props.put("p", "aspectmodified");
        saComp.setServiceProperties(props);
        m_invokeStep.waitForStep(3, 5000);    
        
        // remove components
        m.remove(clientComp);
        m.remove(saComp);
        m.remove(sComp);
    }
        
    /**
     * This test does the following:
     * 
     * - Create S service
     * - Create some S Aspects
     * - Create a Client, depending on S (actually, on the top-level S aspect)
     * - Client has a "change" callback in order to track S service properties modifications.
     * - First, invoke Client.invoke(): all S aspects, and finally original S service must be invoked orderly.
     * - Modify S original service properties, and check if all aspects, and the client has been orderly called in their "change" callback.
     * - Modify the First lowest ranked aspect (rank=1), and check if all aspects, and client have been orderly called in their "change" callback.
     */
    public void testAspectsWithPropagation() {
        System.out.println("----------- Running testAspectsWithPropagation ...");
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        m_invokeStep = new Ensure(); 
        
        // Create our original "S" service.
        Dictionary props = new Hashtable();
        props.put("foo", "bar");
        Component s = m.createComponent()
                .setImplementation(new SImpl())
                .setInterface(S.class.getName(), props);
        
        // Create an aspect aware client, depending on "S" service.
        Client clientImpl;
        Component client = m.createComponent()
                .setImplementation((clientImpl = new Client()))
                .add(m.createServiceDependency()
                     .setService(S.class)
                     .setRequired(true)
                     .setCallbacks("add", "change", "remove", "swap"));

        // Create some "S" aspects
        Component[] aspects = new Component[ASPECTS];
        for (int rank = 1; rank <= ASPECTS; rank ++) {
            aspects[rank-1] = m.createAspectService(S.class, null, rank, "add", "change", "remove", "swap")
                    .setImplementation(new A("A" + rank, rank));
            props = new Hashtable();
            props.put("a" + rank, "v" + rank);
            aspects[rank-1].setServiceProperties(props);
        }                                    
              
        // Register client
        m.add(client);
        
        // Randomly register aspects and original service
        boolean originalServiceAdded = false;
        for (int i = 0; i < ASPECTS; i ++) {
            int index = getRandomAspect();
            m.add(aspects[index]);
            if (! originalServiceAdded && _rnd.nextBoolean()) {
                m.add(s);
                originalServiceAdded = true;
            }
        }
        if (! originalServiceAdded) {
            m.add(s);
        }
              
        // All set, check if client has inherited from top level aspect properties + original service properties
        Map check = new HashMap();
        check.put("foo", "bar");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS);
        checkServiceProperties(check, clientImpl.getServiceProperties());

        // Now invoke client, which orderly calls all aspects in the chain, and finally the original service "S".
        System.out.println("-------------------------- Invoking client.");
        clientImpl.invoke();
        m_invokeStep.waitForStep(ASPECTS+1, 5000);
        
        // Now, change original service "S" properties: this will orderly trigger "change" callbacks on aspects, and on client. 
        System.out.println("-------------------------- Modifying original service properties.");
        m_changeStep = new Ensure();
        props = new Hashtable();
        props.put("foo", "barModified");
        s.setServiceProperties(props);
        
        // Check if aspects and client have been orderly called in their "changed" callback
        m_changeStep.waitForStep(ASPECTS+1, 5000);
        
        // Check if modified "foo" original service property has been propagated
        check = new HashMap();
        check.put("foo", "barModified");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS); // we only see top-level aspect service properties
        checkServiceProperties(check, clientImpl.getServiceProperties());
        
        // Now, change the top-level ranked aspect: it must propagate to all upper aspects, as well as to the client
        System.out.println("-------------------------- Modifying top-level aspect service properties.");

        m_changeStep = new Ensure();
        for (int i = 1; i <= ASPECTS; i ++) {
            m_changeStep.step(i); // only client has to be changed.
        }
        props = new Hashtable();
        props.put("a" + ASPECTS, "v" + ASPECTS + "-Modified");
        aspects[ASPECTS-1].setServiceProperties(props); // That triggers change callbacks for upper aspects (with rank >= 2)
        m_changeStep.waitForStep(ASPECTS+1, 5000); // check if client have been changed.
        
        // Check if top level aspect service properties have been propagated up to the client.
        check = new HashMap();
        check.put("foo", "barModified");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS + "-Modified");
        checkServiceProperties(check, clientImpl.getServiceProperties());

        // Clear all components.
        m_changeStep = null;
        m.clear();
    }    
    
    /**
     * This test does the following:
     * 
     * - Create S service
     * - Create some S Aspects without any callbacks (add/change/remove/swap)
     * - Create a Client, depending on S (actually, on the top-level S aspect)
     * - Client has a "change" callack in order to track S service properties modifications.
     * - First, invoke Client.invoke(): all S aspects, and finally original S service must be invoked orderly.
     * - Modify S original service properties, and check if the client has been called in its "change" callback.
     */
    public void testAspectsWithPropagationAndNoCallbacks() {
        System.out.println("----------- Running testAspectsWithPropagation ...");
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        m_invokeStep = new Ensure(); 
        
        // Create our original "S" service.
        Dictionary props = new Hashtable();
        props.put("foo", "bar");
        Component s = m.createComponent()
                .setImplementation(new SImpl())
                .setInterface(S.class.getName(), props);
        
        // Create an aspect aware client, depending on "S" service.
        Client clientImpl;
        Component client = m.createComponent()
                .setImplementation((clientImpl = new Client()))
                .add(m.createServiceDependency()
                     .setService(S.class)
                     .setRequired(true)
                     .setCallbacks("add", "change", "remove"));

        // Create some "S" aspects
        Component[] aspects = new Component[ASPECTS];
        for (int rank = 1; rank <= ASPECTS; rank ++) {
            aspects[rank-1] = m.createAspectService(S.class, null, rank)
                    .setImplementation(new A("A" + rank, rank));
            props = new Hashtable();
            props.put("a" + rank, "v" + rank);
            aspects[rank-1].setServiceProperties(props);
        }                                    
              
        // Register client
        m.add(client);
        
        // Randomly register aspects and original service
        boolean originalServiceAdded = false;
        for (int i = 0; i < ASPECTS; i ++) {
            int index = getRandomAspect();
            m.add(aspects[index]);
            if (! originalServiceAdded && _rnd.nextBoolean()) {
                m.add(s);
                originalServiceAdded = true;
            }
        }
        if (! originalServiceAdded) {
            m.add(s);
        }
              
        // All set, check if client has inherited from top level aspect properties + original service properties
        Map check = new HashMap();
        check.put("foo", "bar");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS);
        checkServiceProperties(check, clientImpl.getServiceProperties());

        // Now invoke client, which orderly calls all aspects in the chain, and finally the original service "S".
        System.out.println("-------------------------- Invoking client.");
        clientImpl.invoke();
        m_invokeStep.waitForStep(ASPECTS+1, 5000);
        
        // Now, change original service "S" properties: this will orderly trigger "change" callbacks on aspects, and on client. 
        System.out.println("-------------------------- Modifying original service properties.");
        m_changeStep = new Ensure();
        for (int i = 1; i <= ASPECTS; i ++) {
            m_changeStep.step(i); // skip aspects, which have no "change" callbacks.
        }
        props = new Hashtable();
        props.put("foo", "barModified");
        s.setServiceProperties(props);
        
        // Check if aspects and client have been orderly called in their "changed" callback
        m_changeStep.waitForStep(ASPECTS+1, 5000);
        
        // Check if modified "foo" original service property has been propagated
        check = new HashMap();
        check.put("foo", "barModified");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS); // we only see top-level aspect service properties
        checkServiceProperties(check, clientImpl.getServiceProperties());
        
        // Clear all components.
        m_changeStep = null;
        m.clear();
    }    
    
    /**
     * This test does the following:
     * 
     * - Create S service
     * - Create some S Aspects
     * - Create S2 Adapter, which adapts S to S2
     * - Create Client2, which depends on S2. Client2 listens to S2 property change events.
     * - Now, invoke Client2.invoke(): all S aspects, and finally original S service must be invoked orderly.
     * - Modify S original service properties, and check if all aspects, S2 Adapter, and Client2 have been orderly called in their "change" callback.
     */
    public void testAdapterWithAspectsAndPropagation() {
        System.out.println("----------- Running testAdapterWithAspectsAndPropagation ...");

        DependencyManager m = getDM();
        m_invokeStep = new Ensure(); 
        
        // Create our original "S" service.
        Dictionary props = new Hashtable();
        props.put("foo", "bar");
        Component s = m.createComponent()
                .setImplementation(new SImpl())
                .setInterface(S.class.getName(), props);
        
        // Create some "S" aspects
        Component[] aspects = new Component[ASPECTS];
        for (int rank = 1; rank <= ASPECTS; rank ++) {
            aspects[rank-1] = m.createAspectService(S.class, null, rank, "add", "change", "remove", "swap")
                    .setImplementation(new A("A" + rank, rank));
            props = new Hashtable();
            props.put("a" + rank, "v" + rank);
            aspects[rank-1].setServiceProperties(props);
        } 
        
        // Create S2 adapter (which adapts S1 to S2 interface)
        Component adapter = m.createAdapterService(S.class, null, "add", "change", "remove", "swap")
                .setInterface(S2.class.getName(), null)
                .setImplementation(new S2Impl());
        
        // Create Client2, which depends on "S2" service.
        Client2 client2Impl;
        Component client2 = m.createComponent()
                .setImplementation((client2Impl = new Client2()))
                .add(m.createServiceDependency()
                     .setService(S2.class)
                     .setRequired(true)
                     .setCallbacks("add", "change", null));
              
        // Register client2
        m.add(client2);
        
        // Register S2 adapter
        m.add(adapter);
        
        // Randomly register aspects, original service
        boolean originalServiceAdded = false;
        for (int i = 0; i < ASPECTS; i ++) {
            int index = getRandomAspect();
            m.add(aspects[index]);
            if (! originalServiceAdded && _rnd.nextBoolean()) {
                m.add(s);
                originalServiceAdded = true;
            }
        }
        if (! originalServiceAdded) {
            m.add(s);
        }
             
        // Now invoke client2, which orderly calls all S1 aspects, then S1Impl, and finally S2 service
        System.out.println("-------------------------- Invoking client2.");
        client2Impl.invoke2();
        m_invokeStep.waitForStep(ASPECTS+2, 5000);
        
        // Now, change original service "S" properties: this will orderly trigger "change" callbacks on aspects, S2Impl, and Client2.
        System.out.println("-------------------------- Modifying original service properties.");
        m_changeStep = new Ensure();
        props = new Hashtable();
        props.put("foo", "barModified");
        s.setServiceProperties(props);
        
        // Check if aspects and Client2 have been orderly called in their "changed" callback
        m_changeStep.waitForStep(ASPECTS+2, 5000);
        
        // Check if modified "foo" original service property has been propagated to Client2
        Map check = new HashMap();
        check.put("foo", "barModified");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS);
        checkServiceProperties(check, client2Impl.getServiceProperties());
        
        // Clear all components.
        m_changeStep = null;
        m.clear();
    }    
    
    /**
     * This test does the following:
     * 
     * - Create S service
     * - Create some S Aspects without any callbacks (add/change/remove)
     * - Create S2 Adapter, which adapts S to S2 (but does not have any add/change/remove callbacks)
     * - Create Client2, which depends on S2. Client2 listens to S2 property change events.
     * - Now, invoke Client2.invoke(): all S aspects, and finally original S service must be invoked orderly.
     * - Modify S original service properties, and check if all aspects, S2 Adapter, and Client2 have been orderly called in their "change" callback.
     */
    public void testAdapterWithAspectsAndPropagationNoCallbacks() {
        System.out.println("----------- Running testAdapterWithAspectsAndPropagationNoCallbacks ...");

        DependencyManager m = getDM();
        m_invokeStep = new Ensure(); 
        
        // Create our original "S" service.
        Dictionary props = new Hashtable();
        props.put("foo", "bar");
        Component s = m.createComponent()
                .setImplementation(new SImpl())
                .setInterface(S.class.getName(), props);
        
        // Create some "S" aspects
        Component[] aspects = new Component[ASPECTS];
        for (int rank = 1; rank <= ASPECTS; rank ++) {
            aspects[rank-1] = m.createAspectService(S.class, null, rank)
                    .setImplementation(new A("A" + rank, rank));
            props = new Hashtable();
            props.put("a" + rank, "v" + rank);
            aspects[rank-1].setServiceProperties(props);
        } 
        
        // Create S2 adapter (which adapts S1 to S2 interface)
        Component adapter = m.createAdapterService(S.class, null)
                .setInterface(S2.class.getName(), null)
                .setImplementation(new S2Impl());
        
        // Create Client2, which depends on "S2" service.
        Client2 client2Impl;
        Component client2 = m.createComponent()
                .setImplementation((client2Impl = new Client2()))
                .add(m.createServiceDependency()
                     .setService(S2.class)
                     .setRequired(true)
                     .setCallbacks("add", "change", null));
              
        // Register client2
        m.add(client2);
        
        // Register S2 adapter
        m.add(adapter);
        
        // Randomly register aspects, original service
        boolean originalServiceAdded = false;
        for (int i = 0; i < ASPECTS; i ++) {
            int index = getRandomAspect();
            m.add(aspects[index]);
            if (! originalServiceAdded && _rnd.nextBoolean()) {
                m.add(s);
                originalServiceAdded = true;
            }
        }
        if (! originalServiceAdded) {
            m.add(s);
        }
             
        // Now invoke client2, which orderly calls all S1 aspects, then S1Impl, and finally S2 service
        System.out.println("-------------------------- Invoking client2.");
        client2Impl.invoke2();
        m_invokeStep.waitForStep(ASPECTS+2, 5000);
        
        // Now, change original service "S" properties: this will orderly trigger "change" callbacks on aspects, S2Impl, and Client2.
        System.out.println("-------------------------- Modifying original service properties.");
        m_changeStep = new Ensure();
        for (int i = 1; i <= ASPECTS+1; i ++) {
            m_changeStep.step(i); // skip all aspects and the adapter
        }
        props = new Hashtable();
        props.put("foo", "barModified");
        s.setServiceProperties(props);
        
        // Check if Client2 has been called in its "changed" callback
        m_changeStep.waitForStep(ASPECTS+2, 5000);
        
        // Check if modified "foo" original service property has been propagated to Client2
        Map check = new HashMap();
        check.put("foo", "barModified");
        for (int i = 1; i < (ASPECTS - 1); i ++) {
            check.put("a" + i, null); // we must not inherit from lower ranks, only from the top-level aspect.
        }
        check.put("a" + ASPECTS, "v" + ASPECTS);
        checkServiceProperties(check, client2Impl.getServiceProperties());
        
        // Clear all components.
        m_changeStep = null;
        m.clear();
    }    
    
   private void checkServiceProperties(Map<?, ?> check, Dictionary properties) {
        for (Object key : check.keySet()) {
            Object val = check.get(key);   
            if (val == null) {
                Assert.assertNull(properties.get(key));
            } else {
                Assert.assertEquals(val, properties.get(key));
            }
        }
    }
    
    private int getRandomAspect() {
        int index = 0;  
        do {
            index = _rnd.nextInt(ASPECTS);            
        } while (_randoms.contains(new Integer(index)));
        _randoms.add(new Integer(index));
        return index;
    }

    // S Service
    public static interface S {
        public void invoke();
    }
    
    // S ServiceImpl
    static class SImpl implements S {
        public SImpl() {
        }
        
        public String toString() {
            return "S";
        }
        
        public void invoke() {
             m_invokeStep.step(ASPECTS+1);
        }
    }
    
    // S Aspect
    static class A implements S {
        private final String m_name;
        private volatile ServiceRegistration m_registration;
        private volatile S m_next;
        private final int m_rank;

        public A(String name, int rank) {
            m_name = name;
            m_rank = rank;
        }
        
        public String toString() {
            return m_name;
        }
        
        public void invoke() {
            int rank = ServiceUtil.getRanking(m_registration.getReference());
            m_invokeStep.step(ASPECTS - rank + 1);
            m_next.invoke();
        }
               
        public void add(ServiceReference ref, S s) {
            System.out.println("+++ A" + m_rank + ".add:" + s + "/" + ServiceUtil.toString(ref));   
            m_next = s;
        }

        public void swap(ServiceReference oldSRef, S oldS, ServiceReference newSRef, S newS) {
            System.out.println("+++ A" + m_rank + ".swap: new=" + newS + ", props=" + ServiceUtil.toString(newSRef));
            Assert.assertTrue(m_next == oldS);
            m_next = newS;
        }

        public void change(ServiceReference props, S s) {   
            System.out.println("+++ A" + m_rank + ".change: s=" + s + ", props=" + ServiceUtil.toString(props));
            if (m_changeStep != null) {
                int rank = ServiceUtil.getRanking(m_registration.getReference());
                m_changeStep.step(rank);
            }
        }
        
        public void remove(ServiceReference props, S s) {
            System.out.println("+++ A" + m_rank + ".remove: " + s + ", props=" + ServiceUtil.toString(props));
        }
    }
    
    // Aspect aware client, depending of "S" service aspects.
    static class Client {
        private volatile S m_s;
        private volatile ServiceReference m_sRef;

        public Client() {
        }
        
        public Dictionary getServiceProperties() {
            Dictionary props = new Hashtable();
            for (String key : m_sRef.getPropertyKeys()) {
                props.put(key, m_sRef.getProperty(key));
            }
            return props;
        }

        public void invoke() {
            m_s.invoke();           
        }

        public String toString() {
            return "Client";
        }
        
        public void add(ServiceReference ref, S s) {
            System.out.println("+++ Client.add: " + s + "/" + ServiceUtil.toString(ref));
            m_s = s;
            m_sRef = ref;
        }
              
        public void swap(ServiceReference oldSRef, S oldS, ServiceReference newSRef, S newS) {
            System.out.println("+++ Client.swap: m_s = " + m_s + ", old=" + oldS + ", oldProps=" + ServiceUtil.toString(oldSRef) + ", new=" + newS + ", props=" + ServiceUtil.toString(newSRef));
            Assert.assertTrue(m_s == oldS);
            m_s = newS;
            m_sRef = newSRef;
        }

        public void change(ServiceReference properties, S s) {
            System.out.println("+++ Client.change: s=" + s + ", props=" + ServiceUtil.toString(properties));
            if (m_changeStep != null) {
                m_changeStep.step(ASPECTS+1);
            }
        }
        
        public void remove(ServiceReference props, S s) {
            System.out.println("+++ Client.remove: " + s + ", props=" + ServiceUtil.toString(props));
        }
    }
    
    // S2 Service
    public static interface S2 {
        public void invoke2();
    }

    // S2 impl, which adapts S1 interface to S2 interface
    static class S2Impl implements S2 {
        private volatile S m_s; // we shall see top-level aspect on S service
        
        public void add(ServiceReference ref, S s) {
            System.out.println("+++ S2Impl.add: " + s + "/" + ServiceUtil.toString(ref));
            m_s = s;
        }
              
        public void swap(ServiceReference oldSRef, S oldS, ServiceReference newSRef, S newS) {
            System.out.println("+++ S2Impl.swap: new=" + newS + ", props=" + ServiceUtil.toString(newSRef));
            m_s = newS;
        }

        public void change(ServiceReference properties, S s) {
            System.out.println("+++ S2Impl.change: s=" + s + ", props=" + ServiceUtil.toString(properties));
            if (m_changeStep != null) {
                m_changeStep.step(ASPECTS+1);
            }
        }
        
        public void remove(ServiceReference props, S s) {
            System.out.println("+++ S2Impl.remove: " + s + ", props=" + ServiceUtil.toString(props));
        }
        
        public void invoke2() {
            m_s.invoke();
            m_invokeStep.step(ASPECTS + 2); // All aspects, and S1Impl have been invoked
        }

        public String toString() {
            return "S2";
        }        
    }
    
    // Client2 depending on S2.
    static class Client2 {
        private volatile S2 m_s2;
        private volatile ServiceReference m_s2Ref;

        public Dictionary getServiceProperties() {
            Dictionary props = new Hashtable();
            for (String key : m_s2Ref.getPropertyKeys()) {
                props.put(key, m_s2Ref.getProperty(key));
            }
            return props;
        }

        public void invoke2() {
            m_s2.invoke2();           
        }

        public String toString() {
            return "Client2";
        }  
                
        public void add(ServiceReference ref, S2 s2) {
            System.out.println("+++ Client2.add: " + s2 + "/" + ServiceUtil.toString(ref));
            m_s2 = s2;
            m_s2Ref = ref;
        }

        public void change(ServiceReference props, S2 s2) {   
            System.out.println("+++ Client2.change: s2=" + s2 + ", props=" + ServiceUtil.toString(props));
            if (m_changeStep != null) {
                m_changeStep.step(ASPECTS + 2); // S1Impl, all aspects, and S2 adapters have been changed before us.
            }
        }
    }
}
