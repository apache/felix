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
package org.apache.felix.dm.lambda;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.impl.BundleAdapterBuilderImpl;
import org.apache.felix.dm.lambda.impl.BundleDependencyBuilderImpl;
import org.apache.felix.dm.lambda.impl.CompletableFutureDependencyImpl;
import org.apache.felix.dm.lambda.impl.ComponentBuilderImpl;
import org.apache.felix.dm.lambda.impl.ConfigurationDependencyBuilderImpl;
import org.apache.felix.dm.lambda.impl.FactoryPidAdapterBuilderImpl;
import org.apache.felix.dm.lambda.impl.ServiceAdapterBuilderImpl;
import org.apache.felix.dm.lambda.impl.ServiceAspectBuilderImpl;
import org.apache.felix.dm.lambda.impl.ServiceDependencyBuilderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Defines a base for Activators in order to build DependencyManager Components using a java8 style.<p>
 * 
 * Code example using auto configured fields:
 * 
 * <pre> {@code
 * 
 * import org.apache.felix.dm.lambda.DependencyManagerActivator;
 *
 * public class Activator extends DependencyManagerActivator {    
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *         component(comp -> comp
 *             .provides(Service.class, property -> "value")
 *             .impl(ServiceImpl.class)            
 *             .withSvc(LogService.class, ConfigurationAdmni.class) // both services are required and injected in class fields with compatible types.           
 *     }
 * }
 * }</pre>
 *
 * Code example using reflection callbacks:
 * 
 * <pre> {@code
 * import org.apache.felix.dm.lambda.DependencyManagerActivator;
 *
 * public class Activator extends DependencyManagerActivator {    
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *         component(comp -> comp
 *             .provides(Service.class, property -> "value")
 *             .impl(ServiceImpl.class)            
 *             .withSvc(LogService.class, svc -> svc.add("setLog"))              
 *             .withSvc(ConfigurationAdmni.class, svc -> svc.add("setConfigAdmin")))                
 *     }
 * }
 * }</pre>
 *
 * Code example using method references:
 * 
 * <pre> {@code
 * import org.apache.felix.dm.lambda.DependencyManagerActivator;
 *
 * public class Activator extends DependencyManagerActivator {    
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *         component(comp -> comp
 *             .provides(Service.class, property -> "value")
 *             .impl(ServiceImpl.class)            
 *             .withSvc(LogService.class, svc -> svc.add(ServiceImpl::setLog))              
 *             .withSvc(ConfigurationAdmni.class, svc -> svc.add(ServiceImpl::setConfigAdmin)))                
 *     }
 * }
 * }</pre>
 * 
 * When a dependency is not explicitly defined as "required" or "optional", 
 * then it is assumed to be optional by default, like it is the case with the original DM API.
 * You can change the default mode using the "org.apache.felix.dependencymanager.lambda.defaultRequiredDependency system property"
 * (see Felix dm-lambda online documentation).
 */
public abstract class DependencyManagerActivator implements BundleActivator {    
	/**
	 * DependencyManager object used to create/register real DM Components that are built by this activator.
	 */
    private DependencyManager m_manager;
    
    /**
     * Our Activator is starting.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        m_manager = new DependencyManager(context);
        init(context, m_manager);
    }

    /**
     * Our Activator is stopped.
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        destroy();
    }

    /**
     * Sub classes must override this method in order to build some DM components.
     * @param ctx the context associated to the bundle
     * @param dm the DependencyManager assocaited to this activator
     * @throws Exception if the activation fails
     */
    protected abstract void init(BundleContext ctx, DependencyManager dm) throws Exception;

    /**
     * Sub classes may override this method that is called when the Activator is stopped.
     * @throws Exception if the deactivation fails
     */
    protected void destroy() throws Exception {
    }
    
    /**
     * Returns the DependencyManager used to create/managed DM Components.
     * 
     * @return the DependencyManager associated to this Activator
     */
    public DependencyManager getDM() {
        return m_manager;
    }
    
    /**
     * Returns the bundle context that is associated with this bundle.
     * 
     * @return the bundle context
     */
    public BundleContext getBC() {
        return m_manager.getBundleContext();
    }
    
