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
package org.apache.felix.scr.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ConfigurationSupport;
import org.apache.felix.scr.impl.config.ImmediateComponentHolder;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentFactoryImpl;
import org.apache.felix.scr.impl.manager.ConfigurationComponentFactoryImpl;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;


/**
 * The <code>ComponentRegistry</code> class acts as the global registry for
 * components by name and by component ID. As such the component registry also
 * registers itself as the {@link ScrService} to support access to the
 * registered components.
 */
public class ComponentRegistry implements ScrService, ServiceListener
{

    // the name of the ConfigurationAdmin service
    public static final String CONFIGURATION_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";

    // the bundle context
    private BundleContext m_bundleContext;

    /**
     * The map of known components indexed by component name. The values are
     * either the component names (for name reservations) or implementations
     * of the {@link ComponentHolder} interface.
     * <p>
     * The {@link #checkComponentName(String)} will first add an entry to this
     * map being the name of the component to reserve the name. After setting up
     * the component, the {@link #registerComponentHolder(String, ComponentHolder)}
     * method replaces the value of the named entry with the actual
     * {@link ComponentHolder}.
     *
     * @see #checkComponentName(String)
     * @see #registerComponentHolder(String, ComponentHolder)
     * @see #unregisterComponentHolder(String)
     */
    private final Map<ComponentRegistryKey, ComponentHolder> m_componentHoldersByName;

    /**
     * The map of known components indexed by component configuration pid. The values are
     * Sets of the {@link ComponentHolder} interface. Normally, the configuration pid
     * is the component name, but since DS 1.2 (OSGi 4.3), a component may specify a specific
     * pid, and it is possible that different components refer to the same pid. That's why
     * the values of this map are Sets of ComponentHolders, allowing to lookup all components
     * which are using a given configuration pid.
     * This map is used when the ConfigurationSupport detects that a CM pid is updated. When
     * a PID is updated, the ConfigurationSupport listener class invokes the
     * {@link #getComponentHoldersByPid(String)} method which returns an iterator over all
     * components that are using the given pid for configuration.
     * <p>
     *
     * @see #registerComponentHolder(String, ComponentHolder)
     * @see #unregisterComponentHolder(String)
     * @see ConfigurationSupport#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    private final Map<String, Set<ComponentHolder>> m_componentHoldersByPid;

    /**
     * Map of components by component ID. This map indexed by the component
     * ID number (<code>java.lang.Long</code>) contains the actual
     * {@link AbstractComponentManager} instances existing in the system.
     *
     * @see #registerComponentId(AbstractComponentManager)
     * @see #unregisterComponentId(long)
     */
    private final Map<Long, AbstractComponentManager> m_componentsById;

    /**
     * Counter to setup the component IDs as issued by the
     * {@link #registerComponentId(AbstractComponentManager)} method. This
     * counter is only incremented.
     */
    private volatile long m_componentCounter;

    /**
     * The OSGi service registration for the ScrService provided by this
     * instance.
     */
    private ServiceRegistration m_registration;

    // ConfigurationAdmin support -- created on demand upon availability of
    // the ConfigurationAdmin service
    private ConfigurationSupport configurationSupport;

    private final Map<ServiceReference<?>, List<Entry>> m_missingDependencies = new HashMap<ServiceReference<?>, List<Entry>>( );

