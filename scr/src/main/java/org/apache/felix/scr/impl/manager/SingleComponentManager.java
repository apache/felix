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
package org.apache.felix.scr.impl.manager;


<<<<<<< HEAD
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.TargetedPID;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.helper.ActivateMethod.ActivatorParameter;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ModifiedMethod;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;
=======
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.manager.DependencyManager.OpenStatus;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 */
public class SingleComponentManager<S> extends AbstractComponentManager<S> implements ServiceFactory<S>
{

    // keep the using bundles as reference "counters" for instance deactivation
    private final AtomicInteger m_useCount = new AtomicInteger( );

    // The context that will be passed to the implementationObject
    private volatile ComponentContextImpl<S> m_componentContext;

<<<<<<< HEAD
    // the component holder responsible for managing this component
    private final ComponentHolder m_componentHolder;

    // optional properties provided in the ComponentFactory.newInstance method
    private Dictionary<String, Object> m_factoryProperties;

    // the component properties, also used as service properties
    private Dictionary<String, Object> m_properties;
=======
    // Merged properties from xml descriptor and all configurations
    private Map<String, Object> m_configurationProperties;

    // optional properties provided in the ComponentFactory.newInstance method
    private Map<String, Object> m_factoryProperties;

    // the component properties, also used as service properties
    private Map<String, Object> m_properties;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    // properties supplied ot ExtComponentContext.updateProperties
    // null if properties are not to be overwritten
    private Dictionary<String, Object> m_serviceProperties;

<<<<<<< HEAD
    // the component properties from the Configuration Admin Service
    // this is null, if none exist or none are provided
    private Dictionary<String, Object> m_configurationProperties;
    
    private volatile long m_changeCount = -1;
    private TargetedPID m_targetedPID;


    private final ThreadLocal<Boolean> m_circularReferences = new ThreadLocal<Boolean>();
    
   /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     * @param componentMethods
     */
    public SingleComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
            ComponentMetadata metadata, ComponentMethods componentMethods )
    {
        this(activator, componentHolder, metadata, componentMethods, false);
    }
    
    public SingleComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
            ComponentMetadata metadata, ComponentMethods componentMethods, boolean factoryInstance )
    {
        super( activator, metadata, componentMethods, factoryInstance );

        m_componentHolder = componentHolder;
    }

    void clear()
    {
        if ( m_componentHolder != null )
        {
            m_componentHolder.disposed( this );
        }
=======
    /**
     * The constructor receives both the activator and the metadata
     * @param componentMethods
     */
    public SingleComponentManager( final ComponentContainer<S> container, final ComponentMethods<S> componentMethods )
    {
        this(container, componentMethods, false);
    }

    public SingleComponentManager( final ComponentContainer<S> container, final ComponentMethods<S> componentMethods,
            final boolean factoryInstance )
    {
        super( container, componentMethods, factoryInstance );
    }

    @Override
    void clear()
    {
        m_container.disposed( this );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        super.clear();
    }


    // 1. Load the component implementation class
    // 2. Create the component instance and component context
    // 3. Bind the target services
    // 4. Call the activate method, if present
    // if this method is overwritten, the deleteComponent method should
    // also be overwritten
<<<<<<< HEAD
    protected boolean createComponent()
=======
    private boolean createComponent(ComponentContextImpl<S> componentContext)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        if ( !isStateLocked() )
        {
            throw new IllegalStateException( "need write lock (createComponent)" );
        }
        if ( m_componentContext == null )
        {
            S tmpComponent = createImplementationObject( null, new SetImplementationObject<S>()
            {
<<<<<<< HEAD
=======
                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public void presetComponentContext( ComponentContextImpl<S> componentContext )
                {
                    m_componentContext = componentContext;
                }


<<<<<<< HEAD
=======
                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public void resetImplementationObject( S implementationObject )
                {
                    m_componentContext = null;
                }
<<<<<<< HEAD
            } );
=======
            }, componentContext );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

            // if something failed creating the component instance, return false
            if ( tmpComponent == null )
            {
                return false;
            }

            // otherwise set the context and component instance and return true
<<<<<<< HEAD
            log( LogService.LOG_DEBUG, "Set implementation object for component {0}", new Object[] { getName() },  null );

            //notify that component was successfully created so any optional circular dependencies can be retried
            BundleComponentActivator activator = getActivator();
            if ( activator != null )
            {
                activator.missingServicePresent( getServiceReference() );
            }
=======
           getLogger().log( LogService.LOG_DEBUG, "Set implementation object for component", null );

            //notify that component was successfully created so any optional circular dependencies can be retried
            m_container.getActivator().missingServicePresent( getServiceReference() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return true;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    protected void deleteComponent( int reason )
    {
        if ( !isStateLocked() )
        {
            throw new IllegalStateException( "need write lock (deleteComponent)" );
        }
        if ( m_componentContext != null )
        {
            m_useCount.set( 0 );
            disposeImplementationObject( m_componentContext, reason );
<<<<<<< HEAD
            m_componentContext = null;
            log( LogService.LOG_DEBUG, "Unset and deconfigured implementation object for component {0} in deleteComponent for reason {1}", new Object[] { getName(), REASONS[ reason ] },  null );
=======
            m_componentContext.cleanup();
            m_componentContext = null;
            getLogger().log( LogService.LOG_DEBUG, "Unset and deconfigured implementation object for component in deleteComponent for reason {0}", null, REASONS[ reason ] );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            clearServiceProperties();
        }
    }

    void clearServiceProperties()
    {
        m_properties = null;
        m_serviceProperties = null;
    }


<<<<<<< HEAD
    public ComponentInstance getComponentInstance()
=======
    public ComponentInstance<S> getComponentInstance()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_componentContext == null? null: m_componentContext.getComponentInstance();
    }


    //**********************************************************************************************************

    /**
     * Get the object that is implementing this descriptor
     *
     * @return the object that implements the services
     */
