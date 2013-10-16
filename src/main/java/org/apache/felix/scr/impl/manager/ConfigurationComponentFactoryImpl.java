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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.TargetedPID;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.log.LogService;

/**
 * The <code>ComponentFactoryImpl</code> extends the {@link org.apache.felix.scr.impl.manager.AbstractComponentManager}
 * class to implement the component factory functionality. As such the
 * OSGi Declarative Services <code>ComponentFactory</code> interface is
 * implemented.
 * <p>
 * In addition the {@link org.apache.felix.scr.impl.config.ComponentHolder} interface is implemented to use this
 * class directly as the holder for component instances created by the
 * {@link #newInstance(java.util.Dictionary)} method.
 * <p>
 * Finally, if the <code>ds.factory.enabled</code> bundle context property is
 * set to <code>true</code>, component instances can be created by factory
 * configurations. This functionality is present for backwards compatibility
 * with earlier releases of the Apache Felix Declarative Services implementation.
 * But keep in mind, that this is non-standard behaviour.
 */
public class ConfigurationComponentFactoryImpl<S> extends ComponentFactoryImpl<S>
{

    /**
     * The map of components created from Configuration objects maps PID to
     * {@link org.apache.felix.scr.impl.manager.SingleComponentManager} for configuration updating this map is
     * lazily created.
     */
    private final Map<String, SingleComponentManager<S>> m_configuredServices = new HashMap<String, SingleComponentManager<S>>();

    public ConfigurationComponentFactoryImpl( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata );
    }


    /**
     * The component factory does not have a component to create.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to enabled as a consequence of activating
     * the component factory.
     */
    boolean getServiceInternal()
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        getComponentManagers( m_configuredServices, cms );
        for ( Iterator i = cms.iterator(); i.hasNext(); )
        {
            ((AbstractComponentManager)i.next()).enable( false );
        }

        m_activated = true;
        return true;
    }


    /**
     * The component factory does not have a component to delete.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to disabled as a consequence of deactivating
     * the component factory.
     */
    @Override
    protected void deleteComponent( int reason )
    {
        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        getComponentManagers( m_configuredServices, cms );
        for ( AbstractComponentManager<S> cm: cms )
        {
            cm.disable();
        }
    }


    //---------- ComponentHolder interface

    public void configurationDeleted( String pid )
    {
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            super.configurationDeleted( pid );
        }
        else
        {
            SingleComponentManager<S> cm;
            synchronized ( m_configuredServices )
            {
                cm = m_configuredServices.remove( pid );
            }

            if ( cm != null )
            {
                log( LogService.LOG_DEBUG, "Disposing component after configuration deletion", null );

                cm.dispose();
            }
        }
    }


    public boolean configurationUpdated( String pid, Dictionary<String, Object> configuration, long changeCount, TargetedPID targetedPid )
    {
        if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
        {
            return super.configurationUpdated( pid, configuration, changeCount, targetedPid );
        }
        else   //non-spec backwards compatible
        {
            SingleComponentManager<S> cm;
            synchronized ( m_configuredServices )
            {
                cm = m_configuredServices.get( pid );
            }

            if ( cm == null )
            {
                // create a new instance with the current configuration
                cm = createConfigurationComponentManager();

                // this should not call component reactivation because it is
                // not active yet
                cm.reconfigure( configuration, changeCount, m_targetedPID );

                // enable asynchronously if components are already enabled
                if ( getState() == STATE_FACTORY )
                {
                    cm.enable( false );
                }

                synchronized ( m_configuredServices )
                {
                    // keep a reference for future updates
                    m_configuredServices.put( pid, cm );
                }
                return true;

            }
            else
            {
                // update the configuration as if called as ManagedService
                cm.reconfigure( configuration, changeCount, m_targetedPID );
                return false;
            }
        }
    }


    public Component[] getComponents()
    {
        List<AbstractComponentManager<S>> cms = getComponentList();
        getComponentManagers( m_configuredServices, cms );
        return cms.toArray( new Component[ cms.size() ] );
    }


    /**
     * Disposes off all components ever created by this component holder. This
     * method is called if either the Declarative Services runtime is stopping
     * or if the owning bundle is stopped. In both cases all components created
     * by this holder must be disposed off.
     */
    public void disposeComponents( int reason )
    {
        super.disposeComponents( reason );

        List<AbstractComponentManager<S>> cms = new ArrayList<AbstractComponentManager<S>>( );
        getComponentManagers( m_configuredServices, cms );
        for ( AbstractComponentManager acm: cms )
        {
            acm.dispose( reason );
        }

        m_configuredServices.clear();

        // finally dispose the component factory itself
        dispose( reason );
    }

    public synchronized long getChangeCount( String pid)
    {

        if (pid.equals( getComponentMetadata().getConfigurationPid())) 
        {
            return m_changeCount;
        }
        synchronized ( m_configuredServices )
        {
            SingleComponentManager icm = m_configuredServices.get( pid );
            return icm == null? -1: icm.getChangeCount();
        }
    }


    //---------- internal


    /**
     * Creates an {@link org.apache.felix.scr.impl.manager.SingleComponentManager} instance with the
     * {@link org.apache.felix.scr.impl.BundleComponentActivator} and {@link org.apache.felix.scr.impl.metadata.ComponentMetadata} of this
     * instance. The component manager is kept in the internal set of created
     * components. The component is neither configured nor enabled.
     */
    private SingleComponentManager<S> createConfigurationComponentManager()
    {
        return new ComponentFactoryConfiguredInstance<S>( getActivator(), this, getComponentMetadata(), getComponentMethods() );
    }

    static class ComponentFactoryConfiguredInstance<S> extends SingleComponentManager<S> {

        public ComponentFactoryConfiguredInstance( BundleComponentActivator activator, ComponentHolder componentHolder,
                ComponentMetadata metadata, ComponentMethods componentMethods )
        {
            super( activator, componentHolder, metadata, componentMethods, true );
        }

        public boolean isImmediate()
        {
            return true;
        }
    }
}
