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
import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectAnnotation {
    public interface ServiceInterface {
        public void invoke(Runnable run);
    }

    @Component
    public static class ServiceProvider implements ServiceInterface {
        public final static String ENSURE = "AspectAnnotation.ServiceProvider";
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;
        // Injected by reflection.
        protected volatile ServiceRegistration m_sr;

        @Init
        void init() {
            System.out.println("ServiceProvider.init");
        }

        @Destroy
        void destroy() {
            System.out.println("ServiceProvider.destroy");
        }

        public void invoke(Runnable run) {
            run.run();
            m_sequencer.step(6);
        }
    }

    @AspectService(ranking = 20)
    public static class ServiceAspect2 implements ServiceInterface {
        public final static String ENSURE = "AspectAnnotation.ServiceAspect2";

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;
        // Injected by reflection.
        private volatile ServiceInterface m_parentService;

        // Check auto config injections
        @Inject
        volatile BundleContext  m_bc;
        BundleContext m_bcNotInjected;

        @Inject
        volatile DependencyManager  m_dm;
        DependencyManager m_dmNotInjected;

        @Inject
        volatile org.apache.felix.dm.Component  m_component;
        org.apache.felix.dm.Component m_componentNotInjected;

        @Init
        void init() {
            System.out.println("ServiceAspect2.init");
        }

        @Destroy
        void destroy() {
            System.out.println("ServiceAspect2.destroy");
        }

        public void invoke(Runnable run) {
            checkInjectedFields();
            m_sequencer.step(3);
            m_parentService.invoke(run);
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

    @AspectService(ranking = 30, added = "add")
    public static class ServiceAspect3 implements ServiceInterface {
        public final static String ENSURE = "AspectAnnotation.ServiceAspect3";

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;
        // Injected using add callback.
        private volatile ServiceInterface m_parentService;

        @Init
        void init() {
            System.out.println("ServiceAspect3.init");
        }

        @Destroy
        void destroy() {
            System.out.println("ServiceAspect3.destroy");
        }

        void add(ServiceInterface si) {
            m_parentService = si;
        }

        public void invoke(Runnable run) {
            m_sequencer.step(2);
            m_parentService.invoke(run);
        }
    }

    @AspectService(ranking = 10, added = "added", removed = "removed")
    public static class ServiceAspect1 implements ServiceInterface {
        public final static String ENSURE = "AspectAnnotation.ServiceAspect1";
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;
        // Injected by reflection.
        private volatile ServiceInterface m_parentService;

        @Init
        void init() {
            System.out.println("ServiceAspect1.init");
        }

        @Destroy
        void destroy() {
            System.out.println("ServiceAspect1.destroy");
        }

        void added(ServiceInterface si) {
            m_parentService = si;
        }

        @Stop
        void stop() {
            m_sequencer.step(7);
        }

        void removed(ServiceInterface si) {
            m_sequencer.step(8);
        }

        public void invoke(Runnable run) {
            m_sequencer.step(4);
            m_parentService.invoke(run);
        }
    }

    @Component
    public static class ServiceConsumer implements Runnable {
        public final static String ENSURE = "AspectAnnotation.ServiceConsumer";

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        @ServiceDependency
        private volatile ServiceInterface m_service;

        private Thread m_thread;

        @Init
        public void init() {
            m_thread = new Thread(this, "ServiceConsumer");
            m_thread.start();
        }

        public void run() {
            m_sequencer.waitForStep(1, 2000);
            m_service.invoke(new Runnable() {
                public void run() {
                    m_sequencer.step(5);
                }
            });
        }

        @Destroy
        void destroy() {
            m_thread.interrupt();
            try {
                m_thread.join();
            } catch (InterruptedException e) {
            }
        }
    }
}
