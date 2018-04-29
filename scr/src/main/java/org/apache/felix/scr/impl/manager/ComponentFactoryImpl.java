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
<<<<<<< HEAD
import java.util.Collections;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

<<<<<<< HEAD
import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.TargetedPID;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
=======
import org.apache.felix.scr.component.ExtFactoryComponentInstance;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;

/**
 * The <code>ComponentFactoryImpl</code> extends the {@link AbstractComponentManager}
 * class to implement the component factory functionality. As such the
 * OSGi Declarative Services <code>ComponentFactory</code> interface is
 * implemented.
 * <p>
 * In addition the {@link ComponentHolder} interface is implemented to use this
 * class directly as the holder for component instances created by the
 * {@link #newInstance(Dictionary)} method.
 * <p>
<<<<<<< HEAD
 * Finally, if the <code>ds.factory.enabled</code> bundle context property is
 * set to <code>true</code>, component instances can be created by factory
 * configurations. This functionality is present for backwards compatibility
 * with earlier releases of the Apache Felix Declarative Services implementation.
 * But keep in mind, that this is non-standard behaviour.
 */
public class ComponentFactoryImpl<S> extends AbstractComponentManager<S> implements ComponentFactory, ComponentHolder
=======
 * This class implements spec-compliant component factories and the felix
 * "persistent" component factory, where the factory is always registered whether or
 * not all dependencies are present and the created components also persist whether or
 * not the dependencies are present to allow the component instance to exist.
 */
public class ComponentFactoryImpl<S> extends AbstractComponentManager<S> implements ComponentFactory<S>, ComponentContainer<S>
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
{

    /**
     * Contains the component instances created by calling the
     * {@link #newInstance(Dictionary)} method. These component instances are
     * provided with updated configuration (or deleted configuration) if
     * such modifications for the component factory takes place.
     * <p>
     * The map is keyed by the component manager instances. The value of each
     * entry is the same as the entry's key.
     * This is an IdentityHashMap for speed, thus not a Set.
     */
    private final Map<SingleComponentManager<S>, SingleComponentManager<S>> m_componentInstances;

    /**
     * The configuration for the component factory. This configuration is
     * supplied as the base configuration for each component instance created
     * by the {@link #newInstance(Dictionary)} method.
     */
<<<<<<< HEAD
    private volatile Dictionary<String, Object> m_configuration;
    
=======
    private volatile Map<String, Object> m_configuration;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Flag telling if our component factory is currently configured from config admin.
     * We are configured when configuration policy is required and we have received the
     * config admin properties, or when configuration policy is optional or ignored.
     */
    private volatile boolean m_hasConfiguration;
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Configuration change count (R5) or imitation (R4)
     */
    protected volatile long m_changeCount = -1;
<<<<<<< HEAD
    
    protected TargetedPID m_targetedPID;

    public ComponentFactoryImpl( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata, new ComponentMethods() );
        m_componentInstances = new IdentityHashMap<SingleComponentManager<S>, SingleComponentManager<S>>();
        m_configuration = new Hashtable<String, Object>();
=======

    protected TargetedPID m_targetedPID;

    public ComponentFactoryImpl( ComponentContainer<S> container, ComponentMethods componentMethods )
    {
        super( container, componentMethods );
        m_componentInstances = new IdentityHashMap<>();
        m_configuration = new HashMap<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Override
<<<<<<< HEAD
=======
    protected boolean verifyDependencyManagers()
    {
        if (!getComponentMetadata().isPersistentFactoryComponent())
        {
            return super.verifyDependencyManagers();
        }
        return true;
    }

    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean isFactory()
    {
        return true;
    }

    /* (non-Javadoc)
    * @see org.osgi.service.component.ComponentFactory#newInstance(java.util.Dictionary)
    */
<<<<<<< HEAD
    public ComponentInstance newInstance( Dictionary<String, ?> dictionary )
    {
        final SingleComponentManager<S> cm = createComponentManager();
        log( LogService.LOG_DEBUG, "Creating new instance from component factory {0} with configuration {1}",
                new Object[] {getComponentMetadata().getName(), dictionary}, null );

        ComponentInstance instance;
        cm.setFactoryProperties( ( Dictionary<String, Object> ) dictionary );
        //configure the properties
        cm.reconfigure( m_configuration, m_changeCount, m_targetedPID );
        // enable
        cm.enableInternal();
        //activate immediately
        cm.activateInternal( getTrackingCount().get() );

        instance = cm.getComponentInstance();
        if ( instance == null || instance.getInstance() == null )
        {
            // activation failed, clean up component manager
            cm.dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
            throw new ComponentException( "Failed activating component" );
=======
    @Override
    public ComponentInstance<S> newInstance( Dictionary<String, ?> dictionary )
    {
        final SingleComponentManager<S> cm = createComponentManager();
        getLogger().log( LogService.LOG_DEBUG, "Creating new instance from component factory with configuration {0}",
                null, dictionary );

        cm.setFactoryProperties( dictionary );
        //configure the properties
        cm.reconfigure( m_configuration, false, null );
        // enable
        cm.enableInternal();

        ComponentInstance<S> instance;
        if ( getComponentMetadata().isPersistentFactoryComponent() )
        {
            instance = new ModifyComponentInstance<>(cm);
        }
        else
        {
        	instance = cm.getComponentInstance();
        	if ( instance == null ||  instance.getInstance() == null )
        	{
        		// activation failed, clean up component manager
        		cm.dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
        		throw new ComponentException( "Failed activating component" );
        	}
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.put( cm, cm );
        }

        return instance;
    }

<<<<<<< HEAD
    /**
     * Compares this {@code ComponentFactoryImpl} object to another object.
     * 
     * <p>
     * A component factory impl is considered to be <b>equal to </b> another component
     * factory impl if the component names are equal(using {@code String.equals}).
     * 
=======
    private static class ModifyComponentInstance<S> implements ExtFactoryComponentInstance<S>
    {
        private final SingleComponentManager<S> cm;

        public ModifyComponentInstance(SingleComponentManager<S> cm)
        {
            this.cm = cm;
        }

        @Override
        public void dispose()
        {
            cm.dispose();
        }

        @Override
        public S getInstance()
        {
            final ComponentInstance<S> componentInstance = cm.getComponentInstance();
            return componentInstance == null? null: componentInstance.getInstance();
        }

        @Override
        public void modify(Dictionary<String, ?> properties)
        {
            cm.setFactoryProperties( properties );
            cm.reconfigure(false);
        }

    }

    /**
     * Compares this {@code ComponentFactoryImpl} object to another object.
     *
     * <p>
     * A component factory impl is considered to be <b>equal to </b> another component
     * factory impl if the component names are equal(using {@code String.equals}).
     *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * @param object The {@code ComponentFactoryImpl} object to be compared.
     * @return {@code true} if {@code object} is a
     *         {@code ComponentFactoryImpl} and is equal to this object;
     *         {@code false} otherwise.
     */
<<<<<<< HEAD
   public boolean equals(Object object)
    {
        if (!(object instanceof ComponentFactoryImpl))
=======
    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof ComponentFactoryImpl<?>))
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            return false;
        }

<<<<<<< HEAD
        ComponentFactoryImpl other = (ComponentFactoryImpl) object;
        return getComponentMetadata().getName().equals(other.getComponentMetadata().getName());
    }
    
   /**
    * Returns a hash code value for the object.
    * 
    * @return An integer which is a hash code value for this object.
    */
   public int hashCode()
=======
        ComponentFactoryImpl<?> other = (ComponentFactoryImpl<?>) object;
        return getComponentMetadata().getName().equals(other.getComponentMetadata().getName());
    }

   /**
    * Returns a hash code value for the object.
    *
    * @return An integer which is a hash code value for this object.
    */
   @Override
