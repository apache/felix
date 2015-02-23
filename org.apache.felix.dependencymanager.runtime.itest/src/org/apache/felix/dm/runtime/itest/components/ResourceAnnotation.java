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
package org.apache.felix.dm.runtime.itest.components;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.ResourceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"rawtypes"})
public class ResourceAnnotation {
    public final static String ENSURE_RESOURCE = "ResourceAnnotation.resource";
    public final static String ENSURE_FIELD = "ResourceAnnotation.field";
    public final static String ENSURE_ADAPTER = "ResourceAnnotation.adapter";
    public final static String ENSURE_PROVIDER = "ResourceAnnotation.provider";

    /**
     * A Service provided the ServiceProvider, which is a ResourceAdapter.
     */
    public interface ServiceInterface extends Runnable {
    }

     /**
     * A Component which has a resource dependency.
     */
    @Component
    public static class ResourceConsumer {
        @ServiceDependency(required = true, filter = "(name=" + ENSURE_RESOURCE + ")")
        volatile Ensure m_sequencer;

        private volatile int m_resourcesSeen;

        @Start
        void start() {
            System.out.println("ResourceConsumer.start: sequencer=" + m_sequencer);
        }
        
        @ResourceDependency(required = false, filter = "(&(path=/path/to/*.txt)(host=localhost))")
        public void add(URL resource) {
            System.out.println("ResourceConsumer.add: resource=" + resource + ", m_sequencer=" + m_sequencer);
            if (match(resource, "file://localhost/path/to/test1.txt")) {
                m_resourcesSeen++;
                return;
            }

            if (match(resource, "file://localhost/path/to/test2.txt")) {
                m_resourcesSeen++;
                return;
            }

            Assert.fail("Got unexpected resource: " + resource);
        }

        private boolean match(URL resource, String url) {
            return url.equals(resource.toString());
        }

        @Stop
        void stop() {
            System.out.println("ResourceConsumer.stop: m_sequencer=" + m_sequencer);
            Assert.assertEquals(2, m_resourcesSeen);
            m_sequencer.step(1);
        }
    }

    /**
     * A Component which as a resource dependency, using a class field.
     */
    @Component
    public static class ResourceConsumerField {
        @ServiceDependency(required = true, filter = "(name=" + ENSURE_FIELD + ")")
        volatile Ensure m_sequencer;

        @ResourceDependency(filter = "(&(path=*/test1.txt)(host=localhost))")
        URL m_resource;

        @Init
        void init() {
            if (m_resource != null) {
                Assert.assertTrue("file://localhost/path/to/test1.txt".equals(m_resource.toString()));
                m_sequencer.step(1);
            }
        }
    }

    /**
     * Provides some simple resources.
     */
    @Component
    public static class ResourceProvider {
        @ServiceDependency(required = true, filter = "(name=" + ENSURE_PROVIDER + ")")
        volatile Ensure m_sequencer;

        @Inject
        private volatile BundleContext m_context;
        private final Map m_handlers = new HashMap();
        private final URL[] m_resources;

        public ResourceProvider() throws Exception {
            m_resources = new URL[]{
                    new URL("file://localhost/path/to/test1.txt"),
                    new URL("file://localhost/path/to/test2.txt"), 
                    new URL("file://localhost/path/to/README.doc")};
        }

        /**
         * Handles a new Resource consumer
         * @param serviceProperties
         * @param handler
         */
        @SuppressWarnings("unchecked")
        @ServiceDependency(removed = "remove", required = false)
        public void add(Map serviceProperties, ResourceHandler handler) {
            System.out.println("ResourceProvider.addResourceHandler " + handler);
            String filterString = (String) serviceProperties.get("filter");
            Filter filter = null;
            if (filterString != null) {
                try {
                    filter = m_context.createFilter(filterString);
                } catch (InvalidSyntaxException e) {
                    Assert.fail("Could not create filter for resource handler: " + e);
                    return;
                }
            }
            synchronized (m_handlers) {
                m_handlers.put(handler, filter);
            }
            for (int i = 0; i < m_resources.length; i++) {
                if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    System.out.println("ResourceProvider: calling handled.added(" + m_resources[i] + ")");
                    handler.added(m_resources[i], null);
                }
            }
        }

        /**
         * Remove a Resource consumer.
         * @param handler
         */
        public void remove(ResourceHandler handler) {
            System.out.println("ResourceProvider.removeResourceHandler " + handler);

            Filter filter;
            synchronized (m_handlers) {
                filter = (Filter) m_handlers.remove(handler);
            }
            removeResources(handler, filter);
        }

        private void removeResources(ResourceHandler handler, Filter filter) {
            for (int i = 0; i < m_resources.length; i++) {
                if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.removed(m_resources[i], null);
                }
            }
        }

        /**
         * Our component is being destroyed: notify all our registered Resource consumers that we don't
         * provide our Resources anymore.
         */
        @SuppressWarnings("unchecked")
        @Destroy
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

    /**
     * Our ServiceInterface provider, which service is activated by a ResourceAdapter.
     */
    @ResourceAdapterService(filter = "(&(path=/path/to/test1.txt)(host=localhost))", properties = {@Property(name = "foo", value = "bar")}, propagate = true)
    public static class ServiceProvider implements ServiceInterface {
        // Injected by reflection
        URL m_resource;

        @ServiceDependency(filter = "(name=" + ENSURE_ADAPTER + ")")
        Ensure m_sequencer;

        // Check auto config injections
        @Inject
        BundleContext m_bc;
        BundleContext m_bcNotInjected;

        @Inject
        DependencyManager m_dm;
        DependencyManager m_dmNotInjected;

        @Inject
        org.apache.felix.dm.Component m_component;
        org.apache.felix.dm.Component m_componentNotInjected;

        public void run() {
            checkInjectedFields();
            Assert.assertNotNull("Resource has not been injected in the adapter", m_resource);
            Assert.assertEquals("ServiceProvider did not get expected resource", "file://localhost/path/to/test1.txt",
                    m_resource.toString());
            m_sequencer.step(2);
        }

        private void checkInjectedFields() {
            if (m_bc == null) {
                m_sequencer.throwable(new Exception("Bundle Context not injected"));
                return;
            }
            if (m_bcNotInjected != null) {
                m_sequencer.throwable(new Exception("Bundle Context must not be injected"));
                return;
            }

            if (m_dm == null) {
                m_sequencer.throwable(new Exception("DependencyManager not injected"));
                return;
            }
            if (m_dmNotInjected != null) {
                m_sequencer.throwable(new Exception("DependencyManager must not be injected"));
                return;
            }

            if (m_component == null) {
                m_sequencer.throwable(new Exception("Component not injected"));
                return;
            }
            if (m_componentNotInjected != null) {
                m_sequencer.throwable(new Exception("Component must not be injected"));
                return;
            }
        }
    }
    
    /**
     * A Component with a dependency over the ServiceInterface, which is actually provided
     * by a ResourceAdapter.
     */
    @Component
    public static class ServiceConsumer {
        @ServiceDependency
        ServiceInterface m_serviceInterface;

        @ServiceDependency(filter = "(name=" + ENSURE_ADAPTER + ")")
        Ensure m_sequencer;

        @Start
        void start() {
            m_sequencer.step(1);
            m_serviceInterface.run();
        }
    }
}