    /**
     * Creates a Component builder that can be used to create a DM Component. 
     * @return a Component builder that can be used to create a DM Component.
     */
    protected ComponentBuilder<?> component() {
        return new ComponentBuilderImpl(m_manager);
    }
    
    /**
     * Creates a service Aspect builder that can be used to create a DM Aspect Component.
     * 
     * @param <T> the aspect service type
     * @param aspectType the aspect service
     * @return a service Aspect builder.
     */
    protected <T> ServiceAspectBuilder<T> aspect(Class<T> aspectType) {
        ServiceAspectBuilderImpl<T> aspectBuilder = new ServiceAspectBuilderImpl<>(m_manager, aspectType);
        return aspectBuilder;
    }
    
    /**
     * Creates a service Adapter builder that can be used to create a DM Adapter Component. 
     *
     * @param <T> the adapted service type.
     * @param adaptee the adapted service
     * @return a service Adapter builder.
     */
    protected <T> ServiceAdapterBuilder<T> adapter(Class<T> adaptee) {
        ServiceAdapterBuilderImpl<T> adapterBuilder = new ServiceAdapterBuilderImpl<>(m_manager, adaptee);
        return adapterBuilder;
    }
    
    /**
     * Builds a DM Component using a Java8 style ComponentBuilder.
     * @param consumer the lambda that will use the ComponentBuilder for building the DM component. 
     * The component is auto-added to the DependencyManager, unless the lambda calls the ComponentBuilder.autoAdd(false) method.
     * @return a newly built DM component.
     */
    protected Component component(Consumer<ComponentBuilder<?>> consumer) {
        return component(m_manager, consumer);
    }
        
    /**
     * Builds a DM Aspect Component using a Java8 style AspectBuilder.
     * The component is auto-added to the DependencyManager, unless the lambda calls the AspectBuilder.autoAdd(false) method.
     *
     * @param <T> the aspect service type
     * @param aspect the aspect service
     * @param consumer the lambda that will use the AspectBuilder for building the DM aspect component. 
     * @return the DM component build by the consumer of the aspect builder
     */
    protected <T> Component aspect(Class<T> aspect, Consumer<ServiceAspectBuilder<T>> consumer) {
        return aspect(m_manager, aspect, consumer);
    }

    /**
     * Builds a DM Adapter Component using a Java8 style AdapterBuilder.
     * The component is auto-added to the DependencyManager, unless the lambda calls the AdapterBuilder.autoAdd(false) method.
     * 
     * @param <T> the adapted service type
     * @param adaptee the adapted service
     * @param consumer the lambda that will use the AdapterBuilder for building the DM adapter component. 
     * @return a newly built DM component.
     */
    protected <T> Component adapter(Class<T> adaptee, Consumer<ServiceAdapterBuilder<T>> consumer) {
        return adapter(m_manager, adaptee, consumer);
    }
    
    /**
     * Builds a DM Factory Configuration Adapter Component using a Java8 style FactoryPidAdapterBuilder.
     * The component is auto-added to the DependencyManager, unless the lambda calls the FactoryPidAdapterBuilder.autoAdd(false) method.
     *
     * @param consumer the lambda that will use the FactoryPidAdapterBuilder for building the DM factory configuration adapter component. 
     * @return a newly built DM component.
     */
    protected Component factoryPidAdapter(Consumer<FactoryPidAdapterBuilder> consumer) {
        return factoryPidAdapter(m_manager, consumer);
    }

    /**
     * Builds a DM Bundle Adapter Component.
     * @param consumer the lambda used to build the actual bundle adapter. 
     * The component is auto-added to the DependencyManager, unless the lambda calls the BundleAdapter.autoAdd(false) method.
     * @return a newly built DM component.
     */
    protected Component bundleAdapter(Consumer<BundleAdapterBuilder> consumer) {
        return bundleAdapter(m_manager, consumer);
    }
       
    // These static methods can be used when building DM components outside of an activator.
	
    /**
     * Creates a Component builder that can be used to create a Component. 
     * 
     * @param dm the DependencyManager object used to create the component builder
     * @return a Component builder that can be used to create a Component.
     */
    public static ComponentBuilder<?> component(DependencyManager dm) {
        return new ComponentBuilderImpl(dm);
    }
    