<<<<<<< HEAD
    S getInstance()
=======
    private S getInstance()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_componentContext == null? null: m_componentContext.getImplementationObject( true );
    }

    /**
     * The <code>SetImplementationObject</code> interface provides an
     * API for component managers to setup the implementation object and
     * potentially other parts as part of the {@link #createImplementationObject} method
     * processing.
     */
    protected interface SetImplementationObject<S>
    {

        /**
         * Presets the implementation object. This method is called before
         * the component's activator method is called and is intended to
         * temporarily set the implementation object during the activator
         * call.
         */
        void presetComponentContext( ComponentContextImpl<S> componentContext );


        /**
         * Resets the implementation object. This method is called after
         * the activator method terminates with an error and is intended to
         * revert any temporary settings done in the {@link #presetComponentContext(ComponentContextImpl)}
         * method.
         */
        void resetImplementationObject( S implementationObject );

    }


<<<<<<< HEAD
    protected S createImplementationObject( Bundle usingBundle, SetImplementationObject setter )
    {
        final Class<S> implementationObjectClass;
        final S implementationObject;
=======
    @SuppressWarnings("unchecked")
    protected S createImplementationObject( Bundle usingBundle, SetImplementationObject<S> setter, ComponentContextImpl<S> componentContext )
    {
        S implementationObject = null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        Bundle bundle = getBundle();
        if (bundle == null)
        {
<<<<<<< HEAD
            log( LogService.LOG_WARNING, "Bundle shut down during instantiation of the implementation object", null);
            return null;
        }
        try
        {
            // 112.4.4 The class is retrieved with the loadClass method of the component's bundle
            implementationObjectClass = (Class<S>) bundle.loadClass(
                    getComponentMetadata().getImplementationClassName() )  ;

            // 112.4.4 The class must be public and have a public constructor without arguments so component instances
            // may be created by the SCR with the newInstance method on Class
            implementationObject = implementationObjectClass.newInstance();
        }
        catch ( Throwable t )
        {
            // failed to instantiate, return null
            log( LogService.LOG_ERROR, "Error during instantiation of the implementation object", t );
            return null;
        }
        
        ComponentContextImpl componentContext = new ComponentContextImpl(this, usingBundle, implementationObject);

        // 3. set the implementation object prematurely
        setter.presetComponentContext( componentContext );

        // 4. Bind the target services

        DependencyManager<S, ?> failedDm = null;
        for ( DependencyManager<S, ?> dm: getDependencyManagers())
        {
            if ( failedDm == null )
            {
                // if a dependency turned unresolved since the validation check,
                // creating the instance fails here, so we deactivate and return
                // null.
                boolean open = dm.open( implementationObject, componentContext.getEdgeInfo( dm ) );
                if ( !open )
                {
                    log( LogService.LOG_ERROR, "Cannot create component instance due to failure to bind reference {0}",
                            new Object[] { dm.getName() }, null );

                    failedDm = dm;

                }
            }
            else
            {
                componentContext.getEdgeInfo( dm ).ignore();
            }
        }
        if (failedDm != null)
        {
            // make sure, we keep no bindings. Only close the dm's we opened.
            boolean skip = true;
            for ( DependencyManager md: getReversedDependencyManagers() )
            {
                if ( skip && failedDm == md )
                {
                    skip = false;
                }
                if ( !skip )
                {
                    md.close( implementationObject, componentContext.getEdgeInfo( md ) );
                }
            }
=======
            getLogger().log( LogService.LOG_WARNING, "Bundle shut down during instantiation of the implementation object", null);
            return null;
        }

        // bind target services
        final List<DependencyManager.OpenStatus<S, ?>> openStatusList = new ArrayList<>();

        final Map<ReferenceMetadata, DependencyManager.OpenStatus<S, ?>> paramMap = ( getComponentMetadata().getNumberOfConstructorParameters() > 0 ? new HashMap<ReferenceMetadata, DependencyManager.OpenStatus<S, ?>>() : null);
        boolean failed = false;
        for ( DependencyManager<S, ?> dm : getDependencyManagers())
        {
            // if a dependency turned unresolved since the validation check,
            // creating the instance fails here, so we deactivate and return
            // null.
            DependencyManager.OpenStatus<S, ?> open = dm.open( componentContext, componentContext.getEdgeInfo( dm ) );
            if ( open == null )
            {
                getLogger().log( LogService.LOG_DEBUG, "Cannot create component instance due to failure to bind reference {0}",
                        null, dm.getName()  );

                failed = true;
                break;
            }
            openStatusList.add(open);
            if ( dm.getReferenceMetadata().getParameterIndex() != null)
            {
                if ( !dm.bindDependency(componentContext, ReferenceMethod.NOPReferenceMethod, (OpenStatus) open) )
                {
                    getLogger().log( LogService.LOG_DEBUG, "Cannot create component instance due to failure to bind reference {0}",
                            null, dm.getName()  );
                    failed = true;
                    break;
                }

                paramMap.put(dm.getReferenceMetadata(), open);
            }
        }

        if ( !failed )
        {
            try
            {
                implementationObject = getComponentMethods().getConstructor().newInstance(
                        componentContext,
                        paramMap);

            }
            catch ( final InstantiationException ie)
            {
                // we don't need to log the stack trace
                getLogger().log( LogService.LOG_ERROR, "Error during instantiation of the implementation object: " + ie.getMessage(), null );
                this.setFailureReason(ie);
                return null;
            }
            catch ( final Throwable t )
            {
                // failed to instantiate, return null
                getLogger().log( LogService.LOG_ERROR, "Error during instantiation of the implementation object", t );
                this.setFailureReason(t);
                return null;
            }

            componentContext.setImplementationObject(implementationObject);

            // 3. set the implementation object prematurely
            setter.presetComponentContext( componentContext );

            // 4. Bind the target services
            final Iterator<DependencyManager.OpenStatus<S, ?>> iter = openStatusList.iterator();
            for ( DependencyManager<S, ?> dm: getDependencyManagers())
            {
                final DependencyManager.OpenStatus<S, ?> open = iter.next();
                if ( !dm.bind(componentContext, (OpenStatus) open) )
                {
                    getLogger().log( LogService.LOG_DEBUG, "Cannot create component instance due to failure to bind reference {0}",
                            null, dm.getName()  );
                    failed = true;
                    break;
                }
            }
        }
        if (failed)
        {
            // make sure, we keep no bindings. Only close the dm's we opened.
            int skipCount = getReversedDependencyManagers().size() - openStatusList.size();
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                if ( skipCount > 0 )
                {
                    skipCount--;
                }
                else
                {
                    md.close( componentContext, componentContext.getEdgeInfo( md ) );
                }
                md.deactivate();
            }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            setter.resetImplementationObject( implementationObject );
            return null;

        }

        // 5. Call the activate method, if present
<<<<<<< HEAD
        final MethodResult result = getComponentMethods().getActivateMethod().invoke( implementationObject, new ActivatorParameter(
                componentContext, 1 ), null, this );
        if ( result == null )
        {
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            for ( DependencyManager md: getReversedDependencyManagers() )
            {
                md.close( implementationObject, componentContext.getEdgeInfo( md ) );
            }

            // make sure the implementation object is not available
            setter.resetImplementationObject( implementationObject );

           return null;
=======
        final MethodResult failedResult = new MethodResult(true, new HashMap<String, Object>());
        final MethodResult result = getComponentMethods().getActivateMethod().invoke( implementationObject,
                componentContext, 1, failedResult );
        if ( result == failedResult )
        {
            this.setFailureReason((Throwable)failedResult.getResult().get("exception"));
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                md.close( componentContext, componentContext.getEdgeInfo( md ) );
            }

            if ( implementationObject != null )
            {
                // make sure the implementation object is not available
                setter.resetImplementationObject( implementationObject );
            }

            return null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        else
        {
            componentContext.setImplementationAccessible( true );
<<<<<<< HEAD
            m_circularReferences.remove();
            //this may cause a getService as properties now match a filter.
            setServiceProperties( result );
=======
            //call to leaveCreate must be done here since the change in service properties may cause a getService,
            //so the threadLocal must be cleared first.
            m_container.getActivator().leaveCreate(getServiceReference());

            //this may cause a getService as properties now match a filter.
            setServiceProperties( result, null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( ComponentContextImpl<S> componentContext,
            int reason )
    {
        componentContext.setImplementationAccessible( false );
        S implementationObject = componentContext.getImplementationObject( false );

<<<<<<< HEAD
        // 1. Call the deactivate method, if present
        // don't care for the result, the error (acccording to 112.5.12 If the deactivate
        // method throws an exception, SCR must log an error message containing the
        // exception with the Log Service and continue) has already been logged
        final MethodResult result = getComponentMethods().getDeactivateMethod().invoke( implementationObject, new ActivatorParameter( componentContext,
                reason ), null, this );
        if ( result != null )
        {
            setServiceProperties( result );
        }

        // 2. Unbind any bound services
        for ( DependencyManager md: getReversedDependencyManagers() )
        {
            md.close( implementationObject, componentContext.getEdgeInfo( md ) );
=======
        if ( implementationObject != null )
        {
            // 1. Call the deactivate method, if present
            // don't care for the result, the error (acccording to 112.5.12 If the deactivate
            // method throws an exception, SCR must log an error message containing the
            // exception with the Log Service and continue) has already been logged
            final MethodResult result = getComponentMethods().getDeactivateMethod().invoke( implementationObject,
                    componentContext, reason, null );
            if ( result != null )
            {
                setServiceProperties( result, null );
            }
            // 2. Unbind any bound services
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                md.close( componentContext, componentContext.getEdgeInfo( md ) );
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    boolean hasInstance()
    {
        return m_componentContext != null;
    }

<<<<<<< HEAD
    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount )
=======
    @Override
    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
<<<<<<< HEAD
            final S impl = componentContext.getImplementationObject( false );
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeBindMethod( impl, refPair, trackingCount, info );
        }
    }

    <T> void invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount )
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
            final S impl = componentContext.getImplementationObject( false );
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeUpdatedMethod( impl, refPair, trackingCount, info );
        }
    }

    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> oldRefPair, int trackingCount )
