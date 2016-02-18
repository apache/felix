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
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
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
        Component provider = component(m)
            .impl(new ProviderImpl()).provides(Provider.class.getName(), props).build();
        
        component(m, c->c.impl(new Consumer1()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer2()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer3()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer4()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer5()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer6()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer7()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer8()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));
        component(m, c->c.impl(new Consumer9()).withSvc(Provider.class, srv->srv.add("bind").change("change").remove("remove")));

        m.add(provider);
        m_ensure.waitForStep(9, 5000);
        
        props = new Hashtable<>();
        props.put("foo", "zoo");
        provider.setServiceProperties(props);
        m_ensure.waitForStep(18, 5000);
        
        m.remove(provider);
        m_ensure.waitForStep(26, 5000);
    }
        
    /**
     * Tests if all possible dependency callbacks signatures supported by ServiceDependency.
     */
    public void testDependencyCallbackSignaturesRef() {
        DependencyManager m = getDM();
        m_ensure = new Ensure();    
        Hashtable<String, String> props = new Hashtable<>();
        props.put("foo", "bar");
        Component provider = component(m)
            .impl(new ProviderImpl()).provides(Provider.class.getName(), props).build();
        
        component(m, c->c.impl(new Consumer1()).withSvc(Provider.class, srv->srv.add(Consumer1::bind).change(Consumer1::change).remove(Consumer1::remove)));
        component(m, c->c.impl(new Consumer2()).withSvc(Provider.class, srv->srv.add(Consumer2::bind).change(Consumer2::change).remove(Consumer2::remove)));
        component(m, c->c.impl(new Consumer3()).withSvc(Provider.class, srv->srv.add(Consumer3::bind).change(Consumer3::change).remove(Consumer3::remove)));
        component(m, c->c.impl(new Consumer4()).withSvc(Provider.class, srv->srv.add(Consumer4::bindRef).change(Consumer4::changeRef).remove(Consumer4::removeRef)));
        component(m, c->c.impl(new Consumer5()).withSvc(Provider.class, srv->srv.add(Consumer5::bindRef).change(Consumer5::changeRef).remove(Consumer5::removeRef)));
        component(m, c->c.impl(new Consumer6()).withSvc(Provider.class, srv->srv.add(Consumer6::bindRef).change(Consumer6::changeRef).remove(Consumer6::removeRef)));
        component(m, c->c.impl(new Consumer7()).withSvc(Provider.class, srv->srv.add(Consumer7::bindRef).change(Consumer7::changeRef).remove(Consumer7::removeRef)));
        component(m, c->c.impl(new Consumer8()).withSvc(Provider.class, srv->srv.add(Consumer8::bindRef).change(Consumer8::changeRef).remove(Consumer8::removeRef)));
        component(m, c->c.impl(new Consumer9()).withSvc(Provider.class, srv->srv.add(Consumer9::bindRef).change(Consumer9::changeRef).remove(Consumer9::removeRef)));

        m.add(provider);
        m_ensure.waitForStep(9, 5000);
        
        props = new Hashtable<>();
        props.put("foo", "zoo");
        provider.setServiceProperties(props);
        m_ensure.waitForStep(18, 5000);
        
        m.remove(provider);
        m_ensure.waitForStep(26, 5000);
    }

    public static interface Provider {        
    }
    
    public static class ProviderImpl implements Provider {        
    }
    
    class Consumer1 {        
        void bind(Provider provider) {
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
        void change(Provider provider) {
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
        
        void remove(Provider provider) {
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
    }
    
    class Consumer2 {        
        void bind(Provider provider, Map<String, Object> props) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("bar", props.get("foo"));
            m_ensure.step();
        }
        void change(Provider provider, Map<String, Object> props) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", props.get("foo"));
            m_ensure.step();
        }
        void remove(Provider provider, Map<String, Object> props) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", props.get("foo"));
            m_ensure.step();
        }
    }
    
    class Consumer3 {        
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
        void remove(Provider provider, Dictionary<?, ?> props) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", props.get("foo"));
            m_ensure.step();
        }
    }

    class Consumer4 {        
        void bindRef(Provider provider, ServiceReference ref) { // method ref callback
            bind(ref, provider);
        }
        
        void bind(ServiceReference ref, Provider provider) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("bar", ref.getProperty("foo"));
            m_ensure.step();
        }
        
        void changeRef(Provider provider, ServiceReference ref) { // method ref callback
            change(ref, provider);
        }
        
        void change(ServiceReference ref, Provider provider) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
        
        void removeRef(Provider provider, ServiceReference ref) { // method ref callback
            remove(ref, provider);
        }
        
        void remove(ServiceReference ref, Provider provider) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
    }
        
    class Consumer5 {        
        void bindRef(Provider provider, ServiceReference ref) { // method ref callback
            bind(ref);
        }
        
        void bind(ServiceReference ref) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertEquals("bar", ref.getProperty("foo"));
            m_ensure.step();
        }
      
        void changeRef(Provider provider, ServiceReference ref) { // method ref callback
            change(ref);
        }
        
        void change(ServiceReference ref) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
        
        void removeRef(Provider provider, ServiceReference ref) { // method ref callback
            remove(ref);
        }

        void remove(ServiceReference ref) { // reflection based callback
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
    }

    class Consumer6 {     
        
        void bindRef(Provider p, Component c) { // method ref callback
            bind(c);
        }
        
        void bind(Component c) { // reflection based callback
            Assert.assertNotNull(c);
            m_ensure.step();
        }
        
        void changeRef(Provider p, Component c) { // method ref callback
            change(c);
        }

        void change(Component c) { // reflection based callback
            Assert.assertNotNull(c);
            m_ensure.step();
        }
        
        void removeRef(Provider p, Component c) { // method ref callback
            remove(c);
        }

        void remove(Component c) { // reflection based callback
            Assert.assertNotNull(c);
            m_ensure.step();
        }
    }
        
    class Consumer7 {
        void bindRef(Provider p, Component c, ServiceReference ref) { // reflection callback
            bind(c, ref);
        }
        
        void bind(Component c, ServiceReference ref) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertEquals("bar", ref.getProperty("foo"));
            Assert.assertNotNull(context.getService(ref));
            Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
            m_ensure.step();
        }
        
        void changeRef(Provider p, Component c, ServiceReference ref) { // reflection callback
            change(c, ref);
        }
        
        void change(Component c, ServiceReference ref) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertNotNull(context.getService(ref));
            Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
            m_ensure.step();
        }
        
        void removeRef(Provider p, Component c, ServiceReference ref) { // reflection callback
            remove(c, ref);
        }
        
        void remove(Component c, ServiceReference ref) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertNotNull(context.getService(ref));
            Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
            m_ensure.step();
        }
    }
        
    class Consumer8 {
        void bindRef(Provider p, Component c) { // method ref callback
            bind(c, p);
        }
        
        void bind(Component c, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
        
        void changeRef(Provider p, Component c) { // method ref callback
            change(c, p);
        }
        
        void change(Component c, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
        
        void removeRef(Provider p, Component c) { // method ref callback
            remove(c, p);
        }
        
        void remove(Component c, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
    }
    
    class Consumer9 {
        void bindRef(Provider provider, Component c, ServiceReference ref) { // method ref callback
            bind(c, ref, provider);
        }
        
        void bind(Component c, ServiceReference ref, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("bar", ref.getProperty("foo"));
            Assert.assertEquals(context.getService(ref), provider);
            m_ensure.step();
        }
        
        void changeRef(Provider provider, Component c, ServiceReference ref) { // method ref callback
            change(c, ref, provider);
        }
        
        void change(Component c, ServiceReference ref, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertEquals(context.getService(ref), provider);
            m_ensure.step();
        }
        
        void removeRef(Provider provider, Component c, ServiceReference ref) { // method ref callback
            remove(c, ref, provider);
        }
        
        void remove(Component c, ServiceReference ref, Provider provider) { // reflection callback
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertEquals(context.getService(ref), provider);
            m_ensure.step();
        }
    }
        
}
