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

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.Unregistered;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SimpleAnnotations {
    /**
     * Provides a <code>Runnable</code> service, which is required by the
     * {@link Consumer} class.
     */
    @Component(properties = {@Property(name = "foo", value = "bar"), @Property(name="type", value="SimpleAnnotations")})
    public static class Producer implements Runnable {
        public final static String ENSURE = "SimpleAnnotations.Producer";
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure _ensure;

        @ServiceDependency
        volatile LogService _logService;

        @Inject
        volatile BundleContext _ctx;

        @Init
        protected void init() {
            _logService.log(LogService.LOG_INFO, "producer.init");
            // Our component is initializing (at this point: all required
            // dependencies are injected).
            _ensure.step(1);
        }

        @Start
        protected void start() {
            // We are about to be registered in the OSGi registry.
            _ensure.step(2);
        }

        @Registered
        protected void registered(ServiceRegistration sr) {
            _logService.log(LogService.LOG_INFO, "Registered");
            if (sr == null) {
                _ensure.throwable(new Exception("ServiceRegistration is null"));
            }
            if (!"bar".equals(sr.getReference().getProperty("foo"))) {
                _ensure.throwable(new Exception("Invalid Service Properties"));
            }
            _ensure.step(3);
        }

        public void run() {
            _ensure.step(5);
        }

        @Stop
        protected void stop() {
            // We are about to be unregistered from the OSGi registry, and we
            // must stop.
            _ensure.step(8);
        }

        @Unregistered
        protected void stopped() {
            // We are unregistered from the OSGi registry.
            _ensure.step(9);
        }

        @Destroy
        public void destroy() {
            // Our component is shutting down.
            _ensure.step(10);
        }
    }

    /**
     * Consumes a service which is provided by the {@link Producer} class.
     */
    @Component
    public static class Consumer {
        public final static String ENSURE = "SimpleAnnotations.Consumer";
        
        @ServiceDependency
        volatile LogService _logService;

        @ServiceDependency(filter="(type=SimpleAnnotations)")
        volatile Runnable _runnable;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure _ensure;

        @Inject
        volatile BundleContext _bc;
        BundleContext _bcNotInjected;

        @Inject
        volatile DependencyManager _dm;
        DependencyManager _dmNotInjected;

        @Inject
        volatile org.apache.felix.dm.Component _component;
        org.apache.felix.dm.Component _componentNotInjected;

        @Start
        protected void start() {
            _logService.log(LogService.LOG_INFO, "Consumer.START: ");
            checkInjectedFields();
            _ensure.step(4);
            _runnable.run();
        }
        
        private void checkInjectedFields() {
            if (_bc == null) {
                _ensure.throwable(new Exception("Bundle Context not injected"));
                return;
            }
            if (_bcNotInjected != null) {
                _ensure.throwable(new Exception("Bundle Context must not be injected"));
                return;
            }

            if (_dm == null) {
                _ensure.throwable(new Exception("DependencyManager not injected"));
                return;
            }
            if (_dmNotInjected != null) {
                _ensure.throwable(new Exception("DependencyManager must not be injected"));
                return;
            }

            if (_component == null) {
                _ensure.throwable(new Exception("Component not injected"));
                return;
            }
            if (_componentNotInjected != null) {
                _ensure.throwable(new Exception("Component must not be injected"));
                return;
            }
        }

        @Stop
        protected void stop() {
            _ensure.step(6);
        }
        
        @Destroy
        void destroy() {
            _ensure.step(7);
        }
    }
}
