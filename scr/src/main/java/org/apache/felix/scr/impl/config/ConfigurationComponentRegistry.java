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


import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.impl.Activator;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.manager.ComponentFactoryImpl;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.log.LogService;


public class ConfigurationComponentRegistry extends ComponentRegistry implements ServiceListener, ConfigurationListener
{

    // the name of the ConfigurationAdmin service
    private static final String CONFIGURATION_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";

    // the service m_registration of the ConfigurationListener service
    private ServiceRegistration m_registration;

    // the bundle context
    private BundleContext m_bundleContext;


    public ConfigurationComponentRegistry( final BundleContext context )
    {
        super( context );
        m_bundleContext = context;

        // register as listener for configurations
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Declarative Services Configuration Support Listener" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        m_registration = context.registerService( new String[]
            { ConfigurationListener.class.getName() }, this, props );

        // keep me informed on ConfigurationAdmin state changes
        try
        {
            context.addServiceListener( this, "(objectclass=" + CONFIGURATION_ADMIN + ")" );
        }
        catch ( InvalidSyntaxException ise )
        {
            // not expected (filter is tested valid)
        }
    }


    public void dispose()
    {
        m_bundleContext.removeServiceListener( this );

        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }

        super.dispose();
    }


    //---------- BaseConfigurationSupport overwrites

    public ComponentHolder createComponentHolder( final BundleComponentActivator activator,
        final ComponentMetadata metadata )
    {
        // 112.7 configure unless configuration not required
        if ( metadata.isConfigurationIgnored() )
        {
            return super.createComponentHolder( activator, metadata );
        }

        // prepare the configuration holder
        final ComponentHolder holder;
        if ( metadata.isFactory() )
        {
            holder = new ComponentFactoryImpl( activator, metadata );
        }
        else
        {
            holder = new ConfiguredComponentHolder( activator, metadata );
        }

        final BundleContext bundleContext = activator.getBundleContext();
        final String bundleLocation = bundleContext.getBundle().getLocation();
        final String name = metadata.getName();

        final ServiceReference caRef = bundleContext.getServiceReference( CONFIGURATION_ADMIN );
        if ( caRef != null )
        {
            final ConfigurationAdmin ca = ( ConfigurationAdmin ) bundleContext.getService( caRef );
            if ( ca != null )
            {
                try
                {
                    final Configuration[] factory = findFactoryConfigurations( ca, name );
                    if ( factory != null )
                    {
                        for ( int i = 0; i < factory.length; i++ )
                        {
                            final String pid = factory[i].getPid();
                            final Dictionary props = getConfiguration( ca, pid, bundleLocation );
                            holder.configurationUpdated( pid, props );
                        }
                    }
                    else
                    {
                        // check for configuration and configure the holder
                        final Configuration singleton = findSingletonConfiguration( ca, name );
                        if ( singleton != null )
                        {
                            final Dictionary props = getConfiguration( ca, name, bundleLocation );
                            holder.configurationUpdated( name, props );
                        }
                    }
                }
                finally
                {
                    bundleContext.ungetService( caRef );
                }
            }
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
    public void serviceChanged( ServiceEvent event )
    {
        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            Configuration[] configs = null;
            final ServiceReference caRef = event.getServiceReference();
            final Object service = m_bundleContext.getService( caRef );
            try
            {
                if ( service instanceof ConfigurationAdmin )
                {
                    configs = findConfigurations( ( ConfigurationAdmin ) service, null );
                }
            }
            finally
            {
                if ( service != null )
                {
                    m_bundleContext.ungetService( caRef );
                }
            }

            if ( configs != null )
            {
                for ( int i = 0; i < configs.length; i++ )
                {
                    ConfigurationEvent cfgEvent = new ConfigurationEvent( caRef, ConfigurationEvent.CM_UPDATED,
                        configs[i].getFactoryPid(), configs[i].getPid() );
                    configurationEvent( cfgEvent );
                }
            }
        }
    }


    //---------- ConfigurationListener

    /**
     * Called by the Configuration Admin service if a configuration is updated
     * or removed.
     * <p>
     * This method is really only called upon configuration changes; it is not
     * called for existing configurations upon startup of the Configuration
     * Admin service. To bridge this gap, the
     * {@link #serviceChanged(ServiceEvent)} method called when the
     * Configuration Admin service is registered calls this method for all
     * existing configurations to be able to foward existing configurations
     * to components.
     *
     * @param event The configuration change event
     */
    public void configurationEvent( ConfigurationEvent event )
    {
        final String pid = event.getPid();
        final String factoryPid = event.getFactoryPid();

        final ComponentHolder cm;
        if ( factoryPid == null )
        {
            cm = getComponentHolder( pid );
        }
        else
        {
            cm = getComponentHolder( factoryPid );
        }

        Activator.log( LogService.LOG_DEBUG, null, "configurationEvent: Handling "
            + ( ( event.getType() == ConfigurationEvent.CM_DELETED ) ? "DELETE" : "UPDATE" ) + " of Configuration PID="
            + pid, null );

        if ( cm != null && !cm.getComponentMetadata().isConfigurationIgnored() )
        {
            switch ( event.getType() )
            {
                case ConfigurationEvent.CM_DELETED:
                    cm.configurationDeleted( pid );
                    break;

                case ConfigurationEvent.CM_UPDATED:
                    final BundleComponentActivator activator = cm.getActivator();
                    if ( activator == null )
                    {
                        break;
                    }

                    final BundleContext bundleContext = activator.getBundleContext();
                    if ( bundleContext == null )
                    {
                        break;
                    }

                    final ServiceReference caRef = bundleContext.getServiceReference( CONFIGURATION_ADMIN );
                    if ( caRef != null )
                    {
                        try
                        {
                            final ConfigurationAdmin ca = ( ConfigurationAdmin ) bundleContext.getService( caRef );
                            if ( ca != null )
                            {
                                try
                                {
                                    final Dictionary dict = getConfiguration( ca, pid, bundleContext.getBundle()
                                        .getLocation() );
                                    if ( dict != null )
                                    {
                                        cm.configurationUpdated( pid, dict );
                                    }
                                }
                                finally
                                {
                                    bundleContext.ungetService( caRef );
                                }
                            }
                        }
                        catch ( IllegalStateException ise )
                        {
                            // If the bundle has been stopped conurrently
                        }
                    }
                    break;

                default:
                    Activator.log( LogService.LOG_WARNING, null, "Unknown ConfigurationEvent type " + event.getType(),
                        null );
            }
        }
    }


    private Dictionary getConfiguration( final ConfigurationAdmin ca, final String pid, final String bundleLocation )
    {
        try
        {
            final Configuration cfg = ca.getConfiguration( pid );
            if ( bundleLocation.equals( cfg.getBundleLocation() ) || Activator.hasCtWorkaround() )
            {
                return cfg.getProperties();
            }

            // configuration belongs to another bundle, cannot be used here
            Activator.log( LogService.LOG_ERROR, null, "Cannot use configuration pid=" + pid + " for bundle "
                + bundleLocation + " because it belongs to bundle " + cfg.getBundleLocation(), null );
        }
        catch ( IOException ioe )
        {
            Activator.log( LogService.LOG_WARNING, null, "Failed reading configuration for pid=" + pid, ioe );
        }

        return null;
    }


    /**
     * Returns the configuration whose PID equals the given pid. If no such
     * configuration exists, <code>null</code> is returned.
     * @param ctx
     * @param pid
     * @return
     */
    public Configuration findSingletonConfiguration( final ConfigurationAdmin ca, final String pid )
    {
        final String filter = "(service.pid=" + pid + ")";
        final Configuration[] cfg = findConfigurations( ca, filter );
        return ( cfg == null || cfg.length == 0 ) ? null : cfg[0];
    }


    /**
     * Returns all configurations whose factory PID equals the given factory PID
     * or <code>null</code> if no such configurations exist
     * @param ctx
     * @param factoryPid
     * @return
     */
    public Configuration[] findFactoryConfigurations( final ConfigurationAdmin ca, final String factoryPid )
    {
        final String filter = "(service.factoryPid=" + factoryPid + ")";
        return findConfigurations( ca, filter );
    }


    private Configuration[] findConfigurations( final ConfigurationAdmin ca, final String filter )
    {
        try
        {
            return ca.listConfigurations( filter );
        }
        catch ( IOException ioe )
        {
            Activator.log( LogService.LOG_WARNING, null, "Problem listing configurations for filter=" + filter, ioe );
        }
        catch ( InvalidSyntaxException ise )
        {
            Activator.log( LogService.LOG_ERROR, null, "Invalid Configuration selection filter " + filter, ise );
        }

        // no factories in case of problems
        return null;
    }
}
