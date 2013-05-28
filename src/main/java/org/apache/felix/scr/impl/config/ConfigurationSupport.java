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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.impl.Activator;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.log.LogService;

public class ConfigurationSupport implements ConfigurationListener
{
    private static final ChangeCount changeCounter;
    static
    {
        ChangeCount cc = null;
        try
        {
            Configuration.class.getMethod( "getChangeCount", null );
            cc = new R5ChangeCount();
        }
        catch ( SecurityException e )
        {
        }
        catch ( NoSuchMethodException e )
        {
        }
        if ( cc == null )
        {
            cc = new R4ChangeCount();
        }
        changeCounter = cc;
    }

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
                            final Collection<Configuration> factory = findFactoryConfigurations(ca, confPid, bundleContext.getBundle());
                            if (!factory.isEmpty())
                            {
                                for (Configuration config: factory)
                                {
                                    config = getConfiguration( ca, config.getPid() );
                                    if ( checkBundleLocation( config, bundleContext.getBundle() ))
                                    {
                                        long changeCount = changeCounter.getChangeCount( config, false, -1 );
                                        holder.configurationUpdated(config.getPid(), config.getProperties(), changeCount, new TargetedPID(config.getFactoryPid()));
                                    }
                                }
                            }
                            else
                            {
                                // check for configuration and configure the holder
                                Configuration singleton = findSingletonConfiguration(ca, confPid, bundleContext.getBundle());
                                if (singleton != null)
                                {
                                    singleton = getConfiguration( ca, singleton.getPid() );
                                    if ( singleton != null && checkBundleLocation( singleton, bundleContext.getBundle() ))
                                    {
                                    long changeCount = changeCounter.getChangeCount( singleton, false, -1 );
                                    holder.configurationUpdated(confPid, singleton.getProperties(), changeCount, new TargetedPID(singleton.getPid()));
                                    }
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
        final TargetedPID pid = new TargetedPID( event.getPid());
        String rawFactoryPid = event.getFactoryPid();
        final TargetedPID factoryPid = rawFactoryPid == null? null: new TargetedPID( rawFactoryPid);

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
                    componentHolder.configurationDeleted(pid.getServicePid());
                    //fall back to less-strong pid match
                    configureComponentHolder( componentHolder );
                    break;

                case ConfigurationEvent.CM_UPDATED:
                {
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

                    TargetedPID targetedPid = factoryPid == null? pid: factoryPid;
                    TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID();
                    if ( targetedPid.equals(oldTargetedPID) || targetedPid.bindsStronger( oldTargetedPID ))
                    {
                        final ConfigurationInfo configInfo = getConfiguration( pid, componentHolder, bundleContext );
                        if ( configInfo != null )
                        {
                            Dictionary<String, Object> props = null;
                            long changeCount = -1;
                            try
                            {
                                Configuration config = configInfo.getConfiguration();
                                if ( checkBundleLocation( config, bundleContext.getBundle() ) )
                                {
                                    //If this is replacing a weaker targetedPID delete the old one.
                                    if ( !targetedPid.equals( oldTargetedPID ) && oldTargetedPID != null )
                                    {
                                        componentHolder.configurationDeleted( pid.getServicePid() );
                                    }
                                    changeCount = changeCounter.getChangeCount( config, true,
                                            componentHolder.getChangeCount( pid.getServicePid() ) );
                                    props = config.getProperties();
                                }
                            }
                            finally 
                            {
                                bundleContext.ungetService( configInfo.getRef() );
                            }
                            componentHolder.configurationUpdated( pid.getServicePid(), props,
                                    changeCount, targetedPid );
                        }
                    }

                    break;
                }
                case ConfigurationEvent.CM_LOCATION_CHANGED:
                {
                    //TODO is this logic correct for factory pids????
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

                    TargetedPID targetedPid = factoryPid == null? pid: factoryPid;
                    TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID();
                    if ( targetedPid.equals(oldTargetedPID))
                    {
                        //this sets the location to this component's bundle if not already set.  OK here
                        //since it used to be set to this bundle, ok to reset it
                        final ConfigurationInfo configInfo = getConfiguration( pid, componentHolder, bundleContext );
                        if ( configInfo != null )
                        {
                            boolean reconfigure = false;
                            try
                            {
                                Configuration config = configInfo.getConfiguration();
                                //this config was used on this component.  Does it still match?
                                reconfigure = !checkBundleLocation( config, bundleContext.getBundle() );
                            }
                            finally
                            {
                                bundleContext.ungetService( configInfo.getRef() );
                            }
                            if ( reconfigure )
                            {
                                //no, delete it
                                componentHolder.configurationDeleted( pid.getServicePid() );
                                //maybe there's another match
                                configureComponentHolder( componentHolder );

                            }
                        }
                        //else still matches
                        break;
                    }
                    boolean better = targetedPid.bindsStronger( oldTargetedPID );
                    if ( better )
                    {
                        //this sets the location to this component's bundle if not already set.  OK here
                        //because if it is set to this bundle we will use it.
                        final ConfigurationInfo configInfo = getConfiguration( pid, componentHolder, bundleContext );
                        if ( configInfo != null )
                        {
                            Dictionary<String, Object> props = null;
                            long changeCount = -1;
                            try
                            {
                                Configuration config = configInfo.getConfiguration();
                                //this component was not configured with this config.  Should it be now?
                                if ( checkBundleLocation( config, bundleContext.getBundle() ) )
                                {
                                    if ( oldTargetedPID != null )
                                    {
                                        //this is a better match, delete old before setting new
                                        componentHolder.configurationDeleted( pid.getServicePid() );
                                    }
                                    changeCount = changeCounter.getChangeCount( config, true,
                                            componentHolder.getChangeCount( pid.getServicePid() ) );
                                    props = config.getProperties();
                                }
                            }
                            finally
                            {
                                bundleContext.ungetService( configInfo.getRef() );
                            }
                            componentHolder.configurationUpdated( pid.getServicePid(), props,
                                    changeCount, pid );
                        }
                    }
                    //else worse match, do nothing
                    break;
                }
                default:
                    Activator.log(LogService.LOG_WARNING, null, "Unknown ConfigurationEvent type " + event.getType(),
                        null);
                }
            }
        }
    }
    
    private static class ConfigurationInfo
    {
        private final Configuration configuration;
        private final ServiceReference<?> ref;
        public ConfigurationInfo(Configuration configuration, ServiceReference<?> ref)
        {
            super();
            this.configuration = configuration;
            this.ref = ref;
        }
        public Configuration getConfiguration()
        {
            return configuration;
        }
        
        public ServiceReference<?> getRef()
        {
            return ref;
        }
    }

    private ConfigurationInfo getConfiguration(final TargetedPID pid, ComponentHolder componentHolder,
            final BundleContext bundleContext)
    {
        ConfigurationInfo confInfo = null;
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
                            final Configuration config = getConfiguration( ca, pid.getRawPid() );
                            if (config != null)
                            {
                                confInfo = new ConfigurationInfo(config, caRef);
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
                        if ( confInfo == null )
                        {
                            bundleContext.ungetService( caRef );
                        }
                    }
                }
            }
            catch (IllegalStateException ise)
            {
                // If the bundle has been stopped concurrently
                Activator.log(LogService.LOG_WARNING, null, "Bundle in unexpected state",
                    ise);
            }
        }
        return confInfo;
    }    

    private Configuration getConfiguration(final ConfigurationAdmin ca, final String pid)
    {
        try
        {
            return ca.getConfiguration(pid);
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
     * @param bundle TODO
     * @return configuration with the specified Pid
     */
    public Configuration findSingletonConfiguration(final ConfigurationAdmin ca, final String pid, Bundle bundle)
    {
        final String filter = getTargetedPidFilter( pid, bundle, Constants.SERVICE_PID );
        final Configuration[] cfg = findConfigurations(ca, filter);
        if (cfg == null)
        {
            return null;
        }
        String longest = null;
        Configuration best = null;
        for (Configuration config: cfg)
        {
            if ( checkBundleLocation( config, bundle ) )
            {
                String testPid = config.getPid();
                if ( longest == null || testPid.length() > longest.length())
                {
                    longest = testPid;
                    best = config;
                }
            }
            
        }
        return best;
    }

    /**
     * Returns all configurations whose factory PID equals the given factory PID
     * or <code>null</code> if no such configurations exist
     *
     * @param ca ConfigurationAdmin service
     * @param factoryPid factory Pid we want the configurations for
     * @param bundle bundle we're working for (for location and location permission)
     * @return the configurations specifying the supplied factory Pid.
     */
    private Collection<Configuration> findFactoryConfigurations(final ConfigurationAdmin ca, final String factoryPid, Bundle bundle)
    {
        final String filter = getTargetedPidFilter( factoryPid, bundle, ConfigurationAdmin.SERVICE_FACTORYPID );
        Configuration[] configs = findConfigurations(ca, filter);
        if (configs == null)
        {
            return Collections.emptyList();
        }
        Map<String, Configuration> configsByPid = new HashMap<String, Configuration>();
        for (Configuration config: configs)
        {
            if ( checkBundleLocation( config, bundle ) )
            {
                Configuration oldConfig = configsByPid.get( config.getPid() );
                if ( oldConfig == null )
                {
                    configsByPid.put( config.getPid(), config );
                }
                else
                {
                    String newPid = config.getFactoryPid();
                    String oldPid = oldConfig.getFactoryPid();
                    if ( newPid.length() > oldPid.length() )
                    {
                        configsByPid.put( config.getPid(), config );
                    }
                }
            }
        }
        return configsByPid.values();
    }
    
    private boolean checkBundleLocation(Configuration config, Bundle bundle)
    {
        if (config == null)
        {
            return false;
        }
        String configBundleLocation = config.getBundleLocation();
        if ( configBundleLocation == null)
        {
            return true;
        }
        if (configBundleLocation.startsWith( "?" ))
        {
            //multilocation
            return bundle.hasPermission(new ConfigurationPermission(configBundleLocation, ConfigurationPermission.TARGET));
        }
        return configBundleLocation.equals(bundle.getLocation());
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
    
    private String getTargetedPidFilter(String pid, Bundle bundle, String key)
    {
        String bsn = bundle.getSymbolicName();
        String version = bundle.getVersion().toString();
        String location = escape(bundle.getLocation());
        String f = String.format(
                "(|(%1$s=%2$s)(%1$s=%2$s|%3$s)(%1$s=%2$s|%3$s|%4$s)(%1$s=%2$s|%3$s|%4$s|%5$s))", 
                key, pid, bsn, version, location );
        return f;
    }
    
    /**
     * see core spec 3.2.7.  Escape \*() with preceding \
     * @param value
     * @return escaped string
     */
    static final String escape(String value)
    {
        return value.replaceAll( "([\\\\\\*\\(\\)])", "\\\\$1" );
    }
    
    
    private interface ChangeCount {
        long getChangeCount( Configuration configuration, boolean fromEvent, long previous );
    }
    
    private static class R5ChangeCount implements ChangeCount {

        public long getChangeCount(Configuration configuration, boolean fromEvent, long previous)
        {
            return configuration.getChangeCount();
        }
    }   
    
    private static class R4ChangeCount implements ChangeCount {

        public long getChangeCount(Configuration configuration, boolean fromEvent, long previous)
        {
            return fromEvent? previous + 1:0;
        }
        
    }
}
