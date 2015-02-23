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

import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterAnnotation {
    public interface S1 {
        public void run();
    }

    public interface S2 {
        public void run2();
    }

    public interface S3 {
        public void run3();
    }

    @Component
    public static class S3Consumer {
        private volatile Map<String, String> m_serviceProperties;
        private volatile S3 m_s3;

        @ServiceDependency
        void bind(Map<String, String> serviceProperties, S3 s3) {
            m_serviceProperties = serviceProperties;
            m_s3 = s3;
        }

        @Start
        void start() {
            // The adapter service must inherit from adaptee service properties ...
            if ("value1".equals(m_serviceProperties.get("param1")) // adaptee properties
                && "true".equals(m_serviceProperties.get("adapter"))) // adapter properties
            {
                m_s3.run3();
            }
        }
    }

    @AdapterService(adapteeService = S1.class, properties = { @Property(name = "adapter", value = "true") })
    public static class S1ToS3AdapterAutoConfig implements S3 {
        public static final String ENSURE = "AdapterAnnotation.autoConfig";

        // This is the adapted service
        protected volatile S1 m_s1;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

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

        public void run3() {
            checkInjectedFields();
            m_s1.run();
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

    @AdapterService(adapteeService = S1.class, properties = { @Property(name = "adapter", value = "true") }, field = "m_s1")
    public static class S1ToS3AdapterAutoConfigField implements S3 {
        public final static String ENSURE = "AdapterAnnotation.autoConfig.field";
        // This is the adapted service
        protected volatile S1 m_s1;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        public void run3() {
            m_s1.run();
            m_sequencer.step(3);
        }
    }

    @AdapterService(adapteeService = S1.class, properties = { @Property(name = "adapter", value = "true") }, added = "bind", removed = "removed")
    public static class S1ToS3AdapterCallback implements S3 {
        public final static String ENSURE = "AdapterAnnotation.callback";
        // This is the adapted service
        protected Object m_s1;

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected Ensure m_sequencer;

        void bind(S1 s1) {
            m_s1 = s1;
        }

        public void run3() {
            ((S1) m_s1).run();
        }

        @Stop
        void stop() {
            m_sequencer.step(3);
        }

        void removed(S1 s1) {
            m_sequencer.step(4);
        }
    }

    @Component(properties = { @Property(name = "param1", value = "value1") })
    public static class S1Impl implements S1 {
        public final static String ENSURE = "AdapterAnnotation.S1Impl";

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected Ensure m_sequencer;

        @ServiceDependency
        protected S2 m_s2;

        public void run() {
            m_sequencer.step(1);
            m_s2.run2();
        }
    }

    @Component
    public static class S2Impl implements S2 {
        public final static String ENSURE = "AdapterAnnotation.S2Impl";

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected Ensure m_sequencer;

        public void run2() {
            m_sequencer.step(2);
        }
    }
}
