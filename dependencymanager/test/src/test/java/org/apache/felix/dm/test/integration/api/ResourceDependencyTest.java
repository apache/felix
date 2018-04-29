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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class ResourceDependencyTest extends TestBase {
    @Test
    public void testResourceDependency() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ResourceConsumer c = new ResourceConsumer(e);
        Component consumer = m.createComponent()
            .setImplementation(c)
            .add(m.createResourceDependency()
                .setFilter("(&(path=/path/to/*.txt)(host=localhost))")
                .setCallbacks("add", "change", "remove"));
        Component dynamicProxyConsumer = m.createComponent()
            .setFactory(new ResourceConsumerFactory(e), "create")
            .add(m.createResourceDependency()
                    .setFilter("(path=*.doc)")
                    .setCallbacks("add", null)); 
        ResourceProvider provider = new ResourceProvider(e);
        Component resourceProvider = m.createComponent()
            .setImplementation(provider)
            .add(m.createServiceDependency()
                .setService(ResourceHandler.class)
                .setCallbacks("add", "remove"));
        
        // first add the consumer
        m.add(consumer);
        // then the resource provider, which will provide 3 resources,
        // 2 of which match the consumers filter conditions
        m.add(resourceProvider);
        // make sure our consumer invoked openStream() on both resources,
        // increasing the step counter to 2
        e.step(3);
        
        // now add another consumer, that matches only one resource, and uses
        // a dynamic proxy as its implementation
        m.add(dynamicProxyConsumer);
        // ensure the resource was injected properly
        e.waitForStep(4, 5000);
        
        // now change a resource and see if it gets propagated to the consumer
        provider.changeResource();
        
        // wait for change callback
        e.waitForStep(5, 5000);
        e.step(6);
        
        // cleanup
        m.remove(dynamicProxyConsumer);
        m.remove(resourceProvider);
        m.remove(consumer);
        
        // validate that all consumed resources are "unconsumed" again
        c.ensure();
    }
    
    static class ResourceConsumer {
        private volatile int m_counter;
        private Ensure m_ensure;
        
        public ResourceConsumer(Ensure ensure) {
            m_ensure = ensure;
        }
        
        public void add(URL resource) {
            m_counter++;
            m_ensure.step();
        }
        public void change(URL resource) {
            m_ensure.step();
        }
        public void remove(URL resource) {
            m_counter--;
        }
        public void ensure() {
            Assert.assertTrue("all resources should have been added and removed at this point, but " + m_counter + " are remaining", m_counter == 0);
        }
    }
    
    static class ResourceProvider {
        private volatile BundleContext m_context;
        private final Ensure m_ensure;
        private final Map<ResourceHandler, Filter> m_handlers = new HashMap<ResourceHandler, Filter>();
        private URL[] m_resources;
        
        public ResourceProvider(Ensure ensure) {
            m_ensure = ensure;
            try {
                m_resources = new URL[] {
                    new URL("file://localhost/path/to/file1.txt"),
                    new URL("file://localhost/path/to/file2.txt"),
                    new URL("file://localhost/path/to/file3.doc")
                };
            }
            catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        
        public void add(ServiceReference ref, ResourceHandler handler) {
            String filterString = (String) ref.getProperty("filter");
            Filter filter;
            try {
                filter = m_context.createFilter(filterString);
            }
            catch (InvalidSyntaxException e) {
                Assert.fail("Could not create filter for resource handler: " + e);
                return;
            }
            synchronized (m_handlers) {
                m_handlers.put(handler, filter);
            }
            for (int i = 0; i < m_resources.length; i++) {
                if (filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.added(m_resources[i]);
                }
            }
        }

        public void changeResource() {
            Filter filter;
            for (Entry<ResourceHandler, Filter> entry : m_handlers.entrySet()) {
                for (int i = 0; i < m_resources.length; i++) {
                    if (i == 0) {
                        if (entry.getValue().match(ResourceUtil.createProperties(m_resources[i]))) {
                            entry.getKey().changed(m_resources[i]); 
                        }
                    }
                }
            }
        }
        
        public void remove(ServiceReference ref, ResourceHandler handler) {
            Filter filter;
            synchronized (m_handlers) {
                filter = (Filter) m_handlers.remove(handler);
            }
            removeResources(handler, filter);
        }

        private void removeResources(ResourceHandler handler, Filter filter) {
            for (int i = 0; i < m_resources.length; i++) {
                if (filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.removed(m_resources[i]);
                }
            }
        }

        public void destroy() {
            Entry[] handlers;
            synchronized (m_handlers) {
                handlers = (Entry[]) m_handlers.entrySet().toArray(new Entry[m_handlers.size()]);
            }
            for (int i = 0; i < handlers.length; i++) {
                removeResources((ResourceHandler) handlers[i].getKey(), (Filter) handlers[i].getValue());
            }
        }
    }
    
    static class ResourceConsumerFactory {
        private final Ensure m_ensure;
        public ResourceConsumerFactory(Ensure ensure) {
            m_ensure = ensure;
        }
        public Object create() {
            ResourceConsumer resourceConsumer = new ResourceConsumer(m_ensure);
            // create a dynamic proxy for the ResourceProvider
            return Proxy.newProxyInstance(resourceConsumer.getClass().getClassLoader(), resourceConsumer.getClass().getInterfaces(), new DynamicProxyHandler(resourceConsumer, m_ensure));
        }
    }

    static class DynamicProxyHandler implements InvocationHandler {
        Ensure m_ensure;
        ResourceConsumer resourceConsumer = null;
        
        public DynamicProxyHandler(ResourceConsumer resourceConsumer, Ensure ensure) {
            this.resourceConsumer = resourceConsumer;
            m_ensure = ensure;
        }

        public void add(URL resource) {
            m_ensure.step(4);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(resourceConsumer, args);
        }
    } 
}