    protected ComponentRegistry( BundleContext context )
    {
        m_bundleContext = context;
        m_componentHoldersByName = new HashMap /* <ComponentRegistryKey, Object> */ ();
        m_componentHoldersByPid = new HashMap();
        m_componentsById = new HashMap();
        m_componentCounter = -1;

        // keep me informed on ConfigurationAdmin state changes
        try
        {
            context.addServiceListener(this, "(objectclass=" + CONFIGURATION_ADMIN + ")");
        }
        catch (InvalidSyntaxException ise)
        {
            // not expected (filter is tested valid)
        }

        // If the Configuration Admin Service is already registered, setup
        // configuration support immediately
        if (context.getServiceReference(CONFIGURATION_ADMIN) != null)
        {
            getOrCreateConfigurationSupport();
        }

        // register as ScrService
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Declarative Services Management Agent" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        m_registration = context.registerService( new String[]
            { ScrService.class.getName(), }, this, props );
    }


    public void dispose()
    {
        m_bundleContext.removeServiceListener(this);

        if (configurationSupport != null)
        {
            configurationSupport.dispose();
            configurationSupport = null;
        }

        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }
    }


    //---------- ScrService interface

    public Component[] getComponents()
    {
        Object[] holders = getComponentHolders();
        ArrayList list = new ArrayList();
        for ( int i = 0; i < holders.length; i++ )
        {
            if ( holders[i] instanceof ComponentHolder )
            {
                Component[] components = ( ( ComponentHolder ) holders[i] ).getComponents();
                for ( int j = 0; j < components.length; j++ )
                {
                    list.add( components[j] );
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return ( Component[] ) list.toArray( new Component[list.size()] );
    }


    public Component[] getComponents( Bundle bundle )
    {
        Object[] holders = getComponentHolders();
        ArrayList list = new ArrayList();
        for ( int i = 0; i < holders.length; i++ )
        {
            if ( holders[i] instanceof ComponentHolder )
            {
                ComponentHolder holder = ( ComponentHolder ) holders[i];
                BundleComponentActivator activator = holder.getActivator();
                if ( activator != null && activator.getBundleContext().getBundle() == bundle )
                {
                    Component[] components = holder.getComponents();
                    for ( int j = 0; j < components.length; j++ )
                    {
                        list.add( components[j] );
                    }
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return ( Component[] ) list.toArray( new Component[list.size()] );
    }


    public Component getComponent( long componentId )
    {
        synchronized ( m_componentsById )
        {
            return ( Component ) m_componentsById.get( new Long( componentId ) );
        }
    }


    public Component[] getComponents( String componentName )
    {
        List /* <ComponentHolder */list = new ArrayList/* <ComponentHolder> */();
        synchronized ( m_componentHoldersByName )
        {
            for ( Iterator ci = m_componentHoldersByName.values().iterator(); ci.hasNext(); )
            {
                ComponentHolder c = ( ComponentHolder ) ci.next();
                if ( componentName.equals( c.getComponentMetadata().getName() ) )
                {
                    list.addAll( Arrays.asList( c.getComponents() ) );
                }
            }
        }

        return ( list.isEmpty() ) ? null : ( Component[] ) list.toArray( new Component[list.size()] );
    }


    //---------- ComponentManager registration by component Id

    /**
     * Assigns a unique ID to the component, internally registers the
     * component under that ID and returns the assigned component ID.
     *
     * @param componentManager The {@link AbstractComponentManager} for which
     *      to assign a component ID and which is to be internally registered
     *
     * @return the assigned component ID
     */
    final long registerComponentId( final AbstractComponentManager componentManager )
    {
        long componentId;
        synchronized ( m_componentsById )
        {
            m_componentCounter++;
            componentId = m_componentCounter;

            m_componentsById.put( new Long( componentId ), componentManager );
        }

        return componentId;
    }


    /**
     * Unregisters the component with the given component ID from the internal
     * registry. After unregistration, the component ID should be considered
     * invalid.
     *
     * @param componentId The ID of the component to be removed from the
     *      internal component registry.
     */
    final void unregisterComponentId( final long componentId )
    {
        synchronized ( m_componentsById )
        {
            m_componentsById.remove( new Long( componentId ) );
        }
    }


    //---------- ComponentHolder registration by component name

    /**
     * Checks whether the component name is "globally" unique or not. If it is
     * unique, it is reserved until the actual component is registered with
     * {@link #registerComponentHolder(String, ComponentHolder)} or until
     * it is unreserved by calling {@link #unregisterComponentHolder(String)}.
     * If a component with the same name has already been reserved or registered
     * a ComponentException is thrown with a descriptive message.
     *
     * @param bundle the bundle registering the component
     * @param name the component name to check and reserve
     * @throws ComponentException if the name is already in use by another
     *      component.
     */
    final ComponentRegistryKey checkComponentName( final Bundle bundle, final String name )
    {
        // register the name if no registration for that name exists already
        final ComponentRegistryKey key = new ComponentRegistryKey( bundle, name );
        ComponentHolder existingRegistration = null;
        boolean present;
        synchronized ( m_componentHoldersByName )
        {
            present = m_componentHoldersByName.containsKey( key );
            if ( !present )
            {
                m_componentHoldersByName.put( key, null );
            }
            else
            {
                existingRegistration = m_componentHoldersByName.get( key );
            }
        }

        // there was a registration already, throw an exception and use the
        // existing registration to provide more information if possible
        if ( present )
        {
            String message = "The component name '" + name + "' has already been registered";

            if ( existingRegistration != null )
            {
                Bundle cBundle = existingRegistration.getActivator().getBundleContext().getBundle();
                ComponentMetadata cMeta = existingRegistration.getComponentMetadata();

                StringBuffer buf = new StringBuffer( message );
                buf.append( " by Bundle " ).append( cBundle.getBundleId() );
                if ( cBundle.getSymbolicName() != null )
                {
                    buf.append( " (" ).append( cBundle.getSymbolicName() ).append( ")" );
                }
                buf.append( " as Component of Class " ).append( cMeta.getImplementationClassName() );
                message = buf.toString();
            }

            throw new ComponentException( message );
        }

        return key;
    }


    /**
     * Registers the given component under the given name. If the name has not
     * already been reserved calling {@link #checkComponentName(String)} this
     * method throws a {@link ComponentException}.
     *
     * @param name The name to register the component under
     * @param component The component to register
     *
     * @throws ComponentException if the name has not been reserved through
     *      {@link #checkComponentName(String)} yet.
     */
    final void registerComponentHolder( final ComponentRegistryKey key, ComponentHolder component )
    {
        synchronized ( m_componentHoldersByName )
        {
            // only register the component if there is a m_registration for it !
            if ( m_componentHoldersByName.get( key ) != null )
            {
                // this is not expected if all works ok
                throw new ComponentException( "The component name '" + component.getComponentMetadata().getName()
                    + "' has already been registered." );
            }

            m_componentHoldersByName.put( key, component );
        }

        synchronized (m_componentHoldersByPid)
        {
            // See if the component declares a specific configuration pid (112.4.4 configuration-pid)
            String configurationPid = component.getComponentMetadata().getConfigurationPid();

            // Since several components may refer to the same configuration pid, we have to
            // store the component holder in a Set, in order to be able to lookup every
            // components from a given pid.
            Set<ComponentHolder> set = m_componentHoldersByPid.get(configurationPid);
            if (set == null)
            {
                set = new HashSet<ComponentHolder>();
                m_componentHoldersByPid.put(configurationPid, set);
            }
            set.add(component);
        }
    }

    /**
     * Returns the component registered under the given name or <code>null</code>
     * if no component is registered yet.
     */
    public final ComponentHolder getComponentHolder( final Bundle bundle, final String name )
    {
        ComponentHolder entry;
        synchronized ( m_componentHoldersByName )
        {
            entry = m_componentHoldersByName.get( new ComponentRegistryKey( bundle, name ) );
        }

        // only return the entry if non-null and not a reservation
        if ( entry instanceof ComponentHolder )
        {
            return ( ComponentHolder ) entry;
        }

        return null;
    }

    /**
     * Returns the list of ComponentHolder instances whose configuration pids are matching
     * the given pid.
     * @param pid the pid candidate
     * @return a iterator of ComponentHolder, or an empty iterator if no ComponentHolders
     * are found
     */
    public final Iterator<ComponentHolder> getComponentHoldersByPid(String pid)
    {
        Set<ComponentHolder> componentHoldersUsingPid = new HashSet<ComponentHolder>();
        synchronized (m_componentHoldersByPid)
        {
            Set<ComponentHolder> set = m_componentHoldersByPid.get(pid);
            // only return the entry if non-null and not a reservation
            if (set != null)
            {
                componentHoldersUsingPid.addAll(set);
            }
        }
        return componentHoldersUsingPid.iterator();
    }

    /**
     * Returns an array of all values currently stored in the component holders
     * map. The entries in the array are either String types for component
     * name reservations or {@link ComponentHolder} instances for actual
     * holders of components.
     */
    private ComponentHolder[] getComponentHolders()
    {
        synchronized ( m_componentHoldersByName )
        {
            return m_componentHoldersByName.values().toArray( new ComponentHolder[ m_componentHoldersByName.size() ]);
        }
    }


    /**
     * Removes the component registered under that name. If no component is
     * yet registered but the name is reserved, it is unreserved.
     * <p>
     * After calling this method, the name can be reused by other components.
     */
    final void unregisterComponentHolder( final Bundle bundle, final String name )
    {
        unregisterComponentHolder( new ComponentRegistryKey( bundle, name ) );
    }


    /**
     * Removes the component registered under that name. If no component is
     * yet registered but the name is reserved, it is unreserved.
     * <p>
     * After calling this method, the name can be reused by other components.
     */
    final void unregisterComponentHolder( final ComponentRegistryKey key )
    {
        ComponentHolder component;
        synchronized ( m_componentHoldersByName )
        {
            component = m_componentHoldersByName.remove( key );
        }

        if (component != null) {
            synchronized (m_componentHoldersByPid)
            {
                String configurationPid = component.getComponentMetadata().getConfigurationPid();
                Set<ComponentHolder> componentsForPid = m_componentHoldersByPid.get(configurationPid);
                if (componentsForPid != null)
                {
                    componentsForPid.remove(component);
                    if (componentsForPid.size() == 0)
                    {
                        m_componentHoldersByPid.remove(configurationPid);
                    }
                }
            }
        }
    }

    //---------- base configuration support

    /**
     * Factory method to issue {@link ComponentHolder} instances to manage
     * components described by the given component <code>metadata</code>.
     */
    public ComponentHolder createComponentHolder( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        ComponentHolder holder;

        if (metadata.isFactory())
        {
            // 112.2.4 SCR must register a Component Factory
            // service on behalf ot the component
            // as soon as the component factory is satisfied
            if ( !activator.getConfiguration().isFactoryEnabled() )
            {
                holder = new ComponentFactoryImpl(activator, metadata );
            }
            else
            {
                holder = new ConfigurationComponentFactoryImpl(activator, metadata );
            }
        }
        else
        {
            holder = new ImmediateComponentHolder(activator, metadata);
        }

        if (configurationSupport != null)
        {
            configurationSupport.configureComponentHolder(holder);
        }

        return holder;
    }


    //---------- ServiceListener

    /**
     * Called if the Configuration Admin service changes state. This
     * implementation is mainly interested in the Configuration Admin service
     * being registered <i>after</i> the Declarative Services setup to be able
     * to forward existing configuration.
     *
     * @param event The service change event
     */
    public void serviceChanged(ServiceEvent event)
    {
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            ConfigurationSupport configurationSupport = getOrCreateConfigurationSupport();

            final ServiceReference caRef = event.getServiceReference();
            final Object service = m_bundleContext.getService(caRef);
            if (service != null)
            {
                try
                {
                    configurationSupport.configureComponentHolders(caRef, service);
                }
                finally
                {
                    m_bundleContext.ungetService(caRef);
                }
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            disposeConfigurationSupport();
        }
    }

    //---------- Helper method

    /**
     * Returns <code>true</code> if the <code>bundle</code> is to be considered
     * active from the perspective of declarative services.
     * <p>
     * As of R4.1 a bundle may have lazy activation policy which means a bundle
     * remains in the STARTING state until a class is loaded from that bundle
     * (unless that class is declared to not cause the bundle to start). And
     * thus for DS 1.1 this means components are to be loaded for lazily started
     * bundles being in the STARTING state (after the LAZY_ACTIVATION event) has
     * been sent.  Hence DS must consider a bundle active when it is really
     * active and when it is a lazily activated bundle in the STARTING state.
     *
     * @param bundle The bundle check
     * @return <code>true</code> if <code>bundle</code> is not <code>null</code>
     *          and the bundle is either active or has lazy activation policy
     *          and is in the starting state.
     *
     * @see <a href="https://issues.apache.org/jira/browse/FELIX-1666">FELIX-1666</a>
     */
    static boolean isBundleActive( final Bundle bundle )
    {
        if ( bundle != null )
        {
            if ( bundle.getState() == Bundle.ACTIVE )
            {
                return true;
            }

            if ( bundle.getState() == Bundle.STARTING )
            {
                // according to the spec the activationPolicy header is only
                // set to request a bundle to be lazily activated. So in this
                // simple check we just verify the header is set to assume
                // the bundle is considered a lazily activated bundle
                return bundle.getHeaders().get( Constants.BUNDLE_ACTIVATIONPOLICY ) != null;
            }
        }

        // fall back: bundle is not considered active
        return false;
    }

    private ConfigurationSupport getOrCreateConfigurationSupport()
    {
        if (configurationSupport == null)
        {
            configurationSupport = new ConfigurationSupport(m_bundleContext, this);
        }
        return configurationSupport;
    }

    private void disposeConfigurationSupport()
    {
        if (configurationSupport != null)
        {
            this.configurationSupport.dispose();
            this.configurationSupport = null;
        }
    }

    public void missingServicePresent( final ServiceReference serviceReference, ComponentActorThread actor )
    {
        final List<Entry> dependencyManagers = m_missingDependencies.remove( serviceReference );
        if ( dependencyManagers != null )
        {
            actor.schedule( new Runnable()
            {

                public void run()
                {
                    for ( Entry entry : dependencyManagers )
                    {
                        entry.getDm().invokeBindMethodLate( serviceReference, entry.getTrackingCount() );
                    }
                }

                @Override
                public String toString()
                {
                    return "Late binding task of reference " + serviceReference + " for dependencyManagers " + dependencyManagers;
                }

            } );
        }
    }

    public synchronized void registerMissingDependency( DependencyManager dependencyManager, ServiceReference serviceReference, int trackingCount )
    {
        //check that the service reference is from scr
        if ( serviceReference.getProperty( ComponentConstants.COMPONENT_NAME ) == null || serviceReference.getProperty( ComponentConstants.COMPONENT_ID ) == null )
        {
            return;
        }
        List<Entry> dependencyManagers = m_missingDependencies.get( serviceReference );
        if ( dependencyManagers == null )
        {
            dependencyManagers = new ArrayList<Entry>();
            m_missingDependencies.put( serviceReference, dependencyManagers );
        }
        dependencyManagers.add( new Entry( dependencyManager, trackingCount ) );
    }

    private static class Entry
    {
        private final DependencyManager dm;
        private final int trackingCount;

        private Entry( DependencyManager dm, int trackingCount )
        {
            this.dm = dm;
            this.trackingCount = trackingCount;
        }

        public DependencyManager getDm()
        {
            return dm;
        }

        public int getTrackingCount()
        {
            return trackingCount;
        }
    }

}