public int hashCode()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
   {
       return getComponentMetadata().getName().hashCode();
   }

    /**
<<<<<<< HEAD
     * The component factory does not have a component to create.
     */
    protected boolean createComponent()
    {
        return true;
    }


    /**
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * The component factory does not have a component to delete.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to disabled as a consequence of deactivating
     * the component factory.
     */
<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    protected void deleteComponent( int reason )
    {
    }


    @Override
    protected String[] getProvidedServices()
    {
        return new String[] { ComponentFactory.class.getName() };
    }


    public boolean hasConfiguration()
    {
        return m_hasConfiguration;
    }


<<<<<<< HEAD
    public Dictionary<String, Object> getProperties()
    {
        Dictionary<String, Object> props = getServiceProperties();
=======
    /**
     * For ComponentFactoryImpl, this is used only for updating targets on the dependency managers, so we don't need any other
     * properties.
     */
    @Override
    public Map<String, Object> getProperties()
    {
        Map<String, Object> props = new HashMap<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // add target properties of references
        List<ReferenceMetadata> depMetaData = getComponentMetadata().getDependencies();
        for ( ReferenceMetadata rm : depMetaData )
        {
            if ( rm.getTarget() != null )
            {
                props.put( rm.getTargetPropertyName(), rm.getTarget() );
            }
        }

<<<<<<< HEAD
        // add target properties from configuration (if we have one)        
        for ( String key : Collections.list( m_configuration.keys() ) )
=======
        // add target properties from configuration (if we have one)
        for ( String key :  m_configuration.keySet() )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            if ( key.endsWith( ".target" ) )
            {
                props.put( key, m_configuration.get( key ) );
            }
        }

        return props;
    }