    /**
     * Creates a service Aspect builder that can be used to create an Aspect Component. 
     *
     * @param <T> the aspect service type
     * @param dm the DependencyManager object used to register the built component
     * @param aspect the type of the aspect service
     * @return a service Aspect builder that can be used to create an Aspect Component.
     */
    public static <T> ServiceAspectBuilder<T> aspect(DependencyManager dm, Class<T> aspect) {
        ServiceAspectBuilderImpl<T> aspectBuilder = new ServiceAspectBuilderImpl<>(dm, aspect);
        return aspectBuilder;
    }
    
    /**
     * Creates a service Adapter builder that can be used to create an Adapter Component.
     * 
     * @param <T> the adapted service type
     * @param dm the DependencyManager object used to register the built component
     * @param adaptee the type of the adaptee service
     * @return a service Adapter builder that can be used to create an Adapter Component.
     */
    public static <T> ServiceAdapterBuilder<T> adapter(DependencyManager dm, Class<T> adaptee) {
        ServiceAdapterBuilderImpl<T> adapterBuilder = new ServiceAdapterBuilderImpl<>(dm, adaptee);
        return adapterBuilder;
    }

    /**
     * Creates a factory pid adapter that can be used to create a factory adapter Component. 
     * @param dm the DependencyManager object used to register the built component
     * @return a factory pid adapter that can be used to create a factory adapter Component. 
     */
    public static FactoryPidAdapterBuilder factoryPidAdapter(DependencyManager dm) {
        return new FactoryPidAdapterBuilderImpl(dm);
    }
   
    /**
     * Creates a bundle adapter builder that can be used to create a DM bundle adapter Component. 
     *
     * @param dm the DependencyManager object used to create the bundle adapter builder.
     * @return a bundle adapter builder that can be used to create a DM bundle adapter Component.
     */
    public static BundleAdapterBuilder bundleAdapter(DependencyManager dm) {
        return new BundleAdapterBuilderImpl(dm);
    }
   
    /**
     * Creates a DM ServiceDependency builder.
     *
     * @param <T> the service dependency type
     * @param component the component on which you want to build a new service dependency using the returned builder
     * @param service the service dependency type.
     * @return a DM ServiceDependency builder.
     */
    public static <T> ServiceDependencyBuilder<T> serviceDependency(Component component, Class<T> service) {
        return new ServiceDependencyBuilderImpl<>(component, service);
    }
    
    /**
     * Creates a DM Configuration Dependency builder.
     * 
     * @param component the component on which you want to build a new configuration dependency using the returned builder
     * @return a DM Configuration Dependency builder.
     */
    public static ConfigurationDependencyBuilder confDependency(Component component) {
        return new ConfigurationDependencyBuilderImpl(component);
    }
    
    /**
     * Creates a DM Bundle Dependency builder.
     * 
     * @param component the component on which you want to build a new bundle dependency using the returned builder
     * @return a DM Configuration Dependency builder.
     */
    public static BundleDependencyBuilder bundleDependency(Component component) {
        return new BundleDependencyBuilderImpl(component);
    }

    /**
     * Creates a DM CompletableFuture Dependency builder.
     *
     * @param <F> the type of the CompletableFuture result.
     * @param component the component on which you want to build a new completable future dependency using the returned builder.
     * @param future the future the dependency built using the returned builder will depend on.
     * @return a CompletableFuture dependency builder.
     */
    public static <F> FutureDependencyBuilder<F> futureDependency(Component component, CompletableFuture<F> future) {
        return new CompletableFutureDependencyImpl<>(component, future);
    }

    /**
     * Builds a component using a lambda and a component builder
     * @param dm the DependencyManager where the component is auto-added (unless the component.autoAdd(false) is called)
     * @param consumer a lambda that is called to build the component. When the lambda is called, it will be provided with a 
     * ComponentBuilder object that is used to build the actual DM component.
     * 
     * @return the built DM component.
     */
    public static Component component(DependencyManager dm, Consumer<ComponentBuilder<?>> consumer) {
        ComponentBuilder<?> componentBuilder = new ComponentBuilderImpl(dm);
        consumer.accept(componentBuilder);
        Component comp = componentBuilder.build();
        if (((ComponentBuilderImpl) componentBuilder).isAutoAdd()) {
        	dm.add(comp);
        }
        return comp;
    }

