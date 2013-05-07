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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.TargetedPID;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.DelayedComponentManager;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.manager.ServiceFactoryComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;


/**
 * The <code>ConfiguredComponentHolder</code> class is a
 * {@link ComponentHolder} for one or more components instances configured by
 * singleton or factory configuration objects received from the Configuration
 * Admin service.
 * <p>
 * This holder is used only for components configured (optionally or required)
 * by the Configuration Admin service. It is not used for components declared
 * as ignoring configuration or if no Configuration Admin service is available.
 * <p>
 * The holder copes with three situations:
 * <ul>
 * <li>No configuration is available for the held component. That is there is
 * no configuration whose <code>service.pid</code> or
 * <code>service.factoryPid</code> equals the component name.</li>
 * <li>A singleton configuration is available whose <code>service.pid</code>
 * equals the component name.</li>
 * <li>One or more factory configurations exist whose
 * <code>service.factoryPid</code> equals the component name.</li>
 * </ul>
 */
public class ImmediateComponentHolder<S> implements ComponentHolder, SimpleLogger
{

    /**
     * The activator owning the per-bundle components
     */
    private final BundleComponentActivator m_activator;

    /**
     * The {@link ComponentMetadata} describing the held component(s)
     */
    private final ComponentMetadata m_componentMetadata;

    /**
     * A map of components configured with factory configuration. The indices
     * are the PIDs (<code>service.pid</code>) of the configuration objects.
     * The values are the {@link ImmediateComponentManager<S> component instances}
     * created on behalf of the configurations.
     */
    private final Map<String, ImmediateComponentManager<S>> m_components;

    /**
     * The special component used if there is no configuration or a singleton
     * configuration. This field is only <code>null</code> once all components
     * held by this holder have been disposed of by
     * {@link #disposeComponents(int)} and is first created in the constructor.
     * As factory configurations are provided this instance may be configured
     * or "deconfigured".
     * <p>
     * Expected invariants:
     * <ul>
     * <li>This field is only <code>null</code> after disposal of all held
     * components</li>
     * <li>The {@link #m_components} map is empty or the component pointed to
     * by this field is also contained in the map</li>
     * <ul>
     */
    private ImmediateComponentManager<S> m_singleComponent;

    /**
     * Whether components have already been enabled by calling the
     * {@link #enableComponents(boolean)} method. If this field is <code>true</code>
     * component instances created per configuration by the
     * {@link #configurationUpdated(String, Dictionary, long, TargetedPID)} method are also
     * enabled. Otherwise they are not enabled immediately.
     */
    private volatile boolean m_enabled;
    private final ComponentMethods m_componentMethods;
    
    private TargetedPID m_targetedPID;
    

    public ImmediateComponentHolder( final BundleComponentActivator activator, final ComponentMetadata metadata )
    {
        this.m_activator = activator;
        this.m_componentMetadata = metadata;
        this.m_components = new HashMap<String, ImmediateComponentManager<S>>();
        this.m_componentMethods = new ComponentMethods();
        this.m_singleComponent = createComponentManager();
        this.m_enabled = false;
    }

    protected ImmediateComponentManager<S> createComponentManager()
    {

        ImmediateComponentManager<S> manager;
        if ( m_componentMetadata.isFactory() )
        {
            throw new IllegalArgumentException( "Cannot create component factory for " + m_componentMetadata.getName() );
        }
        else if ( m_componentMetadata.isImmediate() )
        {
            manager = new ImmediateComponentManager<S>( m_activator, this, m_componentMetadata, m_componentMethods );
        }
        else if ( m_componentMetadata.getServiceMetadata() != null )
        {
            if ( m_componentMetadata.getServiceMetadata().isServiceFactory() )
            {
                manager = new ServiceFactoryComponentManager<S>( m_activator, this, m_componentMetadata, m_componentMethods );
            }
            else
            {
                manager = new DelayedComponentManager<S>( m_activator, this, m_componentMetadata, m_componentMethods );
            }
        }
        else
        {
            // if we get here, which is not expected after all, we fail
            throw new IllegalArgumentException( "Cannot create a component manager for "
                + m_componentMetadata.getName() );
        }

        return manager;
    }


    public final BundleComponentActivator getActivator()
    {
        return m_activator;
    }


    public final ComponentMetadata getComponentMetadata()
    {
        return m_componentMetadata;
    }