<<<<<<< HEAD
    
    public void setServiceProperties( Dictionary serviceProperties )
=======

    @Override
    public void setServiceProperties( Dictionary<String, ?> serviceProperties )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        throw new IllegalStateException( "ComponentFactory service properties are immutable" );
    }

<<<<<<< HEAD

    public Dictionary<String, Object> getServiceProperties()
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
=======
    @Override
    void postRegister()
    {
        //do nothing
    }

    @Override
    void preDeregister()
    {
        //do nothing
    }

    @Override
    public Dictionary<String, Object> getServiceProperties()
    {
        Dictionary<String, Object> props = new Hashtable<>(getComponentMetadata().getFactoryProperties());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // 112.5.5 The Component Factory service must register with the following properties
        props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
        props.put( ComponentConstants.COMPONENT_FACTORY, getComponentMetadata().getFactoryIdentifier() );

        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );

        return props;
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    boolean hasInstance()
    {
        return false;
    }

<<<<<<< HEAD
    protected boolean collectDependencies()
=======
    @Override
    protected boolean collectDependencies(ComponentContextImpl<S> componentContext)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return true;
    }

<<<<<<< HEAD
    <T> void invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<T> ref, int trackingCount )
    {
    }

    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> reference, int trackingCount )
    {
    }

    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<T> oldRef, int trackingCount )
=======
    @Override
    <T> boolean invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> ref, int trackingCount )
    {
    	return false;
    }

    @Override
    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> reference, int trackingCount )
    {
    }

    @Override
    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRef, int trackingCount )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
    }

    //---------- Component interface


<<<<<<< HEAD
    public ComponentInstance getComponentInstance()
    {
        // a ComponentFactory is not a real component and as such does
        // not have a ComponentInstance
        return null;
    }


    //---------- ComponentHolder interface

    public void configurationDeleted( String pid )
    {
        m_targetedPID = null;
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            log( LogService.LOG_DEBUG, "Handling configuration removal", null );

            m_changeCount = -1;
            // nothing to do if there is no configuration currently known.
            if ( !m_hasConfiguration )
            {
                log( LogService.LOG_DEBUG, "ignoring configuration removal: not currently configured", null );
                return;
            }

            // So far, we were configured: clear the current configuration.
            m_hasConfiguration = false;
            m_configuration = new Hashtable();

            log( LogService.LOG_DEBUG, "Current component factory state={0}", new Object[] {getState()}, null );

            // And deactivate if we are not currently disposed and if configuration is required
            if ( ( getState() & STATE_DISPOSED ) == 0 && getComponentMetadata().isConfigurationRequired() )
            {
                log( LogService.LOG_DEBUG, "Deactivating component factory (required configuration has gone)", null );
                deactivateInternal( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED, true, false );
            }
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            log( LogService.LOG_ERROR, "Component Factory cannot be configured by factory configuration", null );
        }
    }


    public boolean configurationUpdated( String pid, Dictionary<String, Object> configuration, long changeCount, TargetedPID targetedPid )
    {
        if ( m_targetedPID != null && !m_targetedPID.equals( targetedPid ))
        {
            log( LogService.LOG_ERROR, "ImmediateComponentHolder unexpected change in targetedPID from {0} to {1}",
                    new Object[] {m_targetedPID, targetedPid}, null);
            throw new IllegalStateException("Unexpected targetedPID change");
        }
        m_targetedPID = targetedPid;
        if ( configuration != null )
        {
            if ( changeCount <= m_changeCount )
            {
                log( LogService.LOG_DEBUG,
                        "ImmediateComponentHolder out of order configuration updated for pid {0} with existing count {1}, new count {2}",
                        new Object[] { getConfigurationPid(), m_changeCount, changeCount }, null );
                return false;
            }
            m_changeCount = changeCount;
        }
        else 
        {
            m_changeCount = -1;
        }
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            log( LogService.LOG_INFO, "Configuration PID updated for Component Factory", null );

            // Ignore the configuration if our policy is 'ignore'
            if ( getComponentMetadata().isConfigurationIgnored() )
            {
                return false;
            }

            // Store the config admin configuration
            m_configuration = configuration;

            // We are now configured from config admin.
            m_hasConfiguration = true;

            log( LogService.LOG_INFO, "Current ComponentFactory state={0}", new Object[]
                    {getState()}, null );

            // If we are active, but if some config target filters don't match anymore
            // any required references, then deactivate.
            if ( getState() == STATE_FACTORY )
            {
                log( LogService.LOG_INFO, "Verifying if Active Component Factory is still satisfied", null );

                // First update target filters.
                updateTargets( getProperties() );

                // Next, verify dependencies
                if ( !verifyDependencyManagers() )
                {
                    log( LogService.LOG_DEBUG,
                            "Component Factory target filters not satisfied anymore: deactivating", null );
                    deactivateInternal( ComponentConstants.DEACTIVATION_REASON_REFERENCE, false, false );
                    return false;
                }
            }

            // Unsatisfied component and required configuration may change targets
            // to satisfy references.
            if ( getState() == STATE_UNSATISFIED && getComponentMetadata().isConfigurationRequired() )
            {
                // try to activate our component factory, if all dependnecies are satisfied
                log( LogService.LOG_DEBUG, "Attempting to activate unsatisfied component", null );
                // First update target filters.
                updateTargets( getProperties() );
                activateInternal( getTrackingCount().get() );
            }
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            log( LogService.LOG_ERROR, "Component Factory cannot be configured by factory configuration", null );
        }
        return false;
    }


    public synchronized long getChangeCount( String pid)
    {
        
        return m_changeCount;
    }

    public Component[] getComponents()
    {
        List<AbstractComponentManager<S>> cms = getComponentList();
        return cms.toArray( new Component[ cms.size() ] );
    }

    protected List<AbstractComponentManager<S>> getComponentList()
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        cms.add( this );
        getComponentManagers( m_componentInstances, cms );
        return cms;
    }


    /**
     * A component factory component holder enables the held components by
     * enabling itself.
     */
    public void enableComponents( boolean async )
    {
        enable( async );
    }


    /**
     * A component factory component holder disables the held components by
     * disabling itself.
     */
    public void disableComponents( boolean async )
    {
        disable( async );
    }


