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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceDependencyTest extends TestBase {
    public void testResourceDependency() throws MalformedURLException {
        DependencyManager m = getDM();
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
        ResourceProvider provider = new ResourceProvider(context, 
        		new URL("file://localhost/path/to/file1.txt"),
                new URL("file://localhost/path/to/file2.txt"),
                new URL("file://localhost/path/to/file3.doc"));
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
        provider.change(0);
        
        // wait for change callback
        e.waitForStep(5, 5000);
        e.step(6);
        
        // cleanup
        m.remove(dynamicProxyConsumer);
        m.remove(resourceProvider);
        m.remove(consumer);
        
        // validate that all consumed resources are "unconsumed" again
        c.ensure();
        m.clear();
    }
    
    class ResourceConsumer {
        private volatile int m_counter;
        private Ensure m_ensure;
        
        public ResourceConsumer(Ensure ensure) {
            m_ensure = ensure;
        }
        
        public void add(URL resource) {
            debug("ResourceConsumer.add(%s)", resource);
            m_counter++;
            m_ensure.step();
        }
        public void change(URL resource) {
            m_ensure.step();
        }
        public void remove(URL resource) {
            debug("ResourceConsumer.remove(%s)", resource);
            m_counter--;
        }
        public void ensure() {
            Assert.assertTrue("all resources should have been added and removed at this point, but " + m_counter + " are remaining", m_counter == 0);
        }
    }
    
    
    class ResourceConsumerFactory {
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