    /**
     * The configuration with the given <code>pid</code>
     * (<code>service.pid</code> of the configuration object) is deleted.
     * <p>
     * The following situations are supported:
     * <ul>
     * <li>The configuration was a singleton configuration (pid equals the
     * component name). In this case the internal component map is empty and
     * the single component has been configured by the singleton configuration
     * and is no "deconfigured".</li>
     * <li>A factory configuration object has been deleted and the configured
     * object is set as the single component. If the single component held the
     * last factory configuration object, it is deconfigured. Otherwise the
     * single component is disposed off and replaced by another component in
     * the map of existing components.</li>
     * <li>A factory configuration object has been deleted and the configured
     * object is not set as the single component. In this case the component is
     * simply disposed off and removed from the internal map.</li>
     * </ul>
     */
    public void configurationDeleted( final String pid )
    {
        log( LogService.LOG_DEBUG, "ImmediateComponentHolder configuration deleted for pid {0}",
                new Object[] {pid}, null);

        m_targetedPID = null;
        // component to deconfigure or dispose of
        final ImmediateComponentManager icm;
        boolean deconfigure = false;

        synchronized ( m_components )
        {
            // FELIX-2231: nothing to do any more, all components have been disposed off
            if (m_singleComponent == null) 
            {
                return;
            }

            if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
            {
                // singleton configuration has pid equal to component name
                icm = m_singleComponent;
                deconfigure = true;
            }
            else
            {
                // remove the component configured with the deleted configuration
                icm = m_components.remove( pid );
                if ( icm == null ) 
                {
                    // we already removed the component with the deleted configuration
                    return;
                }

                // special casing if the single component is deconfigured
                if ( m_singleComponent == icm )
                {

                    // if the single component is the last remaining, deconfig
                    if ( m_components.isEmpty() )
                    {

                        // if the single component is the last remaining
                        // deconfigure it
		                deconfigure = true;

                    }
                    else
                    {

                        // replace the single component field with another
                        // entry from the map
                        m_singleComponent = m_components.values().iterator().next();

                    }
                }
            }
        }

        // icm may be null if the last configuration deleted was the
        // single component's configuration. Otherwise the component
        // is not the "last" and has to be disposed off
        if ( deconfigure )
        {
	        icm.reconfigure( null, -1 );
        }
        else
        {
            icm.disposeInternal( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED );
        }
    }


    /**
     * Configures a component with the given configuration. This configuration
     * update may happen in various situations:
     * <ul>
     * <li>The <code>pid</code> equals the component name. Hence we have a
     * singleton configuration for the single component held by this holder</li>
     * <li>The configuration is a factory configuration and is the first
     * configuration provided. In this case the single component is provided
     * with the configuration and also stored in the map.</li>
     * <li>The configuration is a factory configuration but not the first. In
     * this case a new component is created, configured and stored in the map</li>
     * </ul>
     */
    public void configurationUpdated( final String pid, final Dictionary<String, Object> props, long changeCount, TargetedPID targetedPid )
    {
        log( LogService.LOG_DEBUG, "ImmediateComponentHolder configuration updated for pid {0} with properties {1}",
                new Object[] {pid, props}, null);

        if ( m_targetedPID != null && !m_targetedPID.equals( targetedPid ))
        {
            log( LogService.LOG_ERROR, "ImmediateComponentHolder unexpected change in targetedPID from {0} to {1}",
                    new Object[] {m_targetedPID, targetedPid}, null);
            throw new IllegalStateException("Unexpected targetedPID change");
        }
        m_targetedPID = targetedPid;
        // component to update or create
        final ImmediateComponentManager icm;
        final String message;
        final boolean enable;
        Object[] notEnabledArguments = null;

        synchronized ( m_components )
        {
            // FELIX-2231: nothing to do any more, all components have been disposed off
            if (m_singleComponent == null) 
            {
                return;
            }

            if ( pid.equals( getComponentMetadata().getConfigurationPid() ) )
            {
                // singleton configuration has pid equal to component name
                icm = m_singleComponent;
                message = "ImmediateComponentHolder reconfiguring single component for pid {0} ";
                enable = false;
            }
            else
            {
                final ImmediateComponentManager existingIcm = m_components.get( pid );
                if ( existingIcm != null )
                {
                    // factory configuration updated for existing component instance
                    icm = existingIcm;
                    message = "ImmediateComponentHolder reconfiguring existing component for pid {0} ";
                    enable = false;
                }
                else
                {
                    // factory configuration created
                    if ( !m_singleComponent.hasConfiguration() )
                    {
                        // configure the single instance if this is not configured
                        icm = m_singleComponent;
                        message = "ImmediateComponentHolder configuring the unconfigured single component for pid {0} ";
                    }
                    else
                    {
                        // otherwise create a new instance to provide the config to
                        icm = createComponentManager();
                        message = "ImmediateComponentHolder configuring a new component for pid {0} ";
                    }

                    // enable the component if it is initially enabled
                    if ( m_enabled && getComponentMetadata().isEnabled() ) 
                    {
                        enable = true;
                    }
                    else 
                    {
                        enable = false;
                        notEnabledArguments = new Object[] {pid, m_enabled, getComponentMetadata().isEnabled()};
                    }

	                // store the component in the map
                    m_components.put( pid, icm );
                }
            }
        }
        log( LogService.LOG_DEBUG, message, new Object[] {pid}, null);

        // configure the component
        icm.reconfigure( props, changeCount );
        log( LogService.LOG_DEBUG, "ImmediateComponentHolder Finished configuring the dependency managers for component for pid {0} ",
                new Object[] {pid}, null );

        if (enable) 
        {
            icm.enable( false );
            log( LogService.LOG_DEBUG, "ImmediateComponentHolder Finished enabling component for pid {0} ",
                    new Object[] {pid}, null );
        }
        else if (notEnabledArguments != null) 
        {
            log( LogService.LOG_DEBUG, "ImmediateComponentHolder Will not enable component for pid {0}: holder enabled state: {1}, metadata enabled: {2} ",
                    notEnabledArguments, null );
        }
    }

