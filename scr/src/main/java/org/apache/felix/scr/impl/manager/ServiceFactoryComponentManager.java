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


import java.util.ArrayList;
import java.util.Collection;
<<<<<<< HEAD
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.helper.ActivateMethod;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ModifiedMethod;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
=======
import java.util.Dictionary;
import java.util.IdentityHashMap;

import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * The <code>ServiceFactoryComponentManager</code> for components specified with &lt;service serviceFactory='true'/&gt;
 * in the xml metadata. The component must be delayed, not immediate or factory.
 */
public class ServiceFactoryComponentManager<S> extends SingleComponentManager<S>
{

<<<<<<< HEAD
    // maintain the map of ComponentContext objects created for the
    // service instances
    private IdentityHashMap<S, ComponentContextImpl> serviceContexts = new IdentityHashMap<S, ComponentContextImpl>();

    /**
     * @param activator BundleComponentActivator for this DS implementation
	 * @param componentHolder ComponentHolder for configuration management
     * @param metadata ComponentMetadata for this component
     * @param componentMethods
     */
    public ServiceFactoryComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
            ComponentMetadata metadata, ComponentMethods componentMethods )
    {
        super( activator, componentHolder, metadata, componentMethods );
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#createComponent()
     */
    protected boolean createComponent()
    {
        // nothing to do, this is handled by getService
        return true;
=======
    @Override
    public void setServiceProperties(Dictionary<String, ?> serviceProperties)
    {
        throw new IllegalStateException( "Bundle or instance scoped service properties are immutable" );
    }


    @Override
    void postRegister()
    {
        // do nothing
    }

    // maintain the map of ComponentContext objects created for the
    // service instances
    private IdentityHashMap<S, ComponentContextImpl<S>> serviceContexts = new IdentityHashMap<>();

    /**
     * @param container ComponentHolder for configuration management
     * @param componentMethods
     */
    public ServiceFactoryComponentManager( ComponentContainer<S> container, ComponentMethods<S> componentMethods )
    {
        super( container, componentMethods );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#deleteComponent()
     */
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
<<<<<<< HEAD
        for (ComponentContextImpl componentContext: getComponentContexts() )
        {
            disposeImplementationObject( componentContext, reason );
            log( LogService.LOG_DEBUG, "Unset implementation object for component {0} in deleteComponent for reason {1}", new Object[] { getName(), REASONS[ reason ] },  null );
=======
        for (ComponentContextImpl<S> componentContext: getComponentContexts() )
        {
            disposeImplementationObject( componentContext, reason );
            getLogger().log( LogService.LOG_DEBUG, "Unset implementation object for component in deleteComponent for reason {0}",
                    null, REASONS[ reason ] );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        serviceContexts.clear();
        clearServiceProperties();
    }


    /* (non-Javadoc)
<<<<<<< HEAD
     * @see org.apache.felix.scr.AbstractComponentManager#getInstance()
     */
    S getInstance()
    {
        // this method is not expected to be called as the base call is
        // overwritten in the ComponentContextImpl class
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public S getService( Bundle bundle, ServiceRegistration<S> registration )
    {
        log( LogService.LOG_DEBUG, "ServiceFactory.getService()", null );

        // When the getServiceMethod is called, the implementation object must be created

        try
        {
            if ( !collectDependencies() )
            {
                log(
                        LogService.LOG_INFO,
                        "getService (ServiceFactory) did not win collecting dependencies, try creating object anyway.",
                        null );

            }
            else
            {
                log(
                        LogService.LOG_DEBUG,
                        "getService (ServiceFactory) won collecting dependencies, proceed to creating object.",
                        null );

            }
        }
        catch ( IllegalStateException e )
=======
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    @Override
    public S getService( Bundle bundle, ServiceRegistration<S> serviceRegistration )
    {
        getLogger().log( LogService.LOG_DEBUG, "ServiceFactory.getService()", null );

        // When the getServiceMethod is called, the implementation object must be created

        ComponentContextImpl<S> componentContext = new ComponentContextImpl<>(this, bundle, serviceRegistration);
        if (collectDependencies(componentContext) )
        {
            getLogger().log( LogService.LOG_DEBUG,
                    "getService (ServiceFactory) dependencies collected.",
                    null );

        }
        else
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            //cannot obtain service from a required reference
            return null;
        }
<<<<<<< HEAD
        // private ComponentContext and implementation instances
        S service = createImplementationObject( bundle, new SetImplementationObject<S>()
        {
=======
        State previousState = getState();
        // private ComponentContext and implementation instances
        S service = createImplementationObject( bundle, new SetImplementationObject<S>()
        {
            @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            public void presetComponentContext( ComponentContextImpl<S> componentContext )
            {
                synchronized ( serviceContexts )
                {
                    serviceContexts.put( componentContext.getImplementationObject( false ), componentContext );
                }
            }

<<<<<<< HEAD
=======
            @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            public void resetImplementationObject( S implementationObject )
            {
                synchronized ( serviceContexts )
                {
                    serviceContexts.remove( implementationObject );
                }
            }

<<<<<<< HEAD
        } );

        // register the components component context if successfull
=======
        }, componentContext );

        // register the components component context if successful
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ( service == null )
        {
            // log that the service factory component cannot be created (we don't
            // know why at this moment; this should already have been logged)
<<<<<<< HEAD
            log( LogService.LOG_ERROR, "Failed creating the component instance; see log for reason", null );
=======
            getLogger().log( LogService.LOG_DEBUG, "Failed creating the component instance; see log for reason", null );
            setState(previousState, State.failed);

        }
        else
        {
            setState(previousState, State.active);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        return service;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
<<<<<<< HEAD
    public void ungetService( Bundle bundle, ServiceRegistration<S> registration, S service )
    {
        log( LogService.LOG_DEBUG, "ServiceFactory.ungetService()", null );
=======
    @Override
    public void ungetService( Bundle bundle, ServiceRegistration<S> registration, S service )
    {
        getLogger().log( LogService.LOG_DEBUG, "ServiceFactory.ungetService()", null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // When the ungetServiceMethod is called, the implementation object must be deactivated
        // private ComponentContext and implementation instances
        final ComponentContextImpl<S> serviceContext;
        synchronized ( serviceContexts )
        {
            serviceContext = serviceContexts.get( service );
        }
        disposeImplementationObject( serviceContext, ComponentConstants.DEACTIVATION_REASON_DISPOSED );
        synchronized ( serviceContexts )
        {
            serviceContexts.remove( service );
            // if this was the last use of the component, go back to REGISTERED state
<<<<<<< HEAD
            if ( serviceContexts.isEmpty() && getState() == STATE_ACTIVE )
            {
                unsetDependenciesCollected();
=======
            State previousState;
            if ( serviceContexts.isEmpty() && (previousState = getState()) == State.active )
            {
                setState(previousState, State.satisfied);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    private Collection<ComponentContextImpl> getComponentContexts()
    {
        synchronized ( serviceContexts )
        {
            return new ArrayList<ComponentContextImpl>( serviceContexts.values() );
        }
    }

    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount )
    {
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            dependencyManager.invokeBindMethod( cc.getImplementationObject( false ), refPair, trackingCount, cc.getEdgeInfo( dependencyManager ) );
        }
    }

    <T> void invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount )
    {
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            dependencyManager.invokeUpdatedMethod( cc.getImplementationObject( false ), refPair, trackingCount, cc.getEdgeInfo( dependencyManager ) );
        }
    }

    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> oldRefPair, int trackingCount )
    {
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            dependencyManager.invokeUnbindMethod( cc.getImplementationObject( false ), oldRefPair, trackingCount, cc.getEdgeInfo( dependencyManager ) );
        }
    }

    protected MethodResult invokeModifiedMethod()
    {
        ModifiedMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        MethodResult result = MethodResult.VOID;
        for ( ComponentContextImpl componentContext : getComponentContexts() )
        {
            Object instance = componentContext.getImplementationObject(true);
            result = modifiedMethod.invoke( instance,
                    new ActivateMethod.ActivatorParameter( componentContext, -1 ), MethodResult.VOID, this );
=======
    private Collection<ComponentContextImpl<S>> getComponentContexts()
    {
        synchronized ( serviceContexts )
        {
            return new ArrayList<>( serviceContexts.values() );
        }
    }

    @Override
    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
    {
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            dependencyManager.invokeBindMethod( cc, refPair, trackingCount, cc.getEdgeInfo( dependencyManager ) );
        }
    }

    @Override
    <T> boolean invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
    {
        // as all instances are treated the same == have the same updated signatures for methods/fields
        // we just need one result
        boolean reactivate = false;
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            if ( dependencyManager.invokeUpdatedMethod( cc, refPair, trackingCount, cc.getEdgeInfo( dependencyManager ) ) )
            {
                reactivate = true;
            }
        }
        return reactivate;
    }

    @Override
    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRefPair, int trackingCount )
    {
        for ( ComponentContextImpl<S> cc : getComponentContexts() )
        {
            dependencyManager.invokeUnbindMethod( cc, oldRefPair, trackingCount, cc.getEdgeInfo( dependencyManager ) );
        }
    }

    @Override
    protected MethodResult invokeModifiedMethod()
    {
        LifecycleMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        MethodResult result = MethodResult.VOID;
        for ( ComponentContextImpl<S> componentContext : getComponentContexts() )
        {
            S instance = componentContext.getImplementationObject(true);
            result = modifiedMethod.invoke( instance,
                    componentContext, -1, MethodResult.VOID );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        }
        return result;
    }
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    @Override
    boolean hasInstance()
    {
        return !serviceContexts.isEmpty();
    }

    //---------- Component interface

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public ComponentInstance getComponentInstance()
    {
        // TODO: should return the component instance corresponding to the
        // bundle owning ScrService
        return super.getComponentInstance();
    }

}
