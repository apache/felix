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

import static java.lang.String.format;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.ipojo.extender.builder.FactoryBuilderException;
import org.apache.felix.ipojo.extender.internal.DefaultJob;
import org.apache.felix.ipojo.extender.internal.Lifecycle;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class is responsible to create the factory for a given type declaration. It also instructs the factories to
 * create
 * the instance for each instance declaration targeting the managed factory.
 */
public class ManagedType implements FactoryStateListener, Lifecycle {
    /**
     * Identify the factory creation job submitted to the QueueService.
     */
    public static final String FACTORY_CREATION_JOB_TYPE = "factory.creation";
    /**
     * Identify the instance startup job submitted to the QueueService.
     */
    public static final String INSTANCE_STARTUP_JOB_TYPE = "instance.startup";
    /**
     * The bundle context
     */
    private final BundleContext m_bundleContext;
    /**
     * The queue service used for the creation.
     */
    private final QueueService m_queueService;
    /**
     * The type declaration that we have to handle.
     */
    private final TypeDeclaration m_declaration;
    /**
     * The service tracker tracking {@link ExtensionDeclaration} services.
     */
    private ServiceTracker m_extensionTracker;

    /**
     * The service tracker tracking the {@link InstanceDeclaration} services.
     */
    private ServiceTracker m_instanceTracker;

    /**
     * The job used to instantiate the factory.
     */
    private Future<IPojoFactory> m_future;

    /**
     * If the Managed Type cannot be initializes, sets this flag to true and no links will be created.
     */
    private boolean m_frozen;

    /**
     * Constructs a Managed Type object for the given type declaration.
     *
     * @param bundleContext the bundle context
     * @param queueService  the queue service
     * @param declaration   the declaration
     */
    public ManagedType(BundleContext bundleContext, QueueService queueService, TypeDeclaration declaration) {
        m_bundleContext = bundleContext;
        m_queueService = queueService;
        m_declaration = declaration;
        try {
            initExtensionTracker();
            initInstanceTracker();
        } catch (InvalidSyntaxException e) {
            // Error during filter creation, freeze the declaration and add a meaningful message
            m_frozen = true;
            m_declaration.unbind("Filter creation error", e);
        }
    }

    /**
     * Initializes the extension declaration tracker.
     *
     * @throws InvalidSyntaxException cannot happen
     */
    private void initExtensionTracker() throws InvalidSyntaxException {
        String filter = format(
                "(&(objectclass=%s)(%s=%s))",
                ExtensionDeclaration.class.getName(),
                ExtensionDeclaration.EXTENSION_NAME_PROPERTY,
                m_declaration.getExtension()
        );
        m_extensionTracker = new ServiceTracker(m_bundleContext, m_bundleContext.createFilter(filter), new ExtensionSupport());
    }

    /**
     * Initializes the instance declaration tracker.
     *
     * @throws InvalidSyntaxException cannot happen
     */
    private void initInstanceTracker() throws InvalidSyntaxException {

        String filter;
        String version = m_declaration.getComponentVersion();
        if (version != null) {
            // Track instance for:
            // * this component's name OR classname
            //            AND
            // * this component's version OR no version
            filter = format(
                    "(&(objectClass=%s)(|(%s=%s)(%s=%s))(|(%s=%s)(!(%s=*))))",
                    InstanceDeclaration.class.getName(),
                    InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                    m_declaration.getComponentName(),
                    InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                    getComponentClassname(),
                    InstanceDeclaration.COMPONENT_VERSION_PROPERTY,
                    version,
                    InstanceDeclaration.COMPONENT_VERSION_PROPERTY
            );
        } else {
            // Track instance for:
            // * this component's name OR classname
            //       AND
            // * no version
            filter = format(
                    "(&(objectClass=%s)(|(%s=%s)(%s=%s))(!(%s=*)))",
                    InstanceDeclaration.class.getName(),
                    InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                    m_declaration.getComponentName(),
                    InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                    getComponentClassname(),
                    InstanceDeclaration.COMPONENT_VERSION_PROPERTY
            );
        }
        m_instanceTracker = new ServiceTracker(m_bundleContext, m_bundleContext.createFilter(filter), new InstanceSupport());
    }

    /**
     * Returns the {@literal classname} attribute value.
     */
    private String getComponentClassname() {
        return m_declaration.getComponentMetadata().getAttribute("classname");
    }

    /**
     * Starting the management.
     * We open only the extension tracker.
     */
    public void start() {
        if (!m_frozen) {
            m_extensionTracker.open(true);
        }
    }

    /**
     * Stopping the management.
     */
    public void stop() {
        m_instanceTracker.close();
        m_extensionTracker.close();
    }

    /**
     * The factory we have built has a state in his change.
     *
     * @param factory  the changing factory
     * @param newState the new factory state
     */
    public void stateChanged(Factory factory, int newState) {
        if (Factory.VALID == newState) {
            // Start tracking instances
            m_instanceTracker.open(true);
        } else {
            // Un-track all instances
            m_instanceTracker.close();
        }
    }

