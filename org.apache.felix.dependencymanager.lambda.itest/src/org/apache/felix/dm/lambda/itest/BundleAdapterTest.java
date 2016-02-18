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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.bundleAdapter;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleAdapterTest extends TestBase {    
    public void testBundleAdapter() {
        DependencyManager m = getDM();
        // create a bundle adapter service (one is created for each bundle)
        Component adapter = bundleAdapter(m)
            .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
            .impl(BundleAdapter.class)
            .provides(BundleAdapter.class)
            .build();

        // create a service provider and consumer
        Consumer c = new Consumer();
        Component consumer = m.createComponent().setImplementation(c)
            .add(m.createServiceDependency().setService(BundleAdapter.class).setCallbacks("add", "remove"));
        
        // add the bundle adapter
        m.add(adapter);
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        // remove the bundle adapter
        m.remove(adapter);
    }
    
    public void testBundleAdapterWithCallbackInstance() {
        DependencyManager m = getDM();
        // create a bundle adapter service (one is created for each bundle)
        BundleAdapterWithCallback baWithCb = new BundleAdapterWithCallback();
        BundleAdapterCallbackInstance cbInstance = new BundleAdapterCallbackInstance(baWithCb);
        
        Component adapter = bundleAdapter(m)
            .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
            .callbackInstance(cbInstance)
            .add("add").remove("remove")
            .impl(baWithCb)
            .provides(BundleAdapter.class.getName())
            .build();

        // create a service provider and consumer
        Consumer c = new Consumer();
        Component consumer = component(m)
            .impl(c)
            .withSvc(BundleAdapter.class, s->s.add("add").remove("remove"))
            .build();
        
        // add the bundle adapter
        m.add(adapter);
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        // remove the bundle adapter
        m.remove(adapter);
    }
        
    public void testBundleAdapterWithCallbackInstanceRef() {
        DependencyManager m = getDM();
        // create a bundle adapter service (one is created for each bundle)
        BundleAdapterWithCallback baWithCb = new BundleAdapterWithCallback();
        BundleAdapterCallbackInstance cbInstance = new BundleAdapterCallbackInstance(baWithCb);
        
        Component adapter = bundleAdapter(m)
            .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
            .add(cbInstance::addRef).remove(cbInstance::removeRef)
            .impl(baWithCb)
            .provides(BundleAdapter.class.getName())
            .build();

        // create a service provider and consumer
        Consumer c = new Consumer();
        Component consumer = component(m)
            .impl(c)
            .withSvc(BundleAdapter.class, s->s.add("add").remove("remove"))
            .build();
        
        // add the bundle adapter
        m.add(adapter);
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        // remove the bundle adapter
        m.remove(adapter);
    }
        
    public static class BundleAdapter {
        volatile Bundle m_bundle;
        
        Bundle getBundle() {
            return m_bundle;
        }
    }
    
    public static class BundleAdapterWithCallback extends BundleAdapter {
        void add(Bundle b) {
        	m_bundle = b;        	
        }
        
        void remove(Bundle b) {
        	m_bundle = null;
        }
    }
    
    public static class BundleAdapterCallbackInstance {
    	final BundleAdapterWithCallback m_ba;
    	
    	BundleAdapterCallbackInstance(BundleAdapterWithCallback ba) {
    		m_ba = ba;
    	}
    	
        void add(Component c, Bundle b) { // reflection callback
        	m_ba.add(b);	
        }
         
        void addRef(Bundle b, Component c) { // method reference callback
            add(c, b);  
        }

        void remove(Component c, Bundle b) { // reflection callback
        	m_ba.remove(b);
        }
        
        void removeRef(Bundle b, Component c) { // method reference callback
            remove (c, b);
        }
    }
    
    static class Consumer {
        private volatile int m_count = 0;

        public void add(BundleAdapter ba) {
            Bundle b = ba.getBundle();
            System.out.println("Consumer.add(" + b.getSymbolicName() + ")");
            Assert.assertNotNull("bundle instance must not be null", b);
            m_count++;
        }
        
        public void check() {
            Assert.assertTrue("we should have found at least one bundle", m_count > 0);
        }
        
        public void remove(BundleAdapter ba) {
            Bundle b = ba.getBundle();
            System.out.println("Consumer.remove(" + b.getSymbolicName() + ")");
            m_count--;
        }
        
        public void doubleCheck() {
            Assert.assertEquals("all bundles we found should have been removed again", 0, m_count);
        }
    }
}
