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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.metadata.TargetedPID;
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

public abstract class RegionConfigurationSupport implements ConfigurationListener
{

    private final SimpleLogger logger;
    private final ServiceReference<ConfigurationAdmin> caReference;
    private final BundleContext caBundleContext;
    private final Long bundleId;

    private final AtomicInteger referenceCount = new AtomicInteger( 1 );

    // the service m_registration of the ConfigurationListener service
    private ServiceRegistration<ConfigurationListener> m_registration;

    /**
     * 
     * @param bundleContext of the ConfigurationAdmin we are tracking
     * @param registry
     */
    public RegionConfigurationSupport(SimpleLogger logger, ServiceReference<ConfigurationAdmin> reference)
    {
        this.logger = logger;
        this.caReference = reference;
        Bundle bundle = reference.getBundle();
        this.bundleId = bundle.getBundleId();
        this.caBundleContext = bundle.getBundleContext();
    }

    public void start()
    {
        // register as listener for configurations
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put( Constants.SERVICE_DESCRIPTION, "Declarative Services Configuration Support Listener" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        this.m_registration = caBundleContext.registerService( ConfigurationListener.class, this, props );

    }

    public Long getBundleId()
    {
        return bundleId;
    }

    public boolean reference()
    {
        if ( referenceCount.get() == 0 )
        {
            return false;
        }
        referenceCount.incrementAndGet();
        return true;
    }

    public boolean dereference()
    {
        if ( referenceCount.decrementAndGet() == 0 )
        {
            this.m_registration.unregister();
            this.m_registration = null;
            return true;
        }
        return false;
    }

    /**
     * The return value is only relevant for the call from {@link #configurationEvent(ConfigurationEvent)}
     * in the case of a deleted configuration which is not a factory configuration!
     */
    public boolean configureComponentHolder(final ComponentHolder<?> holder)
    {

        // 112.7 configure unless configuration not required
        if ( !holder.getComponentMetadata().isConfigurationIgnored() )
        {
            final BundleContext bundleContext = holder.getActivator().getBundleContext();
            if ( bundleContext == null )
            {
                return false;// bundle was stopped concurrently with configuration deletion
            }
            final List<String> confPids = holder.getComponentMetadata().getConfigurationPid();

            final ConfigurationAdmin ca = getConfigAdmin( bundleContext );
            try
            {
                for ( final String confPid : confPids )
                {
                    final Collection<Configuration> factory = findFactoryConfigurations( ca, confPid,
                        bundleContext.getBundle() );
                    if ( !factory.isEmpty() )
                    {
                        boolean created = false;
                        for ( Configuration config : factory )
                        {
                            logger.log( LogService.LOG_DEBUG,
                                "Configuring holder {0} with factory configuration {1}, change count {2}",
                                new Object[] { holder, config, config.getChangeCount() }, null );
                            if ( checkBundleLocation( config, bundleContext.getBundle() ) )
                            {
                                long changeCount = config.getChangeCount();
                                created |= holder.configurationUpdated( new TargetedPID( config.getPid() ),
                                    new TargetedPID( config.getFactoryPid() ), config.getProperties(), changeCount );
                            }
                        }
                        if ( !created )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        // check for configuration and configure the holder
                        Configuration singleton = findSingletonConfiguration( ca, confPid, bundleContext.getBundle() );
                        if ( singleton != null )
                        {
                            logger.log( LogService.LOG_DEBUG,
                                "Configuring holder {0} with configuration {1}, change count {2}",
                                new Object[] { holder, singleton, singleton.getChangeCount() }, null );
                            if ( singleton != null && checkBundleLocation( singleton, bundleContext.getBundle() ) )
                            {
                                long changeCount = singleton.getChangeCount();
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
            finally
            {
                try
                {
                    bundleContext.ungetService( caReference );
                }
                catch ( IllegalStateException e )
                {
                    // ignore, bundle context was shut down during the above.
                }
            }
        }
        return false;
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
        final TargetedPID pid = new TargetedPID( event.getPid() );
        String rawFactoryPid = event.getFactoryPid();
        final TargetedPID factoryPid = rawFactoryPid == null? null: new TargetedPID( rawFactoryPid );

        // iterate over all components which must be configured with this pid
        // (since DS 1.2, components may specify a specific configuration PID (112.4.4 configuration-pid)
        Collection<ComponentHolder<?>> holders = getComponentHolders( factoryPid != null? factoryPid: pid );

        logger.log( LogService.LOG_DEBUG,
            "configurationEvent: Handling {0} of Configuration PID={1} for component holders {2}",
            new Object[] { getEventType( event ), pid, holders }, null );

        for ( ComponentHolder<?> componentHolder : holders )
        {
            if ( !componentHolder.getComponentMetadata().isConfigurationIgnored() )
            {
                switch (event.getType())
                {
                    case ConfigurationEvent.CM_DELETED:
                        if ( factoryPid != null || !configureComponentHolder( componentHolder ) )
                        {
                            componentHolder.configurationDeleted( pid, factoryPid );
                        }
                        break;

                    case ConfigurationEvent.CM_UPDATED:
                    {
                        final ComponentActivator activator = componentHolder.getActivator();
                        if ( activator == null )
                        {
                            break;
                        }

                        final BundleContext bundleContext = activator.getBundleContext();
                        if ( bundleContext == null )
                        {
                            break;
                        }

                        TargetedPID targetedPid = factoryPid == null? pid: factoryPid;
                        TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID( pid, factoryPid );
                        if ( factoryPid != null || targetedPid.equals( oldTargetedPID )
                            || targetedPid.bindsStronger( oldTargetedPID ) )
                        {
                            final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid,
                                componentHolder, bundleContext );
                            if ( configInfo != null )
                            {
                                if ( checkBundleLocation( configInfo.getBundleLocation(), bundleContext.getBundle() ) )
                                {
                                    // The below seems to be unnecessary - and if put in, the behaviour is not spec compliant anymore:
                                    // if a component has a required configuration and a modified method, the component must not be
                                    // reactivated
                                    // If this is replacing a weaker targetedPID delete the old one.
                                    // if ( factoryPid == null && !targetedPid.equals(oldTargetedPID) && oldTargetedPID != null)
                                    //{
                                    //componentHolder.configurationDeleted( pid, factoryPid );
                                    //}
                                    componentHolder.configurationUpdated( pid, factoryPid, configInfo.getProps(),
                                        configInfo.getChangeCount() );
                                }
                            }
                        }

                        break;
                    }
                    case ConfigurationEvent.CM_LOCATION_CHANGED:
                    {
                        //TODO is this logic correct for factory pids????
                        final ComponentActivator activator = componentHolder.getActivator();
                        if ( activator == null )
                        {
                            break;
                        }

                        final BundleContext bundleContext = activator.getBundleContext();
                        if ( bundleContext == null )
                        {
                            break;
                        }

                        TargetedPID targetedPid = factoryPid == null? pid: factoryPid;
                        TargetedPID oldTargetedPID = componentHolder.getConfigurationTargetedPID( pid, factoryPid );
                        if ( targetedPid.equals( oldTargetedPID ) )
                        {
                            //this sets the location to this component's bundle if not already set.  OK here
                            //since it used to be set to this bundle, ok to reset it
                            final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid,
                                componentHolder, bundleContext );
                            if ( configInfo != null )
                            {
                                logger.log( LogService.LOG_DEBUG,
                                    "LocationChanged event, same targetedPID {0}, location now {1}, change count {2}",
                                    new Object[] { targetedPid, configInfo.getBundleLocation(),
                                            configInfo.getChangeCount() },
                                    null );
                                if ( configInfo.getProps() == null )
                                {
                                    throw new IllegalStateException( "Existing Configuration with pid " + pid
                                        + " has had its properties set to null and location changed.  We expected a delete event first." );
                                }
                                //this config was used on this component.  Does it still match?
                                if ( !checkBundleLocation( configInfo.getBundleLocation(), bundleContext.getBundle() ) )
                                {
                                    //no, delete it
                                    componentHolder.configurationDeleted( pid, factoryPid );
                                    //maybe there's another match
                                    configureComponentHolder( componentHolder );

                                }
                                //else still matches
                            }
                            break;
                        }
                        boolean better = targetedPid.bindsStronger( oldTargetedPID );
                        if ( better )
                        {
                            //this sets the location to this component's bundle if not already set.  OK here
                            //because if it is set to this bundle we will use it.
                            final ConfigurationInfo configInfo = getConfigurationInfo( pid, targetedPid,
                                componentHolder, bundleContext );
                            if ( configInfo != null )
                            {
                                logger.log( LogService.LOG_DEBUG,
                                    "LocationChanged event, better targetedPID {0} compared to {1}, location now {2}, change count {3}",
                                    new Object[] { targetedPid, oldTargetedPID, configInfo.getBundleLocation(),
                                            configInfo.getChangeCount() },
                                    null );
                                if ( configInfo.getProps() == null )
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
                                    componentHolder.configurationUpdated( pid, factoryPid, configInfo.getProps(),
                                        configInfo.getChangeCount() );
                                }
                            }
                        }
                        //else worse match, do nothing
                        else
                        {
                            logger.log( LogService.LOG_DEBUG,
                                "LocationChanged event, worse targetedPID {0} compared to {1}, do nothing",
                                new Object[] { targetedPid, oldTargetedPID }, null );
                        }
                        break;
                    }
                    default:
                        logger.log( LogService.LOG_WARNING, "Unknown ConfigurationEvent type {0}",
                            new Object[] { event.getType() }, null );
                }
            }
        }
    }

    protected abstract Collection<ComponentHolder<?>> getComponentHolders(TargetedPID pid);

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
        try
        {
            final ConfigurationAdmin ca = getConfigAdmin( bundleContext );
            try
            {
                Configuration[] configs = ca.listConfigurations( filter( pid.getRawPid() ) );
                if ( configs != null && configs.length > 0 )
                {
                    Configuration config = configs[0];
                    return new ConfigurationInfo( config.getProperties(), config.getBundleLocation(),
                        config.getChangeCount() );
                }
            }
            catch ( IOException e )
            {
                logger.log( LogService.LOG_WARNING, "Failed reading configuration for pid={0}", new Object[] { pid },
                    e );
            }
            catch ( InvalidSyntaxException e )
            {
                logger.log( LogService.LOG_WARNING, "Failed reading configuration for pid={0}", new Object[] { pid },
                    e );
            }
            finally
            {
                bundleContext.ungetService( caReference );
            }
        }
        catch ( IllegalStateException ise )
        {
            // If the bundle has been stopped concurrently
            logger.log( LogService.LOG_WARNING, "Bundle in unexpected state", ise );
        }
        return null;
    }

    private String filter(String rawPid)
    {
        return "(service.pid=" + rawPid + ")";
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
        final Configuration[] cfg = findConfigurations( ca, filter );
        if ( cfg == null )
        {
            return null;
        }
        String longest = null;
        Configuration best = null;
        for ( Configuration config : cfg )
        {
            if ( checkBundleLocation( config, bundle ) )
            {
                String testPid = config.getPid();
                if ( longest == null || testPid.length() > longest.length() )
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
    private Collection<Configuration> findFactoryConfigurations(final ConfigurationAdmin ca, final String factoryPid,
        Bundle bundle)
    {
        final String filter = getTargetedPidFilter( factoryPid, bundle, ConfigurationAdmin.SERVICE_FACTORYPID );
        Configuration[] configs = findConfigurations( ca, filter );
        if ( configs == null )
        {
            return Collections.emptyList();
        }
        Map<String, Configuration> configsByPid = new HashMap<String, Configuration>();
        for ( Configuration config : configs )
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
        if ( config == null )
        {
            return false;
        }
        String configBundleLocation = config.getBundleLocation();
        return checkBundleLocation( configBundleLocation, bundle );
    }

    private boolean checkBundleLocation(String configBundleLocation, Bundle bundle)
    {
        boolean result;
        if ( configBundleLocation == null )
        {
            result = true;
        }
        else if ( configBundleLocation.startsWith( "?" ) )
        {
            //multilocation
            result = bundle.hasPermission(
                new ConfigurationPermission( configBundleLocation, ConfigurationPermission.TARGET ) );
        }
        else
        {
            result = configBundleLocation.equals( bundle.getLocation() );
        }
        logger.log( LogService.LOG_DEBUG, "checkBundleLocation: location {0}, returning {1}",
            new Object[] { configBundleLocation, result }, null );
        return result;
    }

    private Configuration[] findConfigurations(final ConfigurationAdmin ca, final String filter)
    {
        try
        {
            return ca.listConfigurations( filter );
        }
        catch ( IOException ioe )
        {
            logger.log( LogService.LOG_WARNING, "Problem listing configurations for filter={0}",
                new Object[] { filter }, ioe );
        }
        catch ( InvalidSyntaxException ise )
        {
            logger.log( LogService.LOG_ERROR, "Invalid Configuration selection filter {0}", new Object[] { filter },
                ise );
        }

        // no factories in case of problems
        return null;
    }

    private String getTargetedPidFilter(String pid, Bundle bundle, String key)
    {
        String bsn = bundle.getSymbolicName();
        String version = bundle.getVersion().toString();
        String location = escape( bundle.getLocation() );
        String f = String.format( "(|(%1$s=%2$s)(%1$s=%2$s|%3$s)(%1$s=%2$s|%3$s|%4$s)(%1$s=%2$s|%3$s|%4$s|%5$s))", key,
            pid, bsn, version, location );
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

    private ConfigurationAdmin getConfigAdmin(BundleContext bundleContext)
    {
        return bundleContext.getService( caReference );
    }

}
