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
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.log.LogService;


/**
 * The <code>ConfigurationAdapter</code> is just an adapter to the internal
 * configuration object. Instances of this class are returned as Configuration
 * objects to the client, where each caller gets a fresh instance of this
 * class while internal Configuration objects are shared.
 */
public class ConfigurationAdapter implements Configuration
{

    private final ConfigurationAdminImpl configurationAdmin;
    private final ConfigurationImpl delegatee;


    ConfigurationAdapter( ConfigurationAdminImpl configurationAdmin, ConfigurationImpl delegatee )
    {
        this.configurationAdmin = configurationAdmin;
        this.delegatee = delegatee;
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getPid()
     */
    public String getPid()
    {
        checkDeleted();
        return delegatee.getPidString();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getFactoryPid()
     */
    public String getFactoryPid()
    {
        checkDeleted();
        return delegatee.getFactoryPidString();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getBundleLocation()
     */
    public String getBundleLocation()
    {
        // CM 1.4 / 104.13.2.4
        final String bundleLocation = delegatee.getBundleLocation();
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "getBundleLocation() ==> {0}", new Object[]
            { bundleLocation } );
        checkActive();
        configurationAdmin.checkPermission( delegatee.getConfigurationManager(), ( bundleLocation == null ) ? "*" : bundleLocation, true );
        checkDeleted();
        return bundleLocation;
    }


    /**
     * @param bundleLocation
     * @see org.apache.felix.cm.impl.ConfigurationImpl#setStaticBundleLocation(String)
     */
    public void setBundleLocation( String bundleLocation )
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "setBundleLocation(bundleLocation={0})",
            new Object[]
                { bundleLocation } );

        // CM 1.4 / 104.13.2.4
        checkActive();
        final String configLocation = delegatee.getBundleLocation();
        configurationAdmin.checkPermission( delegatee.getConfigurationManager(), ( configLocation == null ) ? "*" : configLocation, true );
        configurationAdmin.checkPermission( delegatee.getConfigurationManager(), ( bundleLocation == null ) ? "*" : bundleLocation, true );
        checkDeleted();
        delegatee.setStaticBundleLocation( bundleLocation );
    }


    /**
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#update()
     */
    public void update() throws IOException
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "update()", ( Throwable ) null );

        checkActive();
        checkDeleted();
        delegatee.update();
    }


    /**
     * @param properties
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#update(java.util.Dictionary)
     */
    public void update( Dictionary properties ) throws IOException
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "update(properties={0})", new Object[]
            { properties } );

        checkActive();
        checkDeleted();
        delegatee.update( properties );
    }


    public Dictionary getProperties()
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "getProperties()", ( Throwable ) null );

        checkDeleted();

        // return a deep copy since the spec says, that modification of
        // any value should not modify the internal, stored value
        return delegatee.getProperties( true );
    }


    public long getChangeCount()
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "getChangeCount()", ( Throwable ) null );

        checkDeleted();

        return delegatee.getRevision();
    }


    /**
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#delete()
     */
    public void delete() throws IOException
    {
        delegatee.getConfigurationManager().log( LogService.LOG_DEBUG, "delete()", ( Throwable ) null );

        checkActive();
        checkDeleted();
        delegatee.delete();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#hashCode()
     */
    public int hashCode()
    {
        return delegatee.hashCode();
    }


    /**
     * @param obj
     * @see org.apache.felix.cm.impl.ConfigurationImpl#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        return delegatee.equals( obj );
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#toString()
     */
    public String toString()
    {
        return delegatee.toString();
    }

    /**
     * Checks whether this configuration object is backed by an active
     * Configuration Admin Service (ConfigurationManager here).
     *
     * @throws IllegalStateException If this configuration object is not
     *      backed by an active ConfigurationManager
     */
    private void checkActive()
    {
        if ( !delegatee.isActive() )
        {
            throw new IllegalStateException( "Configuration " + delegatee.getPid()
                + " not backed by an active Configuration Admin Service" );
        }
    }


    /**
     * Checks whether this configuration object has already been deleted.
     *
     * @throws IllegalStateException If this configuration object has been
     *      deleted.
     */
    private void checkDeleted()
    {
        if ( delegatee.isDeleted() )
        {
            throw new IllegalStateException( "Configuration " + delegatee.getPid() + " deleted" );
        }
    }
}
