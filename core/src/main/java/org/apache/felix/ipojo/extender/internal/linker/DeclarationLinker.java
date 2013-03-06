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

package org.apache.felix.ipojo.extender.internal.linker;

import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.ipojo.extender.internal.Lifecycle;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The linker is responsible to bind extension declaration to type declaration.
 * It tracks TypeDeclaration, and reifies them as factory.
 */
public class DeclarationLinker implements ServiceTrackerCustomizer, Lifecycle {
    /**
     * The bundle context. It uses the iPOJO bundle context.
     */
    private final BundleContext m_bundleContext;

    /**
     * The queue service on which we delegate the binding process.
     */
    private final QueueService m_queueService;

    /**
     * The service tracker looking for TypeDeclaration.
     */
    private final ServiceTracker m_typeTracker;

    /**
     * Creates the linker.
     *
     * @param bundleContext the bundle context
     * @param queueService  the queue service
     */
    public DeclarationLinker(BundleContext bundleContext, QueueService queueService) {
        m_bundleContext = bundleContext;
        m_queueService = queueService;
        m_typeTracker = new ServiceTracker(m_bundleContext, TypeDeclaration.class.getName(), this);
    }

    /**
     * When the iPOJO management starts, we look for type declaration.
     */
    public void start() {
        m_typeTracker.open(true);
    }

    /**
     * When iPOJO stops, we close the tracker.
     */
    public void stop() {
        m_typeTracker.close();
    }

    /**
     * A new type declaration was published.
     *
     * @param reference the service reference of the type declaration
     * @return the managed type object wrapping the service object.
     */
    public Object addingService(ServiceReference reference) {
        TypeDeclaration declaration = (TypeDeclaration) m_bundleContext.getService(reference);
        ManagedType managedType = new ManagedType(reference.getBundle().getBundleContext(), m_queueService, declaration);
        managedType.start();
        return managedType;
    }

    /**
     * Type declaration cannot be modified.
     *
     * @param reference the reference
     * @param service   the object returned by {@link #addingService(org.osgi.framework.ServiceReference)}
     */
    public void modifiedService(ServiceReference reference, Object service) {
        // Ignored
    }

    /**
     * A type declaration service was withdrawn from the  service registry.
     *
     * @param reference the leaving reference
     * @param service   the object returned by {@link #addingService(org.osgi.framework.ServiceReference)}
     */
    public void removedService(ServiceReference reference, Object service) {
        ManagedType managedType = (ManagedType) service;
        managedType.stop();
    }
}
