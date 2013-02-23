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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.felix.scr.impl.Activator;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.log.LogService;

public class ConfigurationSupport implements ConfigurationListener
{

    // the registry of components to be configured
    private final ComponentRegistry m_registry;

    // the service m_registration of the ConfigurationListener service
    private ServiceRegistration m_registration;

    public ConfigurationSupport(final BundleContext bundleContext, final ComponentRegistry registry)
    {
        this.m_registry = registry;

        // register as listener for configurations
        Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_DESCRIPTION, "Declarative Services Configuration Support Listener");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.m_registration = bundleContext.registerService(new String[]
            { "org.osgi.service.cm.ConfigurationListener" }, this, props);
    }

    public void dispose()
    {
        if (this.m_registration != null)
        {
            this.m_registration.unregister();
            this.m_registration = null;
        }
    }

    // ---------- BaseConfigurationSupport overwrites

    public void configureComponentHolder(final ComponentHolder holder)
    {

        // 112.7 configure unless configuration not required
        if (!holder.getComponentMetadata().isConfigurationIgnored())
        {
            final BundleContext bundleContext = holder.getActivator().getBundleContext();
            final String bundleLocation = bundleContext.getBundle().getLocation();
            final String confPid = holder.getComponentMetadata().getConfigurationPid();

            final ServiceReference caRef = bundleContext.getServiceReference(ComponentRegistry.CONFIGURATION_ADMIN);
            if (caRef != null)
            {
                final Object cao = bundleContext.getService(caRef);
                if (cao != null)
                {
                    try
                    {
                        if ( cao instanceof ConfigurationAdmin )
                        {
                            final ConfigurationAdmin ca = ( ConfigurationAdmin ) cao;
                            final Configuration[] factory = findFactoryConfigurations(ca, confPid);
                            if (factory != null)
                            {
                                for (int i = 0; i < factory.length; i++)
                                {
                                    final String pid = factory[i].getPid();
                                    final Dictionary props = getConfiguration(ca, pid, bundleLocation);
                                    holder.configurationUpdated(pid, props);
                                }
                            }
                            else
                            {
                                // check for configuration and configure the holder
                                final Configuration singleton = findSingletonConfiguration(ca, confPid);
                                if (singleton != null)
                                {
                                    final Dictionary props = getConfiguration(ca, confPid, bundleLocation);
                                    holder.configurationUpdated(confPid, props);
                                }
                            }
                        }
                        else
                        {
                            Activator.log( LogService.LOG_WARNING, null, "Cannot configure component "
                                + holder.getComponentMetadata().getName(), null );
                            Activator.log( LogService.LOG_WARNING, null,
                                "Component Bundle's Configuration Admin is not compatible with "
                                    + "ours. This happens if multiple Configuration Admin API versions "
                                    + "are deployed and different bundles wire to different versions", null );

                            Class<?> caoc = cao.getClass();
                        }
                    }
                    finally
                    {
                        try
                        {
                            bundleContext.ungetService( caRef );
                        }
                        catch ( IllegalStateException e )
                        {
                            // ignore, bundle context was shut down during the above.
                        }
                    }
                }
            }
        }
    }

    // ---------- ServiceListener

    public void configureComponentHolders(final ServiceReference<ConfigurationAdmin> configurationAdminReference,
        final Object configurationAdmin)
    {
        if (configurationAdmin instanceof ConfigurationAdmin)
        {
            Configuration[] configs = findConfigurations((ConfigurationAdmin) configurationAdmin, null);
            if (configs != null)
            {
                for (int i = 0; i < configs.length; i++)
                {
                    ConfigurationEvent cfgEvent = new ConfigurationEvent(configurationAdminReference,
                        ConfigurationEvent.CM_UPDATED, configs[i].getFactoryPid(), configs[i].getPid());
                    configurationEvent(cfgEvent);
                }
            }
        }
    }

    // ---------- ConfigurationListener

    /**
     * Called by the Configuration Admin service if a configuration is updated
     * or removed.
     * <p>
     * This method is really only called upon configuration changes; it is not
     * called for existing configurations upon startup of the Configuration
     * Admin service. To bridge this gap, the
     * {@link ComponentRegistry#serviceChanged(org.osgi.framework.ServiceEvent)} method called when the
     * Configuration Admin service is registered calls #configureComponentHolders which calls this method for all
     * existing configurations to be able to forward existing configurations to
     * components.
     *
     * @param event The configuration change event
     */
    public void configurationEvent(ConfigurationEvent event)
    {
        final String pid = event.getPid();
        final String factoryPid = event.getFactoryPid();

        // iterate over all components which must be configured with this pid
        // (since DS 1.2, components may specify a specific configuration PID (112.4.4 configuration-pid)
        Collection<ComponentHolder> holders;

        if (factoryPid == null)
        {
            holders = this.m_registry.getComponentHoldersByPid(pid);
        }
        else
        {
            holders = this.m_registry.getComponentHoldersByPid(factoryPid);
        }

        Activator.log(LogService.LOG_DEBUG, null, "configurationEvent: Handling "
                + ((event.getType() == ConfigurationEvent.CM_DELETED) ? "DELETE" : "UPDATE")
                + " of Configuration PID=" + pid, null);

        for  ( ComponentHolder componentHolder: holders )
        {
            if (!componentHolder.getComponentMetadata().isConfigurationIgnored())
            {
                switch (event.getType()) {
                case ConfigurationEvent.CM_DELETED:
                    componentHolder.configurationDeleted(pid);
                    break;

                case ConfigurationEvent.CM_UPDATED:
                    final BundleComponentActivator activator = componentHolder.getActivator();
                    if (activator == null)
                    {
                        break;
                    }

                    final BundleContext bundleContext = activator.getBundleContext();
                    if (bundleContext == null)
                    {
                        break;
                    }

                    final ServiceReference caRef = bundleContext
                        .getServiceReference(ComponentRegistry.CONFIGURATION_ADMIN);
                    if (caRef != null)
                    {
                        try
                        {
                            final Object cao = bundleContext.getService(caRef);
                            if (cao != null)
                            {
                                try
                                {
                                    if ( cao instanceof ConfigurationAdmin )
                                    {
                                        final ConfigurationAdmin ca = ( ConfigurationAdmin ) cao;
                                        final Dictionary dict = getConfiguration( ca, pid, bundleContext
                                            .getBundle().getLocation() );
                                        if ( dict != null )
                                        {
                                            componentHolder.configurationUpdated( pid, dict );
                                        }
                                    }
                                    else
                                    {
                                        Activator.log( LogService.LOG_WARNING, null, "Cannot reconfigure component "
                                            + componentHolder.getComponentMetadata().getName(), null );
                                        Activator.log( LogService.LOG_WARNING, null,
                                            "Component Bundle's Configuration Admin is not compatible with " +
                                            "ours. This happens if multiple Configuration Admin API versions " +
                                            "are deployed and different bundles wire to different versions",
                                            null );
                                    }
                                }
                                finally
                                {
                                    bundleContext.ungetService( caRef );
                                }
                            }
                        }
                        catch (IllegalStateException ise)
                        {
                            // If the bundle has been stopped concurrently
                            Activator.log(LogService.LOG_WARNING, null, "Unknown ConfigurationEvent type " + event.getType(),
                                ise);
                        }
                    }
                    break;

                case ConfigurationEvent.CM_LOCATION_CHANGED:
                    // FELIX-3650: Don't log WARNING message
                    // FELIX-3584: Implement event support
                    break;

                default:
                    Activator.log(LogService.LOG_WARNING, null, "Unknown ConfigurationEvent type " + event.getType(),
                        null);
                }
            }
        }
    }

    private Dictionary getConfiguration(final ConfigurationAdmin ca, final String pid, final String bundleLocation)
    {
        try
        {
            final Configuration cfg = ca.getConfiguration(pid);
            if (bundleLocation.equals(cfg.getBundleLocation()))
            {
                return cfg.getProperties();
            }

            // configuration belongs to another bundle, cannot be used here
            Activator.log(LogService.LOG_ERROR, null, "Cannot use configuration pid=" + pid + " for bundle "
                + bundleLocation + " because it belongs to bundle " + cfg.getBundleLocation(), null);
        }
        catch (IOException ioe)
        {
            Activator.log(LogService.LOG_WARNING, null, "Failed reading configuration for pid=" + pid, ioe);
        }

        return null;
    }

    /**
     * Returns the configuration whose PID equals the given pid. If no such
     * configuration exists, <code>null</code> is returned.
     *
     * @param ca Configuration Admin service
     * @param pid Pid for desired configuration
     * @return configuration with the specified Pid
     */
    public Configuration findSingletonConfiguration(final ConfigurationAdmin ca, final String pid)
    {
        final String filter = "(service.pid=" + pid + ")";
        final Configuration[] cfg = findConfigurations(ca, filter);
        return (cfg == null || cfg.length == 0) ? null : cfg[0];
    }

    /**
     * Returns all configurations whose factory PID equals the given factory PID
     * or <code>null</code> if no such configurations exist
     *
     * @param ca ConfigurationAdmin service
     * @param factoryPid factory Pid we want the configurations for
     * @return the configurations specifying the supplied factory Pid.
     */
    public Configuration[] findFactoryConfigurations(final ConfigurationAdmin ca, final String factoryPid)
    {
        final String filter = "(service.factoryPid=" + factoryPid + ")";
        return findConfigurations(ca, filter);
    }

    private Configuration[] findConfigurations(final ConfigurationAdmin ca, final String filter)
    {
        try
        {
            return ca.listConfigurations(filter);
        }
        catch (IOException ioe)
        {
            Activator.log(LogService.LOG_WARNING, null, "Problem listing configurations for filter=" + filter, ioe);
        }
        catch (InvalidSyntaxException ise)
        {
            Activator.log(LogService.LOG_ERROR, null, "Invalid Configuration selection filter " + filter, ise);
        }

        // no factories in case of problems
        return null;
    }
}
