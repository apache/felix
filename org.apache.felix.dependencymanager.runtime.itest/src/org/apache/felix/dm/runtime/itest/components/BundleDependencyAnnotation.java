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
import org.apache.felix.dm.annotation.api.BundleAdapterService;
import org.apache.felix.dm.annotation.api.BundleDependency;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleDependencyAnnotation {
    public static final String ENSURE_CONSUMER = "BundleDependencyAnnotation.consumer";
    public static final String ENSURE_ADAPTER = "BundleDependencyAnnotation.adapter";
    public static final String METATYPE_BSN = "org.apache.felix.metatype";

    public interface ServiceInterface extends Runnable {
    }

    /**
     * Simple Consumer which has a BundleDependency dependency.
     */
    @Component
    public static class Consumer {
        @ServiceDependency(filter = "(name=" + ENSURE_CONSUMER + ")")
        private volatile Ensure m_sequencer;

        @BundleDependency(required = true, removed = "removed", filter = "(Bundle-SymbolicName=" + METATYPE_BSN + ")", stateMask = Bundle.ACTIVE)
        public void add(Bundle b) {
            if (b != null && b.getSymbolicName().equals(METATYPE_BSN)) {
                m_sequencer.step(1);
            }
        }

        @Start
        public void start() {
            m_sequencer.step(2);
        }

        @Stop
        public void stop() {
            m_sequencer.step(3);
        }
        
        protected void removed(Bundle b) {
            if (b != null && b.getSymbolicName().equals(METATYPE_BSN)) {
                m_sequencer.step(4);
            }
        }
    }

    /**
     * ServiceInterface Consumer.
     */
    @Component
    public static class ServiceConsumer {
        @ServiceDependency(filter = "(name=" + ENSURE_ADAPTER + ")")
        volatile Ensure m_sequencer;

        @ServiceDependency
        volatile ServiceInterface m_service;

        @Start
        void start() {
            m_sequencer.step(2);
            m_service.run();
        }
    }

    /**
     * A BundleAdapter test, which adapts the dependency manager bundle to the ServiceInterface service.
     */
    @BundleAdapterService(filter = "(Bundle-SymbolicName=" + METATYPE_BSN + ")", stateMask = Bundle.INSTALLED
            | Bundle.RESOLVED | Bundle.ACTIVE, propagate = true, properties = {@Property(name = "foo", value = "bar")})
    public static class ServiceProvider implements ServiceInterface {
        // Adapted bundle (injected by reflection).
        protected volatile Bundle m_bundle;

        // Our Sequencer required dependency
        @ServiceDependency(filter = "(name=" + ENSURE_ADAPTER + ")")
        volatile Ensure m_sequencer;

        // Check auto config injections
        @Inject
        volatile BundleContext m_bc;
        BundleContext m_bcNotInjected;

        @Inject
        volatile DependencyManager m_dm;
        DependencyManager m_dmNotInjected;

        @Inject
        volatile org.apache.felix.dm.Component m_component;
        org.apache.felix.dm.Component m_componentNotInjected;

        @Start
        void start() {
            checkInjectedFields();
            m_sequencer.step(1);
        }

        public void run() {
            if (m_bundle == null || !m_bundle.getSymbolicName().equals(METATYPE_BSN)) {
                throw new IllegalStateException("ServiceProvider did not get proper bundle: " + m_bundle);
            }
            m_sequencer.step(3);
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
}
