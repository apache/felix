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
package org.apache.felix.cm.impl;


import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.log.LogService;


/**
 * The <code>ConfigurationAdminImpl</code> is the per-bundle frontend to the
 * configuration manager. Instances of this class are created on-demand for
 * each bundle trying to get hold of the <code>ConfigurationAdmin</code>
 * service.
 */
public class ConfigurationAdminImpl implements ConfigurationAdmin
{

    // The configuration manager to which most of the tasks are delegated
    private volatile ConfigurationManager configurationManager;

    // The bundle for which this instance has been created
    private volatile Bundle bundle;


    ConfigurationAdminImpl( ConfigurationManager configurationManager, Bundle bundle )
    {
        this.configurationManager = configurationManager;
        this.bundle = bundle;
    }


    void dispose()
    {
        bundle = null;
        configurationManager = null;
    }


    Bundle getBundle()
    {
        return bundle;
    }


    //---------- ConfigurationAdmin interface ---------------------------------

    /* (non-Javadoc)
     * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String)
     */
    public Configuration createFactoryConfiguration( String factoryPid ) throws IOException
    {
        final ConfigurationManager configurationManager = getConfigurationManager();

        configurationManager.log( LogService.LOG_DEBUG, "createFactoryConfiguration(factoryPid={0})", new Object[]
            { factoryPid } );

        // FELIX-3360: new factory configuration with implicit binding is dynamic
        ConfigurationImpl config = configurationManager.createFactoryConfiguration( factoryPid, null );
        config.setDynamicBundleLocation( this.getBundle().getLocation(), false );
        return this.wrap( config );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String, java.lang.String)
     */
    public Configuration createFactoryConfiguration( String factoryPid, String location ) throws IOException
    {
        final ConfigurationManager configurationManager = getConfigurationManager();

        configurationManager.log( LogService.LOG_DEBUG, "createFactoryConfiguration(factoryPid={0}, location={1})",
            new Object[]
                { factoryPid, location } );

        // CM 1.4 / 104.13.2.3
        this.checkPermission( configurationManager, ( location == null ) ? "*" : location, false );

        ConfigurationImpl config = configurationManager.createFactoryConfiguration( factoryPid, location );
        return this.wrap( config );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String)
     */
    public Configuration getConfiguration( String pid ) throws IOException
    {
        final ConfigurationManager configurationManager = getConfigurationManager();

        configurationManager.log( LogService.LOG_DEBUG, "getConfiguration(pid={0})", new Object[]
            { pid } );

        ConfigurationImpl config = configurationManager.getConfiguration( pid );
        if ( config == null )
        {
            config = configurationManager.createConfiguration( pid, null );

            // FELIX-3360: configuration creation with implicit binding is dynamic
            config.setDynamicBundleLocation( getBundle().getLocation(), false );
        }
        else
        {
            if ( config.getBundleLocation() == null )
            {
                configurationManager.log( LogService.LOG_DEBUG, "Binding configuration {0} (isNew: {1}) to bundle {2}",
                    new Object[]
                        { config.getPid(), config.isNew() ? Boolean.TRUE : Boolean.FALSE,
                            this.getBundle().getLocation() } );

                // FELIX-3360: first implicit binding is dynamic
                config.setDynamicBundleLocation( getBundle().getLocation(), true );
            }
            else
            {
                // CM 1.4 / 104.13.2.3
                this.checkPermission( configurationManager, config.getBundleLocation(), false );
            }
        }

        return this.wrap( config );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String, java.lang.String)
     */
    public Configuration getConfiguration( String pid, String location ) throws IOException
    {
        final ConfigurationManager configurationManager = getConfigurationManager();

        configurationManager.log( LogService.LOG_DEBUG, "getConfiguration(pid={0}, location={1})", new Object[]
            { pid, location } );

        // CM 1.4 / 104.13.2.3
        this.checkPermission( configurationManager, ( location == null ) ? "*" : location, false );

        ConfigurationImpl config = configurationManager.getConfiguration( pid );
        if ( config == null )
        {
            config = configurationManager.createConfiguration( pid, location );
        }
        else
        {
            final String configLocation = config.getBundleLocation();
            this.checkPermission( configurationManager, ( configLocation == null ) ? "*" : configLocation, false );
        }

        return this.wrap( config );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.cm.ConfigurationAdmin#listConfigurations(java.lang.String)
     */
    public Configuration[] listConfigurations( String filter ) throws IOException, InvalidSyntaxException
    {
        final ConfigurationManager configurationManager = getConfigurationManager();

        configurationManager.log( LogService.LOG_DEBUG, "listConfigurations(filter={0})", new Object[]
            { filter } );

        ConfigurationImpl ci[] = configurationManager.listConfigurations( this, filter );
        if ( ci == null || ci.length == 0 )
        {
            return null;
        }

        Configuration[] cfgs = new Configuration[ci.length];
        for ( int i = 0; i < cfgs.length; i++ )
        {
            cfgs[i] = this.wrap( ci[i] );
        }

        return cfgs;
    }


    //---------- Security checks ----------------------------------------------

    private Configuration wrap( ConfigurationImpl configuration )
    {
        return new ConfigurationAdapter( this, configuration );
    }


    /**
     * Returns <code>true</code> if the current access control context (call
     * stack) has the CONFIGURE permission.
     */
    boolean hasPermission( final ConfigurationManager configurationManager, String name )
    {
        try
        {
            checkPermission(configurationManager, name, false);
            return true;
        }
        catch ( SecurityException se )
        {
            return false;
        }
    }


    /**
     * Checks whether the current access control context (call stack) has
     * the given permission for the given bundle location and throws a
     * <code>SecurityException</code> if this is not the case.
     *
     * @param name The bundle location to check for permission. If this
     *      is <code>null</code> permission is always granted.
     * @param checkOwn If {@code false} permission is alwas granted if
     *      {@code name} is the same the using bundle's location.
     *
     * @throws SecurityException if the access control context does not
     *      have the appropriate permission
     */
    void checkPermission( final ConfigurationManager configurationManager, String name, boolean checkOwn )
    {
        // the caller's permission must be checked
        final SecurityManager sm = System.getSecurityManager();
        if ( sm != null )
        {
            // CM 1.4 / 104.11.1 Implicit permission
            if ( name != null && ( checkOwn || !name.equals( getBundle().getLocation() ) ) )
            {
                try
                {
                    sm.checkPermission( new ConfigurationPermission( name, ConfigurationPermission.CONFIGURE ) );

                    configurationManager.log( LogService.LOG_DEBUG,
                        "Explicit Permission; grant CONFIGURE permission on configuration bound to {0} to bundle {1}",
                        new Object[]
                            { name, getBundle().getLocation() } );
                }
                catch ( SecurityException se )
                {
                    configurationManager
                        .log(
                            LogService.LOG_DEBUG,
                            "No Permission; denied CONFIGURE permission on configuration bound to {0} to bundle {1}; reason: {2}",
                            new Object[]
                                { name, getBundle().getLocation(), se.getMessage() } );
                    throw se;
                }
            }
            else if ( configurationManager.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                configurationManager.log( LogService.LOG_DEBUG,
                    "Implicit Permission; grant CONFIGURE permission on configuration bound to {0} to bundle {1}",
                    new Object[]
                        { name, getBundle().getLocation() } );

            }
        }
        else if ( configurationManager.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            configurationManager.log( LogService.LOG_DEBUG,
                "No SecurityManager installed; grant CONFIGURE permission on configuration bound to {0} to bundle {1}",
                new Object[]
                    { name, getBundle().getLocation() } );
        }
    }


    /**
     * Returns the {@link ConfigurationManager} backing this configuraiton
     * admin instance or throws {@code IllegalStateException} if already
     * disposed off.
     *
     * @return The {@link ConfigurationManager} instance if still active
     * @throws IllegalStateException if this instance has been
     *      {@linkplain #dispose() disposed off} already.
     */
    private ConfigurationManager getConfigurationManager()
    {
        if ( this.configurationManager == null )
        {
            throw new IllegalStateException( "Configuration Admin service has been unregistered" );
        }

        return this.configurationManager;
    }
}
