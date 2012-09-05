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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
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
 * Finally, if the <code>ds.factory.enabled</code> bundle context property is
 * set to <code>true</code>, component instances can be created by factory
 * configurations. This functionality is present for backwards compatibility
 * with earlier releases of the Apache Felix Declarative Services implementation.
 * But keep in mind, that this is non-standard behaviour.
 */
public class ComponentFactoryImpl extends AbstractComponentManager implements ComponentFactory, ComponentHolder
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
    private final Map m_componentInstances;

    /**
     * The configuration for the component factory. This configuration is
     * supplied as the base configuration for each component instance created
     * by the {@link #newInstance(Dictionary)} method.
     */
    private Dictionary m_configuration;


    public ComponentFactoryImpl( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata );
        m_componentInstances = new IdentityHashMap();
        m_configuration = new Hashtable();
    }


    /* (non-Javadoc)
     * @see org.osgi.service.component.ComponentFactory#newInstance(java.util.Dictionary)
     */
    public ComponentInstance newInstance( Dictionary dictionary )
    {
        final ImmediateComponentManager cm = createComponentManager();
        log( LogService.LOG_DEBUG, "Creating new instance from component factory {0} with configuration {1}",
                new Object[] { getComponentMetadata().getName(), dictionary }, null );

        ComponentInstance instance;
        final boolean release = cm.obtainReadLock( "ComponentFactoryImpl.newInstance.1" );
        try
        {
            cm.setFactoryProperties( dictionary );
            cm.reconfigure( m_configuration );

            // enable and activate immediately
            cm.enableInternal();
            cm.activateInternal();

            instance = cm.getComponentInstance();
            if ( instance == null )
            {
                // activation failed, clean up component manager
                cm.disposeInternal( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
                throw new ComponentException( "Failed activating component" );
            }
        }
        finally
        {
            if ( release )
            {
                cm.releaseReadLock( "ComponentFactoryImpl.newInstance.1" );
            }
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.put( cm, cm );
        }

        return instance;
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
        if (!(object instanceof ComponentFactoryImpl))
        {
            return false;
        }

        ComponentFactoryImpl other = (ComponentFactoryImpl) object;
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
     * The component factory does not have a component to create.
     */
    protected boolean createComponent()
    {
        return true;
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


    protected void registerService()
    {
        log( LogService.LOG_DEBUG, "registering component factory", null );
        registerService(new String[]
            { ComponentFactory.class.getName() });
    }


    public Object getInstance()
    {
        // this does not return the component instance actually
        return null;
    }


    public boolean hasConfiguration()
    {
        return m_configuration != null;
    }


    public Dictionary getProperties()
    {
        Dictionary props = getServiceProperties();

        // add target properties of references
        List depMetaData = getComponentMetadata().getDependencies();
        for ( Iterator di = depMetaData.iterator(); di.hasNext(); )
        {
            ReferenceMetadata rm = ( ReferenceMetadata ) di.next();
            if ( rm.getTarget() != null )
            {
                props.put( rm.getTargetPropertyName(), rm.getTarget() );
            }
        }

        return props;
    }

    public void setServiceProperties( Dictionary serviceProperties )
    {
        throw new IllegalStateException( "ComponentFactory service properties are immutable" );
    }


    public Dictionary getServiceProperties()
    {
        Dictionary props = new Hashtable();

        // 112.5.5 The Component Factory service must register with the following properties
        props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
        props.put( ComponentConstants.COMPONENT_FACTORY, getComponentMetadata().getFactoryIdentifier() );

        // also register with the factory PID
        props.put( Constants.SERVICE_PID, getComponentMetadata().getName() );

        // descriptive service properties
        props.put( Constants.SERVICE_DESCRIPTION, "ManagedServiceFactory for Factory Component"
            + getComponentMetadata().getName() );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );

        return props;
    }

    State getSatisfiedState()
    {
        return Factory.getInstance();
    }

    State getActiveState()
    {
        return Factory.getInstance();
    }

    protected boolean collectDependencies()
    {
        Map old = getDependencyMap();
        if ( old == null )
        {
            Map dependenciesMap = new HashMap();
            for (Iterator i = getDependencyManagers(); i.hasNext(); )
            {
                dependenciesMap.put( i.next(), Collections.EMPTY_MAP );
            }
            setDependencyMap( old, dependenciesMap );
        }
        return true;
    }

    //---------- Component interface


    public ComponentInstance getComponentInstance()
    {
        // a ComponentFactory is not a real component and as such does
        // not have a ComponentInstance
        return null;
    }


    //---------- ComponentHolder interface

    public void configurationDeleted( String pid )
    {
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            // deleting configuration of a component factory is like
            // providing an empty configuration
            m_configuration = new Hashtable();
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            log( LogService.LOG_ERROR, "Component Factory cannot be configured by factory configuration", null );
        }
    }


    public void configurationUpdated( String pid, Dictionary configuration )
    {
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            m_configuration = configuration;
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            log( LogService.LOG_ERROR, "Component Factory cannot be configured by factory configuration", null );
        }
    }


    public Component[] getComponents()
    {
        List cms = getComponentList();
        return (Component[]) cms.toArray( new Component[ cms.size() ] );
    }

    protected List getComponentList()
    {
        List cms = new ArrayList( );
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


    /**
     * Disposes off all components ever created by this component holder. This
     * method is called if either the Declarative Services runtime is stopping
     * or if the owning bundle is stopped. In both cases all components created
     * by this holder must be disposed off.
     */
    public void disposeComponents( int reason )
    {
        List cms = new ArrayList( );
        getComponentManagers( m_componentInstances, cms );
        for ( Iterator i = cms.iterator(); i.hasNext(); )
        {
            ((AbstractComponentManager)i.next()).dispose( reason );
        }

        synchronized ( m_componentInstances )
        {
            m_componentInstances.clear();
        }

        // finally dispose the component factory itself
        dispose( reason );
    }


    public void disposed( ImmediateComponentManager component )
    {
        synchronized ( m_componentInstances )
        {
            m_componentInstances.remove( component );
        }
    }


    //---------- internal


    /**
     * Creates an {@link ImmediateComponentManager} instance with the
     * {@link BundleComponentActivator} and {@link ComponentMetadata} of this
     * instance. The component manager is kept in the internal set of created
     * components. The component is neither configured nor enabled.
     */
    private ImmediateComponentManager createComponentManager()
    {
        return new ComponentFactoryNewInstance( getActivator(), this, getComponentMetadata() );
    }


    protected void getComponentManagers( Map componentMap, List componentManagers )
    {
        if ( componentMap != null )
        {
            synchronized ( componentMap )
            {
                componentManagers.addAll( componentMap.values() );
            }
        }
    }

    static class ComponentFactoryNewInstance extends ImmediateComponentManager {

        public ComponentFactoryNewInstance( BundleComponentActivator activator, ComponentHolder componentHolder,
            ComponentMetadata metadata )
        {
            super( activator, componentHolder, metadata );
        }

        State getActiveState()
        {
            return FactoryInstance.getInstance();
        }

    }

}
