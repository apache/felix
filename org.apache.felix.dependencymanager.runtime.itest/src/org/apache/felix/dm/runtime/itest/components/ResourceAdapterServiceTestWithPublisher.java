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

import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.LifecycleController;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Test a ResourceAdapterService which provides its interface using a @ServiceLifecycle.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"deprecation", "unchecked", "rawtypes", "serial"})
public class ResourceAdapterServiceTestWithPublisher {
    public static final String ENSURE = "ResourceAdapterServiceTestWithPublisher";

    public interface Provider {
    }

    @Component
    public static class Consumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @ServiceDependency(required = false, removed = "unbind")
        void bind(Map properties, Provider provider) {
            m_sequencer.step(1);
            // check ProviderImpl properties
            if ("bar".equals(properties.get("foo"))) {
                m_sequencer.step(2);
            }
            // check extra ProviderImpl properties (returned by start method)
            if ("bar2".equals(properties.get("foo2"))) {
                m_sequencer.step(3);
            }
            // check properties propagated by the resource adapter
            if ("/path/to/test1.txt".equals(properties.get(ResourceHandler.PATH))) {
                m_sequencer.step(4);
            }
            if ("localhost".equals(properties.get(ResourceHandler.HOST))) {
                m_sequencer.step(5);
            }
        }

        void unbind(Provider provider) {
            m_sequencer.step(6);
        }
    }

    @Component
    public static class ResourceProvider {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @Inject
        private volatile BundleContext m_context;
        private final Map m_handlers = new HashMap();
        private URL[] m_resources;

        public ResourceProvider() throws Exception {
            m_resources = new URL[]{new URL("file://localhost/path/to/test1.txt"),
                    new URL("file://localhost/path/to/test2.txt"), new URL("file://localhost/path/to/README.doc")};
        }

        /**
         * Handles a new Resource consumer
         * @param serviceProperties
         * @param handler
         */
        @ServiceDependency(removed = "remove", required = false)
        public void add(Map serviceProperties, ResourceHandler handler) {
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
                    handler.added(m_resources[i]);
                }
            }
        }

        /**
         * Remove a Resource consumer.bar
         * @param handler
         */
        public void remove(ResourceHandler handler) {
            Filter filter;
            synchronized (m_handlers) {
                filter = (Filter) m_handlers.remove(handler);
            }
            removeResources(handler, filter);
        }

        private void removeResources(ResourceHandler handler, Filter filter) {
            for (int i = 0; i < m_resources.length; i++) {
                if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.removed(m_resources[i]);
                }
            }
        }

        /**
         * Our component is being destroyed: notify all our registered Resource consumers that we don't
         * provide our Resources anymore.
         */
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
    
    @ResourceAdapterService(filter = "(&(path=/path/to/test1.txt)(host=localhost))", propagate = true)
    @Property(name = "foo", value = "bar")
    public static class ProviderImpl implements Provider {
        @LifecycleController
        volatile Runnable m_publisher; // injected and used to register our service

        @LifecycleController(start = false)
        volatile Runnable m_unpublisher; // injected and used to unregister our service

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        // Injected by reflection
        volatile URL m_resource;

        @Init
        void init() {
            // register service in 1 second
            Utils.schedule(m_publisher, 1000);
            // unregister the service in 2 seconds
            Utils.schedule(m_unpublisher, 2000);
        }

        @Start
        Map start() {
            // Add some extra service properties ... they will be appended to the one we have defined
            // in the @Service annotation.
            return new HashMap() {
                {
                    put("foo2", "bar2");
                }
            };
        }
    }
}
