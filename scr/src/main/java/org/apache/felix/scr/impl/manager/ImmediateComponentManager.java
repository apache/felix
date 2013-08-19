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


import java.util.Dictionary;
import java.util.Hashtable;
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


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 */
public class ImmediateComponentManager<S> extends AbstractComponentManager<S> implements ServiceFactory<S>
{

    // keep the using bundles as reference "counters" for instance deactivation
    private final AtomicInteger m_useCount = new AtomicInteger( );

    // The context that will be passed to the implementationObject
    private volatile ComponentContextImpl<S> m_componentContext;

    // the component holder responsible for managing this component
    private ComponentHolder m_componentHolder;

    // optional properties provided in the ComponentFactory.newInstance method
    private Dictionary<String, Object> m_factoryProperties;

    // the component properties, also used as service properties
    private Dictionary<String, Object> m_properties;

    // properties supplied ot ExtComponentContext.updateProperties
    // null if properties are not to be overwritten
    private Dictionary<String, Object> m_serviceProperties;

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
    public ImmediateComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
            ComponentMetadata metadata, ComponentMethods componentMethods )
    {
        this(activator, componentHolder, metadata, componentMethods, false);
    }
    
    public ImmediateComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
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

        super.clear();
    }


    // 1. Load the component implementation class
    // 2. Create the component instance and component context
    // 3. Bind the target services
    // 4. Call the activate method, if present
    // if this method is overwritten, the deleteComponent method should
    // also be overwritten
    protected boolean createComponent()
    {
        if ( !isWriteLocked() )
        {
            throw new IllegalStateException( "need write lock (createComponent)" );
        }
        if ( m_componentContext == null )
        {
            S tmpComponent = createImplementationObject( null, new SetImplementationObject<S>()
            {
                public void presetComponentContext( ComponentContextImpl<S> componentContext )
                {
                    m_componentContext = componentContext;
                }


                public void resetImplementationObject( S implementationObject )
                {
                    m_componentContext = null;
                }
            } );

            // if something failed creating the component instance, return false
            if ( tmpComponent == null )
            {
                return false;
            }

            // otherwise set the context and component instance and return true
            log( LogService.LOG_DEBUG, "Set implementation object for component {0}", new Object[] { getName() },  null );

            //notify that component was successfully created so any optional circular dependencies can be retried
            BundleComponentActivator activator = getActivator();
            if ( activator != null )
            {
                activator.missingServicePresent( getServiceReference() );
            }
        }
        return true;
    }


    protected void deleteComponent( int reason )
    {
        if ( !isWriteLocked() )
        {
            throw new IllegalStateException( "need write lock (deleteComponent)" );
        }
        if ( m_componentContext != null )
        {
            m_useCount.set( 0 );
            m_componentContext.setImplementationAccessible( false );
            disposeImplementationObject( m_componentContext, reason );
            m_componentContext = null;
            log( LogService.LOG_DEBUG, "Unset and deconfigured implementation object for component {0} in deleteComponent for reason {1}", new Object[] { getName(), REASONS[ reason ] },  null );
            m_properties = null;
            m_serviceProperties = null;
        }
    }


    public ComponentInstance getComponentInstance()
    {
        return m_componentContext == null? null: m_componentContext.getComponentInstance();
    }


    //**********************************************************************************************************

    /**
     * Get the object that is implementing this descriptor
     *
     * @return the object that implements the services
     */
    S getInstance()
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


    protected S createImplementationObject( Bundle usingBundle, SetImplementationObject setter )
    {
        final Class<S> implementationObjectClass;
        final S implementationObject;

        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        Bundle bundle = getBundle();
        if (bundle == null)
        {
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

        for ( DependencyManager<S, ?> dm: getDependencyManagers())
        {
            // if a dependency turned unresolved since the validation check,
            // creating the instance fails here, so we deactivate and return
            // null.
            boolean open = dm.open( implementationObject, componentContext.getEdgeInfo( dm ) );
            if ( !open )
            {
                log( LogService.LOG_ERROR, "Cannot create component instance due to failure to bind reference {0}",
                        new Object[]
                                {dm.getName()}, null );

                // make sure, we keep no bindings. Only close the dm's we opened.
                boolean skip = true;
                for ( DependencyManager md: getReversedDependencyManagers() )
                {
                    if ( skip && dm == md ) {
                        skip = false;
                    }
                    if ( !skip )
                    {
                        md.close( implementationObject, componentContext.getEdgeInfo( md ) );
                    }
                }

                setter.resetImplementationObject( implementationObject );
                return null;
            }
        }

        // 5. Call the activate method, if present
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
        }
        else
        {
            componentContext.setImplementationAccessible( true );
            m_circularReferences.remove();
            //this may cause a getService as properties now match a filter.
            setServiceProperties( result );
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( ComponentContextImpl<S> componentContext,
            int reason )
    {
        S implementationObject = componentContext.getImplementationObject( false );

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
        }

    }

    boolean hasInstance()
    {
        return m_componentContext != null;
    }

    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> refPair, int trackingCount )
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
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
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
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


    void registerComponentId()
    {
        super.registerComponentId();
        this.m_properties = null;
    }


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
            props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            props.put( ComponentConstants.COMPONENT_ID, getId() );

            m_properties = props;
        }

        return m_properties;
    }

    public void setServiceProperties( Dictionary<String, Object> serviceProperties )
    {
        if ( serviceProperties == null || serviceProperties.isEmpty() )
        {
            m_serviceProperties = null;
        }
        else
        {
            m_serviceProperties = copyTo( null, serviceProperties, false );
            // set component.name and component.id
            m_serviceProperties.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            m_serviceProperties.put( ComponentConstants.COMPONENT_ID, getId() );
        }

        updateServiceRegistration();
    }

    public Dictionary<String, Object> getServiceProperties()
    {
        if ( m_serviceProperties != null )
        {
            return m_serviceProperties;
        }
        return super.getServiceProperties();
    }

    private void updateServiceRegistration()
    {
        ServiceRegistration<?> sr = getServiceRegistration();
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
     * @param changeCount TODO
     * @param targetedPID TODO
     */
    public void reconfigure( Dictionary<String, Object> configuration, long changeCount, TargetedPID targetedPID )
    {
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

        // unsatisfied component and non-ignored configuration may change targets
        // to satisfy references
        if ( getState() == STATE_UNSATISFIED
                && !getComponentMetadata().isConfigurationIgnored() )
        {
            log( LogService.LOG_DEBUG, "Attempting to activate unsatisfied component", null );
            updateTargets( getProperties() );
            activateInternal( getTrackingCount().get() );
            return;
        }

        // reactivate the component to ensure it is provided with the
        // configuration data
        if ( ( getState() & ( STATE_ACTIVE | STATE_FACTORY | STATE_REGISTERED ) ) == 0 )
        {
            // nothing to do for inactive components, leave this method
            log( LogService.LOG_DEBUG, "Component can not be configured in state {0}", new Object[] { getState() }, null );
            updateTargets( getProperties() );
            return;
        }

        // if the configuration has been deleted but configuration is required
        // this component must be deactivated
        if ( configuration == null && getComponentMetadata().isConfigurationRequired() )
        {
            deactivateInternal( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED, true, getTrackingCount().get() );
            updateTargets( getProperties() );
            return;
        }
        if ( !modify() )
        {
            // SCR 112.7.1 - deactivate if configuration is deleted or no modified method declared
            log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure from configuration", null );
            int reason = ( configuration == null ) ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
                    : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;

            // FELIX-2368: cycle component immediately, reconfigure() is
            //     called through ConfigurationListener API which itself is
            //     called asynchronously by the Configuration Admin Service
            deactivateInternal( reason, false, getTrackingCount().get() );
            updateTargets( getProperties() );
            activateInternal( getTrackingCount().get() );
        }
    }

    private boolean modify()
    {
        // 1. no live update if there is no declared method
        if ( getComponentMetadata().getModified() == null )
        {
            log( LogService.LOG_DEBUG, "No modified method, cannot update dynamically", null );
            return false;
        }
        // invariant: we have a modified method name

        // 2. get and check configured method
        // invariant: modify method is configured and found

        // 3. check whether we can dynamically apply the configuration if
        // any target filters influence the bound services
        final Dictionary<String, Object> props = getProperties();
        for ( DependencyManager dm: getDependencyManagers() )
        {
            if ( !dm.canUpdateDynamically( props ) )
            {
                log( LogService.LOG_DEBUG,
                        "Cannot dynamically update the configuration due to dependency changes induced on dependency {0}",
                        new Object[] {dm.getName()}, null );
                return false;
            }
        }
        // invariant: modify method existing and no static bound service changes

        // 4. call method (nothing to do when failed, since it has already been logged)
        //   (call with non-null default result to continue even if the
        //    modify method call failed)
        obtainWriteLock( "ImmediateComponentManager.modify" );
        try
        {
            final MethodResult result = invokeModifiedMethod();
            updateTargets( props );
            if ( result == null )
            {
                // log an error if the declared method cannot be found
                log( LogService.LOG_ERROR, "Declared modify method ''{0}'' cannot be found, configuring by reactivation",
                        new Object[] {getComponentMetadata().getModified()}, null );
                return false;
            }

            // 5. update the target filter on the services now, this may still
            // result in unsatisfied dependencies, in which case we abort
            // this dynamic update and have the component be deactivated
            if ( !verifyDependencyManagers() )
            {
                log( LogService.LOG_ERROR,
                        "Updating the service references caused at least on reference to become unsatisfied, deactivating component",
                        null );
                return false;
            }

            // 6. update service registration properties if we didn't just do it
            if ( result.hasResult() )
            {
                setServiceProperties( result );
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
            releaseWriteLock( "ImmediateComponentManager.modify" );
        }
    }

    protected MethodResult invokeModifiedMethod()
    {
        ModifiedMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        if ( getInstance() != null )
        {
            return modifiedMethod.invoke( getInstance(), new ActivatorParameter( m_componentContext, -1 ),
                    MethodResult.VOID, this );
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
    private boolean servicePropertiesMatches( ServiceRegistration reg, Dictionary<String, Object> props )
    {
        Dictionary<String, Object> regProps = new Hashtable<String, Object>();
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
                obtainWriteLock( "ImmediateComponentManager.getService.1" );
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
                    releaseWriteLock( "ImmediateComponentManager.getService.1" );
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
        {
            return null;
        }

        if ( createComponent() )
        {
            return getInstance();
        }

        // log that the delayed component cannot be created (we don't
        // know why at this moment; this should already have been logged)
        log( LogService.LOG_ERROR, "Failed creating the component instance; see log for reason", null );

        // component could not really be created. This may be temporary
        // so we stay in the registered state but ensure the component
        // instance is deleted
        try
        {
            deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_DEBUG, "Cannot delete incomplete component instance. Ignoring.", t );
        }

        // no service can be returned (be prepared for more logging !!)
        return null;

    }

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
                obtainWriteLock( "ImmediateComponentManager.ungetService.1" );
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
                    releaseWriteLock( "ImmediateComponentManager.ungetService.1" );
                }
            }
        }
    }

    void ungetService( )
    {
        deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
    }

    private boolean keepInstances()
    {
        return getActivator() != null && getActivator().getConfiguration().keepInstances();
    }

    public long getChangeCount()
    {
        return m_changeCount;
    }

    public TargetedPID getConfigurationTargetedPID()
    {
        return m_targetedPID;
    }
}
