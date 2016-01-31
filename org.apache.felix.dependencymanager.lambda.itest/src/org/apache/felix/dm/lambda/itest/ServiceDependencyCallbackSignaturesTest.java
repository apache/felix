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
        
        component(m, c->c.impl(new Consumer1()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer2()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer3()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer4()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer5()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer6()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer7()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer8()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));
        component(m, c->c.impl(new Consumer9()).withSrv(Provider.class, srv->srv.cb("bind", "change", "remove")));

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
        
        component(m, c->c.impl(new Consumer1()).withSrv(Provider.class, srv->srv.cb(Consumer1::bind, Consumer1::change, Consumer1::remove)));
        component(m, c->c.impl(new Consumer2()).withSrv(Provider.class, srv->srv.cb(Consumer2::bind, Consumer2::change, Consumer2::remove)));
        component(m, c->c.impl(new Consumer3()).withSrv(Provider.class, srv->srv.cb(Consumer3::bind, Consumer3::change, Consumer3::remove)));
        component(m, c->c.impl(new Consumer4()).withSrv(Provider.class, srv->srv.cb(Consumer4::bind, Consumer4::change, Consumer4::remove)));
        component(m, c->c.impl(new Consumer5()).withSrv(Provider.class, srv->srv.cb(Consumer5::bind, Consumer5::change, Consumer5::remove)));
        component(m, c->c.impl(new Consumer6()).withSrv(Provider.class, srv->srv.cb(Consumer6::bind, Consumer6::change, Consumer6::remove)));
        component(m, c->c.impl(new Consumer7()).withSrv(Provider.class, srv->srv.cb(Consumer7::bind, Consumer7::change, Consumer7::remove)));
        component(m, c->c.impl(new Consumer8()).withSrv(Provider.class, srv->srv.cb(Consumer8::bind, Consumer8::change, Consumer8::remove)));
        component(m, c->c.impl(new Consumer9()).withSrv(Provider.class, srv->srv.cb(Consumer9::bind, Consumer9::change, Consumer9::remove)));

        m.add(provider);
        m_ensure.waitForStep(9, 5000);
        
        props = new Hashtable<>();
        props.put("foo", "zoo");
        provider.setServiceProperties(props);
        m_ensure.waitForStep(18, 5000);
        
        m.remove(provider);
        m_ensure.waitForStep(26, 5000);
    }
    
    private void declareConsumer(DependencyManager m, Object consumerImpl) {
        Component consumer = component(m)
            .impl(consumerImpl)
            .withSrv(Provider.class, srv->srv.cb("bind", "change", "change"))
            .build();
        m.add(consumer);
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
        void remove(ServiceReference ref, Provider provider) {
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
    }
        
    class Consumer5 {        
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
        void remove(ServiceReference ref) {
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            m_ensure.step();
        }
    }

    class Consumer6 {        
        void bind(Component c) {
            Assert.assertNotNull(c);
            m_ensure.step();
        }
        void change(Component c) {
            Assert.assertNotNull(c);
            m_ensure.step();
        }
        void remove(Component c) {
            Assert.assertNotNull(c);
            m_ensure.step();
        }
    }
        
    class Consumer7 {
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
        void remove(Component c, ServiceReference ref) {
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertNotNull(context.getService(ref));
            Assert.assertEquals(context.getService(ref).getClass(), ProviderImpl.class);
            m_ensure.step();
        }
    }
        
    class Consumer8 {
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
        void remove(Component c, Provider provider) {
            Assert.assertNotNull(c);
            Assert.assertNotNull(provider);
            m_ensure.step();
        }
    }
    
    class Consumer9 {
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
        void remove(Component c, ServiceReference ref, Provider provider) {
            Assert.assertNotNull(c);
            Assert.assertNotNull(ref);
            Assert.assertNotNull(provider);
            Assert.assertEquals("zoo", ref.getProperty("foo"));
            Assert.assertEquals(context.getService(ref), provider);
            m_ensure.step();
        }
    }
        
}
