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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.component.ExtFactoryComponentInstance;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
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
 * This class implements spec-compliant component factories and the felix 
 * "persistent" component factory, where the factory is always registered whether or
 * not all dependencies are present and the created components also persist whether or 
 * not the dependencies are present to allow the component instance to exist.
 */
public class ComponentFactoryImpl<S> extends AbstractComponentManager<S> implements ComponentFactory, ComponentContainer<S>
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
    private volatile Map<String, Object> m_configuration;
    
    /**
     * Flag telling if our component factory is currently configured from config admin.
     * We are configured when configuration policy is required and we have received the
     * config admin properties, or when configuration policy is optional or ignored.
     */
    private volatile boolean m_hasConfiguration;
    
    /**
     * Configuration change count (R5) or imitation (R4)
     */
    protected volatile long m_changeCount = -1;
    
    protected TargetedPID m_targetedPID;

    public ComponentFactoryImpl( ComponentContainer<S> container, ComponentMethods componentMethods )
    {
        super( container, componentMethods );
        m_componentInstances = new IdentityHashMap<SingleComponentManager<S>, SingleComponentManager<S>>();
        m_configuration = new HashMap<String, Object>();
    }


    protected boolean verifyDependencyManagers()
    {
        if (!getComponentMetadata().isPersistentFactoryComponent())
        {
            return super.verifyDependencyManagers();
        }
        return true;    
    }
    
    @Override
    public boolean isFactory()
    {
        return true;
    }

    /* (non-Javadoc)
    * @see org.osgi.service.component.ComponentFactory#newInstance(java.util.Dictionary)
    */
    public ComponentInstance newInstance( Dictionary<String, ?> dictionary )
    {
        final SingleComponentManager<S> cm = createComponentManager();
        log( LogService.LOG_DEBUG, "Creating new instance from component factory {0} with configuration {1}",
                new Object[] {getComponentMetadata().getName(), dictionary}, null );

        cm.setFactoryProperties( dictionary );
        //configure the properties
        cm.reconfigure( m_configuration, false );
        // enable
        cm.enableInternal();

        ComponentInstance instance;
        if ( getComponentMetadata().isPersistentFactoryComponent() ) 
        {
            instance = new ModifyComponentInstance<S>(cm);
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
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.put( cm, cm );
        }
        
        return instance;
    }
    
    private static class ModifyComponentInstance<S> implements ExtFactoryComponentInstance
    {
        private final SingleComponentManager<S> cm;

        public ModifyComponentInstance(SingleComponentManager<S> cm)
        {
            this.cm = cm;
        }

        public void dispose()
        {
            cm.dispose();            
        }

        public Object getInstance()
        {
            final ComponentInstance componentInstance = cm.getComponentInstance();
            return componentInstance == null? null: componentInstance.getInstance();
        }

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
     * @param object The {@code ComponentFactoryImpl} object to be compared.
     * @return {@code true} if {@code object} is a
     *         {@code ComponentFactoryImpl} and is equal to this object;
     *         {@code false} otherwise.
     */
    public boolean equals(Object object)
    {
        if (!(object instanceof ComponentFactoryImpl<?>))
        {
            return false;
        }

        ComponentFactoryImpl<?> other = (ComponentFactoryImpl<?>) object;
        return getComponentMetadata().getName().equals(other.getComponentMetadata().getName());
    }
    
   /**
    * Returns a hash code value for the object.
    * 
    * @return An integer which is a hash code value for this object.
    */
   public int hashCode()
   {
       return getComponentMetadata().getName().hashCode();
   }

    /**
     * The component factory does not have a component to delete.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to disabled as a consequence of deactivating
     * the component factory.
     */
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


    /** 
     * For ComponentFactoryImpl, this is used only for updating targets on the dependency managers, so we don't need any other 
     * properties.
     */
    public Map<String, Object> getProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();

        // add target properties of references
        List<ReferenceMetadata> depMetaData = getComponentMetadata().getDependencies();
        for ( ReferenceMetadata rm : depMetaData )
        {
            if ( rm.getTarget() != null )
            {
                props.put( rm.getTargetPropertyName(), rm.getTarget() );
            }
        }

        // add target properties from configuration (if we have one)        
        for ( String key :  m_configuration.keySet() )
        {
            if ( key.endsWith( ".target" ) )
            {
                props.put( key, m_configuration.get( key ) );
            }
        }

        return props;
    }
    
    public void setServiceProperties( Dictionary<String, ?> serviceProperties )
    {
        throw new IllegalStateException( "ComponentFactory service properties are immutable" );
    }

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

    public Dictionary<String, Object> getServiceProperties()
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        // 112.5.5 The Component Factory service must register with the following properties
        props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
        props.put( ComponentConstants.COMPONENT_FACTORY, getComponentMetadata().getFactoryIdentifier() );

        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );

        return props;
    }

    boolean hasInstance()
    {
        return false;
    }

    protected boolean collectDependencies(ComponentContextImpl<S> componentContext)
    {
        return true;
    }

    <T> boolean invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> ref, int trackingCount )
    {
    	return false;
    }

    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> reference, int trackingCount )
    {
    }

    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRef, int trackingCount )
    {
    }

    //---------- Component interface


    public ComponentInstance getComponentInstance()
    {
        // a ComponentFactory is not a real component and as such does
        // not have a ComponentInstance
        return null;
    }

    /**
     * Disposes off all components ever created by this component holder. This
     * method is called if either the Declarative Services runtime is stopping
     * or if the owning bundle is stopped. In both cases all components created
     * by this holder must be disposed off.
     */
    public void dispose( int reason )
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        getComponentManagers( m_componentInstances, cms );
        for ( AbstractComponentManager<S> acm: cms )
        {
            acm.dispose( reason );
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.clear();
        }

        // finally dispose the component factory itself
        super.dispose( reason );
    }


    public void disposed( SingleComponentManager<S> component )
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
        return new SingleComponentManager<S>( this, getComponentMethods(), !getComponentMetadata().isPersistentFactoryComponent() );
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

    public TargetedPID getConfigurationTargetedPID(TargetedPID pid, TargetedPID factoryPid)
    {
        return m_targetedPID;
    }


	@Override
	public void reconfigure(Map<String, Object> configuration, boolean configurationDeleted) {
		m_configuration = configuration;
		List<SingleComponentManager<S>> cms;
		synchronized (m_componentInstances)
        {
            cms = new ArrayList<SingleComponentManager<S>>(m_componentInstances.keySet());
        }
		for (SingleComponentManager<S> cm: cms)
		{
		    cm.reconfigure( configuration, configurationDeleted);
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

}
