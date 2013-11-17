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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceUtil;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Test for aspects with service properties propagations.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
public class AspectWithPropagationTest extends TestBase {
    private final static int ASPECTS = 2;
    private final Set<Integer> _randoms = new HashSet<Integer>();
    private final Random _rnd = new Random();
    private static Ensure m_invokeStep;
    private static Ensure m_changeStep;
    
    @Test
    public void testAspectsWithPropagation() {
        DependencyManager m = new DependencyManager(context);
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
                     .setDebug("client")
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
            if (_rnd.nextBoolean()) {
                m.add(s);
                originalServiceAdded = true;
            }
        }
        if (! originalServiceAdded) {
            m.add(s);
        }
              
        // All set, check if client has inherited from all aspect properties + original service properties
        Dictionary check = new Hashtable();
        check.put("foo", "bar");
        for (int i = 1; i <= ASPECTS; i ++) {
            check.put("a" + i, "v" + i);
        }            
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
        check = new Hashtable();
        check.put("foo", "barModified");
        for (int i = 1; i <= ASPECTS; i ++) {
            check.put("a" + i, "v" + i);
        }            
        checkServiceProperties(check, clientImpl.getServiceProperties());
        
        // Now, change the lower ranked aspect: it must propagate to all upper aspects, as well as to the client
        System.out.println("-------------------------- Modifying First aspect service properties.");

        m_changeStep = new Ensure();
        m_changeStep.step(1); // skip the first lower-ranked aspect, only upper aspects (rank >= 2) will be changed.
        props = new Hashtable();
        props.put("a1", "v1Modified");
        aspects[0].setServiceProperties(props); // That triggers change callbacks for upper aspects (with rank >= 2)
        m_changeStep.waitForStep(ASPECTS+1, 5000); // check if Aspects with rank > 1 and the clients have been changed.
        
        // Check if first aspect service properties have been propagated up to the client.
        check = new Hashtable();
        check.put("foo", "barModified");
        check.put("a1", "v1Modified");
        for (int i = 2; i <= ASPECTS; i ++) {
            check.put("a" + i, "v" + i);
        }        
        checkServiceProperties(check, clientImpl.getServiceProperties());

        // Clear all components.
        m.clear();
    }    
    
    private void checkServiceProperties(Dictionary check, Dictionary properties) {
        Enumeration e = check.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object val = check.get(key);                     
            Assert.assertEquals(val, properties.get(key));
        }        
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
            System.out.println("+++ Client.swap: new=" + newS + ", props=" + ServiceUtil.toString(newSRef));
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
    
    private int getRandomAspect() {
        int index = 0;  
        do {
            index = _rnd.nextInt(ASPECTS);            
        } while (_randoms.contains(new Integer(index)));
        _randoms.add(new Integer(index));
        return index;
    }
}