=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Disposes off all components ever created by this component holder. This
     * method is called if either the Declarative Services runtime is stopping
     * or if the owning bundle is stopped. In both cases all components created
     * by this holder must be disposed off.
     */
<<<<<<< HEAD
    public void disposeComponents( int reason )
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        getComponentManagers( m_componentInstances, cms );
        for ( AbstractComponentManager acm: cms )
=======
    @Override
    public void dispose( int reason )
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<>( );
        getComponentManagers( m_componentInstances, cms );
        for ( AbstractComponentManager<S> acm: cms )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            acm.dispose( reason );
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.clear();
        }

        // finally dispose the component factory itself
<<<<<<< HEAD
        dispose( reason );
    }


    public void disposed( SingleComponentManager component )
=======
        super.dispose( reason );
    }


    @Override
    public void disposed( SingleComponentManager<S> component )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        synchronized ( m_componentInstances )
        {
            m_componentInstances.remove( component );
        }
    }


    //---------- internal


    /**
     * Creates an {@link SingleComponentManager} instance with the
     * {@link BundleComponentActivator} and {@link ComponentMetadata} of this
     * instance. The component manager is kept in the internal set of created
     * components. The component is neither configured nor enabled.
     */
    private SingleComponentManager<S> createComponentManager()
    {
<<<<<<< HEAD
        return new ComponentFactoryNewInstance<S>( getActivator(), this, getComponentMetadata(), getComponentMethods() );
=======
        return new SingleComponentManager<>( this, getComponentMethods(), !getComponentMetadata().isPersistentFactoryComponent() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    protected void getComponentManagers( Map<?, SingleComponentManager<S>> componentMap, List<AbstractComponentManager<S>> componentManagers )
    {
        if ( componentMap != null )
        {
            synchronized ( componentMap )
            {
                componentManagers.addAll( componentMap.values() );
            }
        }
    }

<<<<<<< HEAD
    static class ComponentFactoryNewInstance<S> extends SingleComponentManager<S> {

        public ComponentFactoryNewInstance( BundleComponentActivator activator, ComponentHolder componentHolder,
                ComponentMetadata metadata, ComponentMethods componentMethods )
        {
            super( activator, componentHolder, metadata, componentMethods, true );
        }

    }

    public TargetedPID getConfigurationTargetedPID(TargetedPID pid)
    {
        return m_targetedPID;
    }


=======
    public TargetedPID getConfigurationTargetedPID(TargetedPID pid, TargetedPID factoryPid)
    {
        return m_targetedPID;
    }


	@Override
	public void reconfigure(Map<String, Object> configuration, boolean configurationDeleted, TargetedPID factoryPid) {
	    if ( factoryPid != null ) {
	        // ignore factory configuration changes for component factories.
	        return;
	    }
		m_configuration = configuration;
		List<SingleComponentManager<S>> cms;
		synchronized (m_componentInstances)
        {
            cms = new ArrayList<>(m_componentInstances.keySet());
        }
		for (SingleComponentManager<S> cm: cms)
		{
		    cm.reconfigure( configuration, configurationDeleted, factoryPid);
		}
	}


    @Override
    public void getComponentManagers(List<AbstractComponentManager<S>> cms)
    {
        synchronized (m_componentInstances)
        {
            cms.addAll(m_componentInstances.keySet());
        }
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