=======
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeBindMethod( componentContext, refPair, trackingCount, info );
        }
    }

    @Override
    <T> boolean invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
    {
        final ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
            final EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            return dependencyManager.invokeUpdatedMethod( componentContext, refPair, trackingCount, info );
        }
        return false;
    }

    @Override
    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRefPair, int trackingCount )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
<<<<<<< HEAD
            final S impl = componentContext.getImplementationObject( false );
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeUnbindMethod( impl, oldRefPair, trackingCount, info );
        }
    }

    protected void setFactoryProperties( Dictionary<String, Object> dictionary )
    {
        m_factoryProperties = copyTo( null, dictionary );
    }


    public boolean hasConfiguration()
    {
        return m_configurationProperties != null;
    }


=======
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeUnbindMethod( componentContext, oldRefPair, trackingCount, info );
        }
    }

    protected void setFactoryProperties( Dictionary<String, ?> dictionary )
    {
        m_factoryProperties = copyToMap( dictionary, true );
    }


    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    void registerComponentId()
    {
        super.registerComponentId();
        this.m_properties = null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    void unregisterComponentId()
    {
        super.unregisterComponentId();
        this.m_properties = null;
    }


    /**
     * Returns the (private copy) of the Component properties to be used
     * for the ComponentContext as well as eventual service registration.
     * <p/>
     * Method implements the Component Properties provisioning as described
     * in 112.6, Component Properties.
     *
<<<<<<< HEAD
     * @return a private Hashtable of component properties
     */
    public Dictionary<String, Object> getProperties()
    {

        if ( m_properties == null )
        {

            // 1. the properties from the component descriptor
            Dictionary<String, Object> props = copyTo( null, getComponentMetadata().getProperties() );

            // 2. add target properties of references
            // 112.6 Component Properties, target properties (p. 302)
            for ( ReferenceMetadata rm : getComponentMetadata().getDependencies() )
            {
                if ( rm.getTarget() != null )
                {
                    props.put( rm.getTargetPropertyName(), rm.getTarget() );
                }
            }

            // 3. overlay with Configuration Admin properties
            copyTo( props, m_configurationProperties );

            // 4. copy any component factory properties, not supported yet
            copyTo( props, m_factoryProperties );

            // 5. set component.name and component.id
=======
     * @return a private map of component properties
     */
    @Override
    public Map<String, Object> getProperties()
    {
        if ( m_properties == null )
        {
            // 1. Merge all the config properties
            Map<String, Object> props = new HashMap<>();
            if ( m_configurationProperties != null )
            {
                props.putAll(m_configurationProperties);
            }
            if ( m_factoryProperties != null)
            {
                props.putAll(m_factoryProperties);
                if (getComponentMetadata().getDSVersion().isDS13() && m_factoryProperties.containsKey(Constants.SERVICE_PID))
                {
                    final List<String> servicePids = new ArrayList<>();
                    final Object configPropServicePids = m_configurationProperties.get(Constants.SERVICE_PID);
                    if ( configPropServicePids instanceof List )
                    {
                        servicePids.addAll((List)configPropServicePids);
                    }
                    else
                    {
                        servicePids.add(configPropServicePids.toString());
                    }
                    if (m_factoryProperties.get(Constants.SERVICE_PID) instanceof String)
                    {
                        servicePids.add((String)m_factoryProperties.get(Constants.SERVICE_PID));
                    }

                    if ( servicePids.size() == 1 )
                    {
                        props.put(Constants.SERVICE_PID, servicePids.get(0));
                    }
                    else
                    {
                        props.put(Constants.SERVICE_PID, servicePids);
                    }
                }
            }

            // 2. set component.name and component.id
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            props.put( ComponentConstants.COMPONENT_ID, getId() );

            m_properties = props;
        }

        return m_properties;
    }

<<<<<<< HEAD
    public void setServiceProperties( Dictionary<String, Object> serviceProperties )
=======
    @Override
    public void setServiceProperties( Dictionary<String, ?> serviceProperties )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        if ( serviceProperties == null || serviceProperties.isEmpty() )
        {
            m_serviceProperties = null;
        }
        else
        {
<<<<<<< HEAD
            m_serviceProperties = copyTo( null, serviceProperties, false );
=======
            m_serviceProperties = copyToDictionary( serviceProperties, false );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            // set component.name and component.id
            m_serviceProperties.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            m_serviceProperties.put( ComponentConstants.COMPONENT_ID, getId() );
        }

        updateServiceRegistration();
    }

<<<<<<< HEAD
=======
    @Override
    void postRegister()
    {
        if (m_serviceProperties != null)
        {
            updateServiceRegistration();
        }
    }

    @Override
    void preDeregister()
    {
        if (m_componentContext != null)
        {
            m_componentContext.unsetServiceRegistration();
        }
    }

    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Dictionary<String, Object> getServiceProperties()
    {
        if ( m_serviceProperties != null )
        {
            return m_serviceProperties;
        }
        return super.getServiceProperties();
    }

<<<<<<< HEAD
    private void updateServiceRegistration()
    {
        ServiceRegistration<?> sr = getServiceRegistration();
=======
    final ServiceReference<S> getServiceReference()
    {
        ServiceRegistration<S> reg = getServiceRegistration();
        if (reg != null)
        {
            return reg.getReference();
        }
        return null;
    }

    @Override
    protected ServiceRegistration<S> getServiceRegistration()
    {
        if ( getComponentMetadata().getDSVersion() == DSVersion.DS12Felix )
        {
            return m_componentContext != null ? m_componentContext.getServiceRegistration() : null;
        }
        return super.getServiceRegistration();
    }

    private void updateServiceRegistration()
    {
        ServiceRegistration<S> sr = getServiceRegistration();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ( sr != null )
        {
            try
            {
                // Don't propagate if service properties did not change.
                final Dictionary<String, Object> regProps = getServiceProperties();
                if ( !servicePropertiesMatches( sr, regProps ) )
                {
                    sr.setProperties( regProps );
                }
<<<<<<< HEAD
            }
            catch ( IllegalStateException ise )
            {
                // service has been unregistered asynchronously, ignore
            }
            catch ( IllegalArgumentException iae )
            {
                log( LogService.LOG_ERROR,
                        "Unexpected configuration property problem when updating service registration", iae );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Unexpected problem when updating service registration", t );
            }
        }
=======
                else
                {
                    getLogger().log( LogService.LOG_DEBUG, "Not updating service registration, no change in properties", null );
                }
            }
            catch ( final IllegalStateException ise )
            {
                // service has been unregistered asynchronously, ignore
            }
            catch ( final IllegalArgumentException iae )
            {
                getLogger().log( LogService.LOG_ERROR,
                        "Unexpected configuration property problem when updating service registration", iae );
            }
            catch ( final Throwable t )
            {
                getLogger().log( LogService.LOG_ERROR, "Unexpected problem when updating service registration", t );
            }
        }
        else
        {
            getLogger().log( LogService.LOG_DEBUG, "No service registration to update", null );
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Called by the Configuration Admin Service to update the component with
     * Configuration properties.
     * <p/>
     * This causes the component to be reactivated with the new configuration
     * unless no configuration has ever been set on this component and the
     * <code>configuration</code> parameter is <code>null</code>. In this case
     * nothing is to be done. If a configuration has previously been set and
     * now the configuration is deleted, the <code>configuration</code>
     * parameter is <code>null</code> and the component has to be reactivated
     * with the default configuration.
     *
     * @param configuration The configuration properties for the component from
     *                      the Configuration Admin Service or <code>null</code> if there is
     *                      no configuration or if the configuration has just been deleted.
<<<<<<< HEAD
     * @param changeCount Change count for the configuration
     * @param targetedPID TargetedPID for the configuration
     */
    public void reconfigure( Dictionary<String, Object> configuration, long changeCount, TargetedPID targetedPID )
    {
        CountDownLatch enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if ( targetedPID == null || !targetedPID.equals( m_targetedPID ) )
            {
                m_targetedPID = targetedPID;
                m_changeCount = -1;
            }
            if ( configuration != null )
            {
                if ( changeCount <= m_changeCount )
                {
                    log( LogService.LOG_DEBUG,
                            "ImmediateComponentHolder out of order configuration updated for pid {0} with existing count {1}, new count {2}",
                            new Object[] { getConfigurationPid(), m_changeCount, changeCount }, null );
                    return;
                }
                m_changeCount = changeCount;
            }
            else 
            {
                m_changeCount = -1;
            }
            // nothing to do if there is no configuration (see FELIX-714)
            if ( configuration == null && m_configurationProperties == null )
            {
                log( LogService.LOG_DEBUG, "No configuration provided (or deleted), nothing to do", null );
                return;
            }

            // store the properties
            m_configurationProperties = configuration;

            // clear the current properties to force using the configuration data
            m_properties = null;

            
            // reactivate the component to ensure it is provided with the
            // configuration data
            if ( m_disposed || !m_internalEnabled )
            {
                // nothing to do for inactive components, leave this method
                log( LogService.LOG_DEBUG, "Component can not be activated due to configuration in state {0}", new Object[] { getState() }, null );
=======
     * @param configurationDeleted TODO
     * @param factoryPid TODO
     */
    @Override
    public void reconfigure( Map<String, Object> configuration, boolean configurationDeleted, TargetedPID factoryPid)
    {
        // store the properties
        m_configurationProperties = configuration;

        reconfigure(configurationDeleted);
    }

    void reconfigure(boolean configurationDeleted)
    {
        Deferred<Void> enableLatch = enableLatchWait();
        try
        {
            // clear the current properties to force using the configuration data
            m_properties = null;


            // reactivate the component to ensure it is provided with the
            // configuration data
            if ( !getState().isEnabled() )
            {
                // nothing to do for inactive components, leave this method
                getLogger().log( LogService.LOG_DEBUG, "Component can not be activated since it is in state {0}", null, getState() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                //enabling the component will set the target properties, do nothing now.
                return;
            }

<<<<<<< HEAD
            // if the configuration has been deleted but configuration is required
            // this component must be deactivated
            if ( configuration == null && getComponentMetadata().isConfigurationRequired() )
            {
                //deactivate and remove service listeners
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED, true, false );
                //do not reset targets as that will reinstall the service listeners which may activate the component.
                //when a configuration arrives the properties will get set based on the new configuration.
                return;
            }

            // unsatisfied component and non-ignored configuration may change targets
            // to satisfy references
            obtainActivationWriteLock( "reconfigure" );
            try
            {
                if ( getState() == STATE_UNSATISFIED
                        && !getComponentMetadata().isConfigurationIgnored() )
                {
                    log( LogService.LOG_DEBUG, "Attempting to activate unsatisfied component", null );
                    updateTargets( getProperties() );
                    releaseActivationWriteeLock( "reconfigure.unsatisfied" );
                    activateInternal( getTrackingCount().get() );
                    return;
                }

                if ( !modify() )
                {
                    // SCR 112.7.1 - deactivate if configuration is deleted or no modified method declared
                    log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure from configuration", null );
                    int reason = ( configuration == null ) ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
=======
            // unsatisfied component and non-ignored configuration may change targets
            // to satisfy references
            obtainActivationWriteLock( );
            try
            {
                if ( !getState().isSatisfied() && !getComponentMetadata().isConfigurationIgnored() )
                {
                    getLogger().log( LogService.LOG_DEBUG, "Attempting to activate unsatisfied component", null );
                    updateTargets( getProperties() );
                    releaseActivationWriteeLock(  );
                    activateInternal( );
                    return;
                }

                if ( !modify(configurationDeleted) )
                {
                    // SCR 112.7.1 - deactivate if configuration is deleted or no modified method declared
                    getLogger().log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure from configuration", null );
                    int reason = configurationDeleted ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;

                    // FELIX-2368: cycle component immediately, reconfigure() is
                    //     called through ConfigurationListener API which itself is
                    //     called asynchronously by the Configuration Admin Service
<<<<<<< HEAD
                    releaseActivationWriteeLock( "reconfigure.modified.1" );;
                    deactivateInternal( reason, false, false );
                    obtainActivationWriteLock( "reconfigure.deactivate.activate" );
                    try
                    {
                        updateTargets( getProperties() );
                    }
                    finally
                    {
                        releaseActivationWriteeLock( "reconfigure.deactivate.activate" );;
                    }
                    activateInternal( getTrackingCount().get() );
=======
                    releaseActivationWriteeLock(  );
                    //we have already determined that modify cannot be called. Therefore factory instances must be disposed.
                    boolean dispose = m_factoryInstance;
                    deactivateInternal( reason, dispose, dispose );
                    if ( !dispose )
                    {
                        obtainActivationWriteLock();
                        try
                        {
                            updateTargets(getProperties());
                        }
                        finally
                        {
                            releaseActivationWriteeLock();
                        }
                        activateInternal();
                    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
            finally
            {
                //used if modify succeeds or if there's an exception.
<<<<<<< HEAD
                releaseActivationWriteeLock( "reconfigure.end" );;
=======
                releaseActivationWriteeLock(  );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        finally
        {
<<<<<<< HEAD
            enableLatch.countDown();
        }
    }

    private boolean modify()
    {
        // 1. no live update if there is no declared method
        if ( getComponentMetadata().getModified() == null )
        {
            log( LogService.LOG_DEBUG, "No modified method, cannot update dynamically", null );
=======
            enableLatch.resolve(null);
        }
    }

    private boolean modify(boolean configurationDeleted)
    {
        //0 SCR 112.7.1 If configuration is deleted, and version is < 1.3 and no flag set, then deactivate unconditionally.
        // For version 1.3 and later, or with a flag, more sensible behavior is allowed.
        if ( configurationDeleted && !getComponentMetadata().isDeleteCallsModify()){
            return false;
        }

        // 1. no live update if there is no declared method
        if ( getComponentMetadata().getModified() == null )
        {
            getLogger().log( LogService.LOG_DEBUG, "No modified method, cannot update dynamically", null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            return false;
        }
        // invariant: we have a modified method name

        // 2. get and check configured method
        // invariant: modify method is configured and found

        // 3. check whether we can dynamically apply the configuration if
        // any target filters influence the bound services
<<<<<<< HEAD
        final Dictionary<String, Object> props = getProperties();
        for ( DependencyManager dm: getDependencyManagers() )
        {
            if ( !dm.canUpdateDynamically( props ) )
            {
                log( LogService.LOG_DEBUG,
                        "Cannot dynamically update the configuration due to dependency changes induced on dependency {0}",
                        new Object[] {dm.getName()}, null );
=======
        final Map<String, Object> props = getProperties();
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            if ( !dm.canUpdateDynamically( props ) )
            {
                getLogger().log( LogService.LOG_DEBUG,
                        "Cannot dynamically update the configuration due to dependency changes induced on dependency {0}",
                        null, dm.getName() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                return false;
            }
        }
        // invariant: modify method existing and no static bound service changes

        // 4. call method (nothing to do when failed, since it has already been logged)
        //   (call with non-null default result to continue even if the
        //    modify method call failed)
<<<<<<< HEAD
        obtainStateLock( "ImmediateComponentManager.modify" );
=======
        obtainStateLock(  );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        try
        {
            //cf 112.5.12 where invoking modified method before updating target services is specified.
            final MethodResult result = invokeModifiedMethod();
            updateTargets( props );
            if ( result == null )
            {
                // log an error if the declared method cannot be found
<<<<<<< HEAD
                log( LogService.LOG_ERROR, "Declared modify method ''{0}'' cannot be found, configuring by reactivation",
                        new Object[] {getComponentMetadata().getModified()}, null );
=======
                getLogger().log( LogService.LOG_ERROR, "Declared modify method ''{0}'' cannot be found, configuring by reactivation",
                        null, getComponentMetadata().getModified() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                return false;
            }

            // 5. update the target filter on the services now, this may still
            // result in unsatisfied dependencies, in which case we abort
            // this dynamic update and have the component be deactivated
            if ( !verifyDependencyManagers() )
            {
<<<<<<< HEAD
                log( LogService.LOG_ERROR,
                        "Updating the service references caused at least on reference to become unsatisfied, deactivating component",
=======
                getLogger().log( LogService.LOG_DEBUG,
                        "Updating the service references caused at least one reference to become unsatisfied, deactivating component",
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        null );
                return false;
            }

            // 6. update service registration properties if we didn't just do it
            if ( result.hasResult() )
            {
<<<<<<< HEAD
                setServiceProperties( result );
=======
                setServiceProperties( result, null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
            else
            {
                updateServiceRegistration();
            }

            // 7. everything set and done, the component has been updated
            return true;
        }
        finally
        {
<<<<<<< HEAD
            releaseStateLock( "ImmediateComponentManager.modify" );
=======
            releaseStateLock(  );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    protected MethodResult invokeModifiedMethod()
    {
<<<<<<< HEAD
        ModifiedMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        if ( getInstance() != null )
        {
            return modifiedMethod.invoke( getInstance(), new ActivatorParameter( m_componentContext, -1 ),
                    MethodResult.VOID, this );
=======
        LifecycleMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        if ( getInstance() != null )
        {
            return modifiedMethod.invoke( getInstance(), m_componentContext, -1,
                    MethodResult.VOID );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return MethodResult.VOID;
    }

    /**
     * Checks if the given service registration properties matches another set
     * of properties.
     *
     * @param reg   the service registration whose service properties will be
     *              compared to the props parameter
     * @param props the properties to be compared with the registration
     *              service properties.
     * @return <code>true</code> if the registration service properties equals
     *         the prop properties, false if not.
     */
<<<<<<< HEAD
    private boolean servicePropertiesMatches( ServiceRegistration reg, Dictionary<String, Object> props )
    {
        Dictionary<String, Object> regProps = new Hashtable<String, Object>();
=======
    private boolean servicePropertiesMatches( ServiceRegistration<S> reg, Dictionary<String, Object> props )
    {
        Dictionary<String, Object> regProps = new Hashtable<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        String[] keys = reg.getReference().getPropertyKeys();
        for ( int i = 0; keys != null && i < keys.length; i++ )
        {
            if ( !keys[i].equals( org.osgi.framework.Constants.OBJECTCLASS )
                    && !keys[i].equals( org.osgi.framework.Constants.SERVICE_ID ) )
            {
                regProps.put( keys[i], reg.getReference().getProperty( keys[i] ) );
            }
        }
        return regProps.equals( props );
    }

<<<<<<< HEAD
    public S getService( Bundle bundle, ServiceRegistration<S> serviceRegistration )
    {
        boolean success = getServiceInternal();
        if ( success )
        {
            m_useCount.incrementAndGet();
            return m_componentContext.getImplementationObject( true );
        }
        else
        {
            return null;
        }
    }

    
    @Override
    boolean getServiceInternal()
    {
        if (m_circularReferences.get() != null)
        {
            log( LogService.LOG_ERROR,  "Circular reference detected, getService returning null", null );
            dumpThreads();
            return false;             
        }
        m_circularReferences.set( Boolean.TRUE );
        try
        {
            boolean success = true;
            if ( m_componentContext == null )
            {
                try
                {
                    if ( !collectDependencies() )
                    {
                        log(
                                LogService.LOG_DEBUG,
                                "getService did not win collecting dependencies, try creating object anyway.",
                                null );

                    }
                    else
                    {
                        log(
                                LogService.LOG_DEBUG,
                                "getService won collecting dependencies, proceed to creating object.",
                                null );

                    }
                }
                catch ( IllegalStateException e )
                {
                    log(
                            LogService.LOG_INFO,
                            "Could not obtain all required dependencies, getService returning null",
                            null );
                    success = false;
                }
                obtainStateLock( "ImmediateComponentManager.getService.1" );
                try
                {
                    if ( m_componentContext == null )
                    {
                        //state should be "Registered"
                        S result = getService( );
                        if ( result == null )
                        {
                            success = false;;
                        }
                        else
                        {
                            m_activated = true;
                        }
                    }
                }
                finally
                {
                    releaseStateLock( "ImmediateComponentManager.getService.1" );
                }
            }
            return success;
        }
        finally
        {
            //normally this will have been done after object becomes accessible.  This is double-checking.
            m_circularReferences.remove();
        }
    }

    private S getService()
    {
        //should be write locked
        if (!isEnabled())
=======
    @Override
    public S getService( Bundle bundle, ServiceRegistration<S> serviceRegistration )
    {
        if ( m_container.getActivator().enterCreate( serviceRegistration.getReference() ) )
        {
            return null;
        }
        obtainStateLock();
        try
        {
            m_useCount.incrementAndGet();
        }
        finally
        {
            releaseStateLock( );
        }
        boolean decrement = true;
        try
        {
            try
            {
                boolean success = getServiceInternal(serviceRegistration);
                ComponentContextImpl<S> componentContext = m_componentContext;
                if ( success && componentContext != null)
                {
                    decrement = false;
                    return componentContext.getImplementationObject( true );
                }
                else
                {
                    return null;
                }
            }
            finally
            {
                //This is backup.  Normally done in createComponent.
                m_container.getActivator().leaveCreate(serviceRegistration.getReference());
            }

        }
        finally
        {
            if ( decrement )
            {
                ungetService( bundle, serviceRegistration, null );
            }
        }
    }


    @Override
    boolean getServiceInternal(ServiceRegistration<S> serviceRegistration)
    {
        boolean success = true;
        if ( m_componentContext == null )
        {
            ComponentContextImpl<S> componentContext = new ComponentContextImpl<>(this, this.getBundle(), serviceRegistration);
            if ( collectDependencies(componentContext))
            {
                getLogger().log( LogService.LOG_DEBUG,
                        "getService (single component manager) dependencies collected.",
                        null );
            }
            else
            {
                getLogger().log( LogService.LOG_INFO,
                        "Could not obtain all required dependencies, getService returning null",
                        null );
                return false;
            }
            obtainStateLock(  );
            try
            {
                if ( m_componentContext == null )
                {
                    State previousState = getState();
                    //state should be "Registered"
                    S result = getService(componentContext );
                    if ( result == null )
                    {
                        success = false;
                        setState(previousState, State.failed);
                    }
                    else
                    {
                        setState(previousState, State.active);
                    }
                }
            }
            finally
            {
                releaseStateLock(  );
            }
        }
        return success;
    }

    private S getService(ComponentContextImpl<S> componentContext)
    {
        //should be write locked
        if (!getState().isEnabled())
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            return null;
        }

<<<<<<< HEAD
        if ( createComponent() )
=======
        if ( createComponent(componentContext) )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            return getInstance();
        }

        // log that the delayed component cannot be created (we don't
        // know why at this moment; this should already have been logged)
<<<<<<< HEAD
        log( LogService.LOG_ERROR, "Failed creating the component instance; see log for reason", null );
=======
        getLogger().log( LogService.LOG_DEBUG, "Failed creating the component instance; see log for reason", null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // component could not really be created. This may be temporary
        // so we stay in the registered state but ensure the component
        // instance is deleted
        try
        {
            deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
        }
<<<<<<< HEAD
        catch ( Throwable t )
        {
            log( LogService.LOG_DEBUG, "Cannot delete incomplete component instance. Ignoring.", t );
=======
        catch ( final Throwable t )
        {
            getLogger().log( LogService.LOG_DEBUG, "Cannot delete incomplete component instance. Ignoring.", t );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        // no service can be returned (be prepared for more logging !!)
        return null;

    }

<<<<<<< HEAD
    public void ungetService( Bundle bundle, ServiceRegistration<S> serviceRegistration, S o )
    {
        // the framework should not call ungetService more than it calls
        // calls getService. Still, we want to be sure to not go below zero
        if ( m_useCount.get() > 0 )
        {
            int useCount = m_useCount.decrementAndGet();

            // unget the service instance if no bundle is using it
            // any longer unless delayed component instances have to
            // be kept (FELIX-3039)
            if ( useCount == 0 && !isImmediate() && !keepInstances() )
            {
                obtainStateLock( "ImmediateComponentManager.ungetService.1" );
                try
                {
                    if ( m_useCount.get() == 0 )
                    {
                        ungetService( );
                        unsetDependenciesCollected();
                    }
                }
                finally
                {
                    releaseStateLock( "ImmediateComponentManager.ungetService.1" );
                }
            }
        }
    }

    void ungetService( )
    {
        deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
=======
    @Override
    public void ungetService( Bundle bundle, ServiceRegistration<S> serviceRegistration, S o )
    {
        obtainStateLock( );
        try
        {
            // unget the service instance if no bundle is using it
            // any longer unless delayed component instances have to
            // be kept (FELIX-3039)
            if (  m_useCount.decrementAndGet() == 0 && !isImmediate() && !keepInstances() )
            {
                State previousState = getState();
                deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
                setState(previousState, State.satisfied);
            }
        }
        finally
        {
            releaseStateLock(  );
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    private boolean keepInstances()
    {
<<<<<<< HEAD
        return getActivator() != null && getActivator().getConfiguration().keepInstances();
    }

    public long getChangeCount()
    {
        return m_changeCount;
    }

    public TargetedPID getConfigurationTargetedPID()
    {
        return m_targetedPID;
=======
        return getComponentMetadata().isDelayedKeepInstances();
    }

    @Override
    public void getComponentManagers(List<AbstractComponentManager<S>> cms)
    {
        cms.add(this);
    }

    @Override
    public ServiceReference<S> getRegisteredServiceReference()
    {
        final ComponentContextImpl<S> ctx = this.m_componentContext;
        if ( ctx != null )
        {
            return ctx.getServiceReference();
        }
        return null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
