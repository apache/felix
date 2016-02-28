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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.util.Dictionary;

import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentTest extends TestBase {
    private final Ensure m_ensure = new Ensure();
    
    public void testSimple() throws Exception {
        final DependencyManager dm = getDM();

        // Create consumer (dependency is required by default using builder api).
        component(dm, comp -> comp
        		.factory(Consumer::new)
        		.withSvc(Provider.class, srv -> srv.filter("(name=provider2)").add(Consumer::add).remove(Consumer::remove))
        		.withSvc(Provider.class, srv -> srv.filter("(name=provider1)").autoConfig("m_autoConfiguredProvider")));
                
        // Create providers (auto added to dependency manager)
        component(dm, comp -> comp
        		.impl(new Provider() { public String toString() { return "provider1";}})
        		.provides(Provider.class).properties("name", "provider1"));
        		
        component(dm, comp -> comp
        		.impl(new Provider() { public String toString() { return "provider2";}})
        		.provides(Provider.class).properties("name", "provider2"));
        		
        m_ensure.waitForStep(2, 5000);
        dm.clear();
        m_ensure.waitForStep(5, 5000);
    }
    
    public void testSimple2() throws Exception {
        final DependencyManager dm = getDM();

        // Create consumer (dependency is required by default using builder api).
        component(dm, comp -> comp
                .factory(Consumer::new)
                .withSvc(Provider.class, srv -> srv.filter("(name=provider2)").add("add").remove("remove"))
                .withSvc(Provider.class, "(name=provider1)", "m_autoConfiguredProvider", true));
                
        // Create providers (auto added to dependency manager)
        component(dm, comp -> comp
                .impl(new Provider() { public String toString() { return "provider1";}})
                .provides(Provider.class).properties("name", "provider1"));
                
        component(dm, comp -> comp
                .impl(new Provider() { public String toString() { return "provider2";}})
                .provides(Provider.class).properties("name", "provider2"));
                
        m_ensure.waitForStep(2, 5000);
        dm.clear();
        m_ensure.waitForStep(5, 5000);
    }

    
    public static interface Provider {    	
    }
    
    public class Consumer {
        Provider m_provider;
        Provider m_autoConfiguredProvider;
                
		void add(Provider provider, Dictionary<String, Object> props) {
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
        
        void remove(Provider provider, Dictionary<String, Object> props) {
            Assert.assertEquals(m_provider, provider);
            m_ensure.step(5);
        }
    }
}
