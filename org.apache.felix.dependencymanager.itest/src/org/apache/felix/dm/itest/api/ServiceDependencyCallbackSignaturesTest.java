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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("unused")
public class ServiceDependencyCallbackSignaturesTest extends TestBase {
    volatile Ensure m_ensure;
    
    /**
     * Tests if all possible dependency callbacks signatures supported by ServiceDependency.
     */
    public void testDependencyCallbackSignatures() {
        DependencyManager m = getDM();
        m_ensure = new Ensure();    
        Hashtable<String, String> props = new Hashtable<>();
        props.put("foo", "bar");
        Component provider = m.createComponent()
            .setImplementation(new ProviderImpl()).setInterface(Provider.class.getName(), props);
        
        declareConsumer(m, new Object() {
            void bind(Component c, ServiceReference ref, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                Assert.assertEquals(context.getService(ref), provider);
                m_ensure.step();
            }
            void change(Component c, ServiceReference ref, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                Assert.assertEquals(context.getService(ref), provider);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c, ServiceReference ref, Object provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                Assert.assertNotNull(provider);
                Assert.assertEquals(context.getService(ref), provider);
                m_ensure.step();
            }
            void change(Component c, ServiceReference ref, Object provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                Assert.assertNotNull(provider);
                Assert.assertEquals(context.getService(ref), provider);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c, ServiceReference ref) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                Assert.assertNotNull(context.getService(ref));
                Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
                m_ensure.step();
            }
            void change(Component c, ServiceReference ref) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(ref);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                Assert.assertNotNull(context.getService(ref));
                Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(provider);
                m_ensure.step();
            }
            void change(Component c, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(provider);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c, Object provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(provider);
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
            void change(Component c, Object provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(provider);
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c) {
                Assert.assertNotNull(c);
                m_ensure.step();
            }
            void change(Component c) {
                Assert.assertNotNull(c);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Component c, Map<String, String> props, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(props);
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", props.get("foo"));
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
            void change(Component c, Map<String, String> props, Provider provider) {
                Assert.assertNotNull(c);
                Assert.assertNotNull(props);
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", props.get("foo"));
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(ServiceReference ref, Provider provider) {
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                m_ensure.step();
            }
            void change(ServiceReference ref, Provider provider) {
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(ServiceReference ref, Object provider) {
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
            void change(ServiceReference ref, Object provider) {
                Assert.assertNotNull(ref);
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(ServiceReference ref) {
                Assert.assertNotNull(ref);
                Assert.assertEquals("bar", ref.getProperty("foo"));
                m_ensure.step();
            }
            void change(ServiceReference ref) {
                Assert.assertNotNull(ref);
                Assert.assertEquals("zoo", ref.getProperty("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Provider provider) {
                Assert.assertNotNull(provider);
                m_ensure.step();
            }
            void change(Provider provider) {
                Assert.assertNotNull(provider);
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Provider provider, Map<?, ?> props) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", props.get("foo"));
                m_ensure.step();
            }
            void change(Provider provider, Map<?, ?> props) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", props.get("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Map<?, ?> props, Provider provider) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", props.get("foo"));
                m_ensure.step();
            }
            void change(Map<?, ?> props, Provider provider) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", props.get("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Provider provider, Dictionary<?, ?> props) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", props.get("foo"));
                m_ensure.step();
            }
            void change(Provider provider, Dictionary<?, ?> props) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", props.get("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Dictionary<?, ?> props, Provider provider) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("bar", props.get("foo"));
                m_ensure.step();
            }
            void change(Dictionary<?, ?> props, Provider provider) {
                Assert.assertNotNull(provider);
                Assert.assertEquals("zoo", props.get("foo"));
                m_ensure.step();
            }
        });
        declareConsumer(m, new Object() {
            void bind(Object provider) {
                Assert.assertNotNull(provider);
                Assert.assertEquals(provider.getClass(), ProviderImpl.class);
                m_ensure.step();
            }
            void change(Object provider) {
                bind(provider);
            }
        });

        m.add(provider);
        m_ensure.waitForStep(16, 5000);
        
        props = new Hashtable<>();
        props.put("foo", "zoo");
        provider.setServiceProperties(props);
        m_ensure.waitForStep(32, 5000);
        
        m.remove(provider);
        m_ensure.waitForStep(48, 5000);
    }
    
    private void declareConsumer(DependencyManager m, Object consumerImpl) {
        Component consumer = m.createComponent().setImplementation(consumerImpl)
            .add(m.createServiceDependency().setService(Provider.class).setCallbacks("bind", "change", "change").setRequired(true));
        m.add(consumer);
    }

    public static interface Provider {        
    }
    
    public static class ProviderImpl implements Provider {        
    }   
}
