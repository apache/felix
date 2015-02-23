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
import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ComponentTest extends TestBase {
    private final Ensure m_ensure = new Ensure();

    public void testSimple() throws Exception {
        final DependencyManager dm = getDM();
        Component consumer = dm.createComponent();
        consumer
            .setImplementation(new Consumer())
            .add(dm.createServiceDependency()
                .setService(Provider.class, "(name=provider2)")
                .setRequired(true)
                .setCallbacks("add", "remove"))
            .add(dm.createServiceDependency()
                .setService(Provider.class, "(name=provider1)")
                .setRequired(true)
                .setAutoConfig("m_autoConfiguredProvider"));
        
        Dictionary props = new Hashtable();
        props.put("name", "provider1");
        Component provider1 = dm.createComponent()
        		.setImplementation(new Provider() { public String toString() { return "provider1";}})
        		.setInterface(Provider.class.getName(), props);
        props = new Hashtable();
        props.put("name", "provider2");
        Component provider2 = dm.createComponent()
				   .setImplementation(new Provider() { public String toString() { return "provider2";}})
				   .setInterface(Provider.class.getName(), props);
        dm.add(provider1);
        dm.add(provider2);
        dm.add(consumer);
        m_ensure.waitForStep(2, 5000);
        dm.remove(provider1); 
        dm.remove(provider2);    
        m_ensure.waitForStep(5, 5000);
        dm.clear();
    }
    
    public static interface Provider {    	
    }
    
    public class Consumer {
        Provider m_provider;
        Provider m_autoConfiguredProvider;
        
        void add(Map props, Provider provider) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("provider2", props.get("name"));
            m_provider = provider;
            m_ensure.step(1);
        }
        
        void start() {
            Assert.assertNotNull(m_autoConfiguredProvider);
            Assert.assertEquals("provider1", m_autoConfiguredProvider.toString());
            m_ensure.step(2);
        }
        
        void stop() {
            m_ensure.step(3);
        }
        
        void destroy() {
            m_ensure.step(4);
        }
        
        void remove(Provider provider) {
            Assert.assertEquals(m_provider, provider);
            m_ensure.step(5);
        }
    }
}