    /**
     * The service tracker customizer for extension declaration.
     * It submits a factory building job when an extension matching our type declaration is found.
     */
    private class ExtensionSupport implements ServiceTrackerCustomizer {
        public Object addingService(ServiceReference reference) {
            final Object service = m_bundleContext.getService(reference);
            if (service instanceof ExtensionDeclaration) {
                m_future = m_queueService.submit(new DefaultJob<IPojoFactory>(reference.getBundle(), FACTORY_CREATION_JOB_TYPE) {

                    /**
                     * The factory creation job.
                     * @return the IPojoFactory
                     * @throws Exception the factory cannot be created
                     */
                    public IPojoFactory call() throws Exception {
                        ExtensionDeclaration declaration = (ExtensionDeclaration) service;
                        try {
                            // Build and start the factory instance
                            IPojoFactory factory = declaration.getFactoryBuilder().build(m_bundleContext, m_declaration.getComponentMetadata());
                            factory.addFactoryStateListener(ManagedType.this);
                            factory.start();

                            // Change the status
                            m_declaration.bind();

                            return factory;
                        } catch (FactoryBuilderException e) {
                            m_declaration.unbind(format("Cannot build '%s' factory instance", m_declaration.getExtension()), e);
                        } catch (Throwable t) {
                            m_declaration.unbind(format("Error during '%s' factory instance creation", m_declaration.getExtension()), t);
                        }

                        return null;
                    }
                }, format("Building Factory for type %s", m_declaration.getComponentName()));
                // Return something, otherwise, ServiceTracker think that we're not interested
                // in this service and never call us back on disposal.
                return service;
            }

            return null;
        }

        public void modifiedService(ServiceReference reference, Object o) {
        }

        public void removedService(ServiceReference reference, Object o) {

            ExtensionDeclaration extensionDeclaration = (ExtensionDeclaration) o;
            // Then stop the factory
            try {
                IPojoFactory factory = m_future.get();
                // It is possible that the factory couldn't be created
                if (factory != null) {
                    factory.removeFactoryStateListener(ManagedType.this);
                    factory.dispose();
                    m_declaration.unbind(format("Extension '%s' is missing",
                                                extensionDeclaration.getExtensionName()));
                }
            } catch (InterruptedException e) {
                m_declaration.unbind("Could not create Factory", e);
            } catch (ExecutionException e) {
                m_declaration.unbind("Factory creation throw an Exception", e);
            }
            m_future = null;
        }
    }

    private class InstanceSupport implements ServiceTrackerCustomizer {
        public Object addingService(final ServiceReference reference) {
            Object service = m_bundleContext.getService(reference);
            if (service instanceof InstanceDeclaration) {
                final InstanceDeclaration instanceDeclaration = (InstanceDeclaration) service;

                // Check that instance is not already bound
                if (instanceDeclaration.getStatus().isBound()) {
                    return null;
                }

                // Handle visibility (private/public factories)
                if (!m_declaration.isPublic()) {
                    if (!reference.getBundle().equals(m_bundleContext.getBundle())) {
                        Bundle origin = m_bundleContext.getBundle();
                        instanceDeclaration.unbind(
                                format("Component '%s/%s' is private. It only accept instances " +
                                               "from bundle %s/%s [%d] (instance bundle origin: %d)",
                                       m_declaration.getComponentName(),
                                       m_declaration.getComponentVersion(),
                                       origin.getSymbolicName(),
                                       origin.getVersion(),
                                       origin.getBundleId(),
                                       reference.getBundle().getBundleId())
                        );
                        return null;
                    }
                }

                return m_queueService.submit(new DefaultJob<ComponentInstance>(reference.getBundle(), INSTANCE_STARTUP_JOB_TYPE) {
                    public ComponentInstance call() throws Exception {
                        try {
                            // Create the component's instance
                            // It is automatically started
                            // Future.get should never be null since this tracker is started when the factory has been created
                            ComponentInstance instance = m_future.get().createComponentInstance(instanceDeclaration.getConfiguration());
                            if (instance instanceof InstanceBundleContextAware) {
                                ((InstanceBundleContextAware) instance).setInstanceBundleContext(instanceDeclaration
                                        .getBundle().getBundleContext());
                            }

                            // Notify the declaration that everything is fine
                            instanceDeclaration.bind();

                            return instance;
                        } catch (UnacceptableConfiguration c) {
                            instanceDeclaration.unbind(format("Instance configuration is invalid (component:%s/%s, bundle:%d)",
                                                              m_declaration.getComponentName(),
                                                              m_declaration.getComponentVersion(),
                                                              reference.getBundle().getBundleId()),
                                    c);
                        } catch (MissingHandlerException e) {
                            instanceDeclaration.unbind(
                                    format(
                                            "Component '%s/%s' (required for instance creation) is missing some handlers",
                                            m_declaration.getComponentName(),
                                            m_declaration.getComponentVersion()
                                    ),
                                    e);
                        } catch (ConfigurationException e) {
                            instanceDeclaration.unbind(
                                    format(
                                            "Instance configuration is incorrect for component '%s/%s'",
                                            m_declaration.getComponentName(),
                                            m_declaration.getComponentVersion()),
                                    e);
                        }

                        return null;
                    }
                }, format("Creating component instance of type %s (declaration from bundle %d)",
                          m_declaration.getComponentName(),
                          reference.getBundle().getBundleId()));
            }

            return null;
        }

        public void modifiedService(ServiceReference reference, Object o) {
        }

        public void removedService(ServiceReference reference, Object o) {
            InstanceDeclaration instanceDeclaration = (InstanceDeclaration) m_bundleContext.getService(reference);
            Future<ComponentInstance> future = (Future<ComponentInstance>) o;
            ComponentInstance instance;
            try {
                instance = future.get();
                // It is possible that the instance couldn't be created
                if (instance != null) {
                    String message = format("Factory for Component '%s/%s' is missing",
                                            instance.getFactory().getName(),
                                            m_declaration.getComponentVersion());
                    instanceDeclaration.unbind(message);

                    instance.stop();
                    instance.dispose();
                }

            } catch (InterruptedException e) {
                instanceDeclaration.unbind("Could not create ComponentInstance", e);
            } catch (ExecutionException e) {
                instanceDeclaration.unbind("ComponentInstance creation throw an Exception", e);
            }
        }
    }


}