    public synchronized long getChangeCount( String pid)
    {
        
        ImmediateComponentManager icm =  pid.equals( getComponentMetadata().getConfigurationPid())? 
                m_singleComponent: m_components.get( pid );
        return icm == null? -1: icm.getChangeCount();
    }

    public Component[] getComponents()
    {
        synchronized ( m_components )
        {
            Component[] components = getComponentManagers( false );
            return ( components != null ) ? components : new Component[] { m_singleComponent };
        }
    }


    public void enableComponents( final boolean async )
    {
        ImmediateComponentManager[] cms;
        synchronized ( m_components )
        {
            m_enabled = true;
            cms = getComponentManagers( false );
            if ( cms == null )
            {
                cms = new ImmediateComponentManager[] { m_singleComponent };
            }
        }
        for ( ImmediateComponentManager cm : cms )
        {
            cm.enable( async );
        }
    }


    public void disableComponents( final boolean async )
    {
        ImmediateComponentManager[] cms;
        synchronized ( m_components )
        {
            m_enabled = false;

            cms = getComponentManagers( false );
            if ( cms == null )
            {
                cms = new ImmediateComponentManager[] { m_singleComponent };
            }
        }
        for ( ImmediateComponentManager cm : cms )
        {
            cm.disable( async );
        }
    }


    public void disposeComponents( final int reason )
    {
        ImmediateComponentManager[] cms;
        synchronized ( m_components )
        {
            // FELIX-1733: get a copy of the single component and clear
            // the field to prevent recreation in disposed(ICM)
            final ImmediateComponentManager singleComponent = m_singleComponent;
            m_singleComponent = null;

            cms = getComponentManagers( true );
            if ( cms == null )
            {
                cms = new ImmediateComponentManager[] { singleComponent };
            }
        }
        for ( ImmediateComponentManager cm : cms )
        {
            cm.dispose( reason );
        }
    }


    public void disposed( ImmediateComponentManager component )
    {
        // ensure the component is removed from the components map
        synchronized ( m_components )
        {
            if ( !m_components.isEmpty() )
            {
                for ( Iterator vi = m_components.values().iterator(); vi.hasNext(); )
                {
                    if ( component == vi.next() )
                    {
                        vi.remove();
                        break;
                    }
                }
            }

            // if the component is the single component, we have to replace it
            // by another entry in the map
            if ( component == m_singleComponent )
            {
                if ( m_components.isEmpty() )
                {
                    // now what ??
                    // is it correct to create a new manager ???
                    m_singleComponent = createComponentManager();
                }
                else
                {
                    m_singleComponent = m_components.values().iterator().next();
                }
            }
        }
    }

    /**
     * Compares this {@code ImmediateComponentHolder} object to another object.
     * 
     * <p>
     * A ImmediateComponentHolder is considered to be <b>equal to </b> another 
     * ImmediateComponentHolder if the component names are equal(using 
     * {@code String.equals}).
     * 
     * @param object The {@code ImmediateComponentHolder} object to be compared.
     * @return {@code true} if {@code object} is a
     *         {@code ImmediateComponentHolder} and is equal to this object;
     *         {@code false} otherwise.
     */
   public boolean equals(Object object)
    {
        if (!(object instanceof ImmediateComponentHolder))
        {
            return false;
        }

        ImmediateComponentHolder other = (ImmediateComponentHolder) object;
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

    //---------- internal

   /**
     * Returns all components from the map, optionally also removing them
     * from the map. If there are no components in the map, <code>null</code>
     * is returned.
     */
    private ImmediateComponentManager[] getComponentManagers( final boolean clear )
    {
        // fast exit if there is no component in the map
        if ( m_components.isEmpty() )
        {
            return null;
        }

        final ImmediateComponentManager[] cm = new ImmediateComponentManager[m_components.size()];
        m_components.values().toArray( cm );

        if ( clear )
        {
            m_components.clear();
        }
        return cm;
    }

    public boolean isLogEnabled( int level )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            return activator.isLogEnabled( level );
        }

        // bundle activator has already been removed, so no logging
        return false;
    }

    public void log( int level, String message, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, getComponentMetadata(), null, ex );
        }
    }

    public void log( int level, String message, Object[] arguments, Throwable ex )
    {
        BundleComponentActivator activator = getActivator();
        if ( activator != null )
        {
            activator.log( level, message, arguments, getComponentMetadata(), null, ex );
        }
    }

    public TargetedPID getConfigurationTargetedPID()
    {
        return m_targetedPID;
    }

}
