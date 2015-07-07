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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
            Configuration.class.getMethod( "getChangeCount", (Class<?>[]) null );
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
    private ServiceRegistration<?> m_registration;
    
    public ConfigurationSupport(final BundleContext bundleContext, final ComponentRegistry registry)
    {
        this.m_registry = registry;

        // register as listener for configurations
        Dictionary<String, Object> props = new Hashtable<String, Object>();
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

    public boolean configureComponentHolder(final ComponentHolder<?> holder)
    {

        // 112.7 configure unless configuration not required
        if (!holder.getComponentMetadata().isConfigurationIgnored())
        {
            final BundleContext bundleContext = holder.getActivator().getBundleContext();
            if ( bundleContext == null )
            {
                return false;// bundle was stopped concurrently with configuration deletion
            }
            final List<String> confPids = holder.getComponentMetadata().getConfigurationPid();

            final ServiceReference<?> caRef = bundleContext.getServiceReference(ComponentRegistry.CONFIGURATION_ADMIN);
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
                            for (final String confPid : confPids )
                            {
                                final Collection<Configuration> factory = findFactoryConfigurations( ca, confPid,
                                        bundleContext.getBundle() );
                                if ( !factory.isEmpty() )
                                {
                                    for ( Configuration config: factory )
                                    {
                                        boolean created = false;
                                        Activator.log( LogService.LOG_DEBUG, null,
                                                "Configuring holder {0} with factory configuration {1}", new Object[] {
                                                        holder, config }, null );
                                        config = getConfiguration( ca, config.getPid() );
                                        if ( checkBundleLocation( config, bundleContext.getBundle() ) )
                                        {
                                            long changeCount = changeCounter.getChangeCount( config, false, -1 );
                                            created |= holder.configurationUpdated( new TargetedPID( config.getPid() ), 
                                            		new TargetedPID( config.getFactoryPid() ),
                                                    config.getProperties(),
                                                    changeCount );
                                        }
                                        if ( !created ) 
                                        {
                                        	return false;
                                        }
                                    }
                                }
                                else
                                {
                                    // check for configuration and configure the holder
                                    Configuration singleton = findSingletonConfiguration( ca, confPid,
                                            bundleContext.getBundle() );
                                    if ( singleton != null )
                                    {
                                        singleton = getConfiguration( ca, singleton.getPid() );
                                        Activator.log( LogService.LOG_DEBUG, null,
                                                "Configuring holder {0} with configuration {1}", new Object[] { holder,
                                                        singleton }, null );
                                        if ( singleton != null
                                                && checkBundleLocation( singleton, bundleContext.getBundle() ) )
                                        {
                                            long changeCount = changeCounter.getChangeCount( singleton, false, -1 );
                                            holder.configurationUpdated( new TargetedPID( singleton.getPid() ), null,
                                                    singleton.getProperties(), changeCount );
                                        }
                                        else
                                        {
                                        	return false;
                                        }
                                    }
                                    else 
                                    {
                                    	return false;
                                    }
                                }
                            }
                            return !confPids.isEmpty();
                        }
                        else
                        {
                            Activator.log( LogService.LOG_WARNING, null, "Cannot configure component {0}",
                                 new Object[] {holder.getComponentMetadata().getName()}, null );
                            Activator.log( LogService.LOG_WARNING, null,
                                "Component Bundle's Configuration Admin is not compatible with "
                                    + "ours. This happens if multiple Configuration Admin API versions "
                                    + "are deployed and different bundles wire to different versions", null );
                            return false;

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
        return false;
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
        Collection<ComponentHolder<?>> holders;

        if (factoryPid == null)
        {
            holders = this.m_registry.getComponentHoldersByPid(pid);
        }
        else
        {
            holders = this.m_registry.getComponentHoldersByPid(factoryPid);
        }

        Activator.log(LogService.LOG_DEBUG, null, "configurationEvent: Handling {0}  of Configuration PID={1} for component holders {2}",
                new Object[] {getEventType(event), pid, holders},
                null);

        for  ( ComponentHolder<?> componentHolder: holders )
        {
            if (!componentHolder.getComponentMetadata().isConfigurationIgnored())
            {
                switch (event.getType()) {
                case ConfigurationEvent.CM_DELETED:
                    if ( factoryPid != null || !configureComponentHolder( componentHolder ) )
                    {
                        componentHolder.configurationDeleted( pid, factoryPid );
                    }
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
                    TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID(pid, factoryPid);
                    if ( factoryPid != null || targetedPid.equals(oldTargetedPID) || targetedPid.bindsStronger( oldTargetedPID ))
                    {
                        final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid, componentHolder, bundleContext );
                        if ( checkBundleLocation( configInfo.getBundleLocation(), bundleContext.getBundle() ) )
                        {
                            //If this is replacing a weaker targetedPID delete the old one.
                            if ( factoryPid == null && !targetedPid.equals(oldTargetedPID) && oldTargetedPID != null)
                            {
                                componentHolder.configurationDeleted( pid, factoryPid );
                            }
                            componentHolder.configurationUpdated( pid, factoryPid, configInfo.getProps(), configInfo.getChangeCount() );
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
                    TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID(pid, factoryPid);
                    if ( targetedPid.equals(oldTargetedPID))
                    {
                        //this sets the location to this component's bundle if not already set.  OK here
                        //since it used to be set to this bundle, ok to reset it
                        final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid, componentHolder, bundleContext );
                        Activator.log(LogService.LOG_DEBUG, null, "LocationChanged event, same targetedPID {0}, location now {1}",
                                new Object[] {targetedPid, configInfo.getBundleLocation()},
                                null);
                        if (configInfo.getProps() == null)
                        {
                            throw new IllegalStateException("Existing Configuration with pid " + pid + 
                                    " has had its properties set to null and location changed.  We expected a delete event first.");
                        }
                        //this config was used on this component.  Does it still match?
                        if (!checkBundleLocation( configInfo.getBundleLocation(), bundleContext.getBundle() ))
                        {
                            //no, delete it
                            componentHolder.configurationDeleted( pid, factoryPid );
                            //maybe there's another match
                            configureComponentHolder(componentHolder);
                            
                        }
                        //else still matches
                        break;
                    }
                    boolean better = targetedPid.bindsStronger( oldTargetedPID );
                    if ( better )
                    {
                        //this sets the location to this component's bundle if not already set.  OK here
                        //because if it is set to this bundle we will use it.
                        final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid, componentHolder, bundleContext );
                        Activator.log(LogService.LOG_DEBUG, null, "LocationChanged event, better targetedPID {0} compared to {1}, location now {2}",
                                new Object[] {targetedPid, oldTargetedPID, configInfo.getBundleLocation()},
                                null);
                        if (configInfo.getProps() == null)
                        {
                            //location has been changed before any properties are set.  We don't care.  Wait for an updated event with the properties
                            break;
                        }
                        //this component was not configured with this config.  Should it be now?
                        if ( checkBundleLocation( configInfo.getBundleLocation(), bundleContext.getBundle() ) )
                        {
                            if ( oldTargetedPID != null )
                            {
                                //this is a better match, delete old before setting new
                                componentHolder.configurationDeleted( pid, factoryPid );
                            }
                            componentHolder.configurationUpdated( pid, factoryPid,
                                    configInfo.getProps(), configInfo.getChangeCount() );
                        }
                    }
                    //else worse match, do nothing
                    else
                    {
                        Activator.log(LogService.LOG_DEBUG, null, "LocationChanged event, worse targetedPID {0} compared to {1}, do nothing",
                                new Object[] {targetedPid, oldTargetedPID},
                                null);
                    }
                    break;
                }
                default:
                    Activator.log(LogService.LOG_WARNING, null, "Unknown ConfigurationEvent type {0}", new Object[] {event.getType()},
                        null);
                }
            }
        }
    }

    
    private String getEventType(ConfigurationEvent event)
    {
        switch (event.getType())
        {
        case ConfigurationEvent.CM_UPDATED:
            return "UPDATED";
        case ConfigurationEvent.CM_DELETED:
            return "DELETED";
        case ConfigurationEvent.CM_LOCATION_CHANGED:
            return "LOCATION CHANGED";
        default:
            return "Unkown event type: " + event.getType();
        }

    }

    private static class ConfigurationInfo
    {
        private final Dictionary<String, Object> props;
        private final String bundleLocation;
        private final long changeCount;
        
        public ConfigurationInfo(Dictionary<String, Object> props, String bundleLocation, long changeCount)
        {
            this.props = props;
            this.bundleLocation = bundleLocation;
            this.changeCount = changeCount;
        }

        public long getChangeCount()
        {
            return changeCount;
        }

        public Dictionary<String, Object> getProps()
        {
            return props;
        }

        public String getBundleLocation()
        {
            return bundleLocation;
        }
        
    }

    /**
     * This gets config admin, gets the requested configuration, extracts the info we need from it, and ungets config admin.
     * Some versions of felix config admin do not allow access to configurations after the config admin instance they were obtained from
     * are ungot.  Extracting the info we need into "configInfo" solves this problem.
     * 
     * @param pid TargetedPID for the desired configuration
     * @param targetedPid the targeted factory pid for a factory configuration or the pid for a singleton configuration
     * @param componentHolder ComponentHolder that holds the old change count (for r4 fake change counting)
     * @param bundleContext BundleContext to get the CA from
     * @return ConfigurationInfo object containing the info we need from the configuration.
     */
    private ConfigurationInfo getConfigurationInfo(final TargetedPID pid, TargetedPID targetedPid,
            ComponentHolder<?> componentHolder, final BundleContext bundleContext)
    {
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
                            return new ConfigurationInfo(config.getProperties(), config.getBundleLocation(),
                                    changeCounter.getChangeCount( config, true, componentHolder.getChangeCount( pid, targetedPid ) ) );
                        }
                        else
                        {
                            Activator.log( LogService.LOG_WARNING, null, "Cannot reconfigure component {0}",
                                new Object[] {componentHolder.getComponentMetadata().getName()}, null );
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
                Activator.log(LogService.LOG_WARNING, null, "Bundle in unexpected state",
                    ise);
            }
        }
        return null;
    }

    private Configuration getConfiguration(final ConfigurationAdmin ca, final String pid)
    {
        try
        {
            return ca.getConfiguration(pid);
        }
        catch (IOException ioe)
        {
            Activator.log(LogService.LOG_WARNING, null, "Failed reading configuration for pid={0}", new Object[] {pid}, ioe);
        }

        return null;
    }

    /**
     * Returns the configuration whose PID equals the given pid. If no such
     * configuration exists, <code>null</code> is returned.
     *
     * @param ca Configuration Admin service
     * @param pid Pid for desired configuration
     * @param bundle bundle of the component we are configuring (used in targeted pids)
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
        return checkBundleLocation( configBundleLocation, bundle );
    }

    private boolean checkBundleLocation(String configBundleLocation, Bundle bundle)
    {
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
            Activator.log(LogService.LOG_WARNING, null, "Problem listing configurations for filter={0}", new Object[] {filter}, ioe);
        }
        catch (InvalidSyntaxException ise)
        {
            Activator.log(LogService.LOG_ERROR, null, "Invalid Configuration selection filter {0}", new Object[] {filter}, ise);
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
