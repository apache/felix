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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * The <code>ServiceFactoryComponentManager</code> for components specified with &lt;service serviceFactory='true'/&gt;
 * in the xml metadata. The component must be delayed, not immediate or factory.
 */
public class ServiceFactoryComponentManager<S> extends ImmediateComponentManager<S>
{

    // maintain the map of ComponentContext objects created for the
    // service instances
    private IdentityHashMap<S, BundleComponentContext> serviceContexts = new IdentityHashMap<S, BundleComponentContext>();

    // pseudo map of implementation objects to be used for service
    // binding while calling the activate method. The map's keys and values
    // are just the implementation objects. The objects will only be
    // contained while the activate method is being called.
    private IdentityHashMap<S, BundleComponentContext> tmpImplementationObjects = new IdentityHashMap<S, BundleComponentContext>();

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
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#deleteComponent()
     */
    protected void deleteComponent( int reason )
    {
        if ( !isWriteLocked() )
        {
            throw new IllegalStateException( "need write lock (deleteComponent)" );
        }
        for (Iterator i = serviceContexts.values().iterator(); i.hasNext(); )
        {
            BundleComponentContext componentContext = ( BundleComponentContext ) i.next();
            i.remove();
            disposeImplementationObject( componentContext.getInstance(), componentContext, reason );
            log( LogService.LOG_DEBUG, "Unset implementation object for component {0} in deleteComponent", new Object[] { getName() },  null );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.felix.scr.AbstractComponentManager#getInstance()
     */
    Object getInstance()
    {
        // this method is not expected to be called as the base call is
        // overwritten in the BundleComponentContext class
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
        {
            //cannot obtain service from a required reference
            return null;
        }
        // private ComponentContext and implementation instances
        final BundleComponentContext serviceContext = new BundleComponentContext( this, bundle );
        S service = createImplementationObject( serviceContext, new SetImplementationObject<S>()
        {
            public void presetImplementationObject( S implementationObject )
            {
                serviceContext.setImplementationObject( implementationObject );
                tmpImplementationObjects.put( implementationObject, serviceContext );

            }


            public void setImplementationObject( S implementationObject )
            {
                serviceContexts.put( implementationObject, serviceContext );
                tmpImplementationObjects.remove( implementationObject );

                // if this is the first use of this component, switch to ACTIVE state
                if ( getState() == STATE_REGISTERED )
                {
                    changeState( Active.getInstance() );
                }
            }


            public void resetImplementationObject( S implementationObject )
            {
                tmpImplementationObjects.remove( implementationObject );
                serviceContext.setImplementationObject( null );
            }

        } );

        // register the components component context if successfull
        if ( service == null )
        {
            // log that the service factory component cannot be created (we don't
            // know why at this moment; this should already have been logged)
            log( LogService.LOG_ERROR, "Failed creating the component instance; see log for reason", null );
        }

        return service;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService( Bundle bundle, ServiceRegistration<S> registration, S service )
    {
        log( LogService.LOG_DEBUG, "ServiceFactory.ungetService()", null );

        // When the ungetServiceMethod is called, the implementation object must be deactivated
        // private ComponentContext and implementation instances
        final ComponentContext serviceContext;
        serviceContext = ( ComponentContext ) serviceContexts.remove( service );

        disposeImplementationObject( service, serviceContext, ComponentConstants.DEACTIVATION_REASON_DISPOSED );

        // if this was the last use of the component, go back to REGISTERED state
        if ( serviceContexts.isEmpty() && getState() == STATE_ACTIVE )
        {
            changeState( Registered.getInstance() );
            unsetDependencyMap();
        }
    }

    <T> void update( DependencyManager<S, T> dependencyManager, ServiceReference<T> ref )
    {
        for ( S implementationObject : serviceContexts.keySet() )
        {
            dependencyManager.update( implementationObject, ref );
        }
    }

    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, ServiceReference<T> reference )
    {
        for ( S implementationObject : serviceContexts.keySet() )
        {
            dependencyManager.invokeBindMethod( implementationObject, reference );
        }
        for ( S implementationObject : tmpImplementationObjects.keySet() )
        {
            dependencyManager.invokeBindMethod( implementationObject, reference );
        }
    }

    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, ServiceReference<T> oldRef )
    {
        for ( S implementationObject : serviceContexts.keySet() )
        {
            dependencyManager.invokeUnbindMethod( implementationObject, oldRef );
        }
        for ( S implementationObject : tmpImplementationObjects.keySet() )
        {
            dependencyManager.invokeUnbindMethod( implementationObject, oldRef );
        }
    }

    protected MethodResult invokeModifiedMethod()
    {
        ModifiedMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        MethodResult result = null;
        for ( BundleComponentContext componentContext : serviceContexts.values() )
        {
            Object instance = componentContext.getInstance();
            result = modifiedMethod.invoke( instance,
                    new ActivateMethod.ActivatorParameter( componentContext, -1 ), MethodResult.VOID );

        }
        for ( BundleComponentContext componentContext : tmpImplementationObjects.values() )
        {
            Object instance = componentContext.getInstance();
            result = modifiedMethod.invoke( instance,
                    new ActivateMethod.ActivatorParameter( componentContext, -1 ), MethodResult.VOID );

        }
        return result;
    }

    protected boolean hasInstance()
    {
        return !serviceContexts.isEmpty();
    }

    //---------- Component interface

    public ComponentInstance getComponentInstance()
    {
        // TODO: should return the component instance corresponding to the
        // bundle owning ScrService
        return super.getComponentInstance();
    }

    private static class BundleComponentContext extends ComponentContextImpl
    {

        private Bundle m_usingBundle;
        private Object m_implementationObject;


        BundleComponentContext( AbstractComponentManager componentManager, Bundle usingBundle )
        {
            super( componentManager );

            m_usingBundle = usingBundle;
        }


        private void setImplementationObject( Object implementationObject )
        {
            m_implementationObject = implementationObject;
        }


        public Bundle getUsingBundle()
        {
            return m_usingBundle;
        }


        //---------- ComponentInstance interface ------------------------------

        public Object getInstance()
        {
            return m_implementationObject;
        }
    }
}