    /**
     * Update an existing component. Typically, this method can be used from a Component.init method, where more dependencies has to be added.
     * @param comp an existing DM component
     * @param consumer the lambda that will be used to update the component
     */
    public static void component(Component comp, Consumer<ComponentBuilder<?>> consumer) {
        ComponentBuilder<?> componentBuilder = new ComponentBuilderImpl(comp, true /* update component */);
        consumer.accept(componentBuilder);
        componentBuilder.build();
    }
    
    /**
     * Builds an aspect DM Component.
     * 
     * @param <T> the aspect service type
     * @param dm the DependencyManager object used to register the built component
     * @param aspect the type of the aspect service
     * @param consumer a lambda used to build the DM aspect component
     * @return a new DM aspect component. The aspect component is auto-added into the dm object, unless the lambda calls
     * the AspectBuilder.autoAdd(false) method.
     */
    public static <T> Component aspect(DependencyManager dm, Class<T> aspect, Consumer<ServiceAspectBuilder<T>> consumer) {
        ServiceAspectBuilderImpl<T> aspectBuilder = new ServiceAspectBuilderImpl<>(dm, aspect);
        consumer.accept(aspectBuilder);
        Component comp = aspectBuilder.build();
        if (aspectBuilder.isAutoAdd()) {
        	dm.add(comp);
        }
        return comp;
    }    
        
    /**
     * Builds an adapter DM Component.
     * 
     * @param <T> the adapted service type
     * @param dm the DependencyManager object used to register the built component
     * @param adaptee the type of the adapted service
     * @param consumer a lambda used to build the DM adapter component
     * @return a new DM adapter component. The adapter component is auto-added into the dm object, unless the lambda calls
     * the AspectBuilder.autoAdd(false) method is called.
     */
    public static <T> Component adapter(DependencyManager dm, Class<T> adaptee, Consumer<ServiceAdapterBuilder<T>> consumer) {
        ServiceAdapterBuilderImpl<T> adapterBuilder = new ServiceAdapterBuilderImpl<>(dm, adaptee);
        consumer.accept(adapterBuilder);
        Component comp = adapterBuilder.build();
        if (adapterBuilder.isAutoAdd()) {
        	dm.add(comp);
        }
        return comp;
    }
      
    /**
     * Builds a bundle adapter DM Component.
     * 
     * @param dm the DependencyManager object used to register the built component
     * @param consumer a lambda used to build the bundle adapter component
     * @return a new bundle adapter component. The adapter component is auto-added into the dm object, unless the lambda calls
     * the AspectBuilder.autoAdd(false) method is called.
     */
    public static Component bundleAdapter(DependencyManager dm, Consumer<BundleAdapterBuilder> consumer) {
        BundleAdapterBuilderImpl adapterBuilder = new BundleAdapterBuilderImpl(dm);
        consumer.accept(adapterBuilder);
        Component comp = adapterBuilder.build();
        if (adapterBuilder.isAutoAdd()) {
            dm.add(comp);
        }
        return comp;
    }

    /**
     * Builds a DM factory configuration adapter.
     * @param dm the DependencyManager object used to create DM components.
     * @param consumer a lambda used to build the DM factory configuration adapter component
     * @return a new DM factory configuration adapter component. The adapter component is auto-added into the dm object, unless the lambda calls
     * the FactoryPidAdapterBuilder.autoAdd(false) method is called
     */
    public static Component factoryPidAdapter(DependencyManager dm, Consumer<FactoryPidAdapterBuilder> consumer) {
        FactoryPidAdapterBuilderImpl factoryPidAdapter = new FactoryPidAdapterBuilderImpl(dm);
        consumer.accept(factoryPidAdapter);
        Component comp = factoryPidAdapter.build();
        if (factoryPidAdapter.isAutoAdd()) {
        	dm.add(comp);
        }
        return comp;
    }
}
