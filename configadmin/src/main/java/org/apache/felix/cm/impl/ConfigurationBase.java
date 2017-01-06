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
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.helper.TargetedPID;
import org.osgi.service.log.LogService;


abstract class ConfigurationBase
{

    /**
     * The {@link ConfigurationManager configuration manager} instance which
     * caused this configuration object to be created.
     */
    private final ConfigurationManager configurationManager;

    // the persistence manager storing this factory mapping
    private final PersistenceManager persistenceManager;

    // the basic ID of this instance
    private final TargetedPID baseId;

    protected ConfigurationBase( final ConfigurationManager configurationManager,
        final PersistenceManager persistenceManager, final String baseId )
    {
        if ( configurationManager == null )
        {
            throw new IllegalArgumentException( "ConfigurationManager must not be null" );
        }

        if ( persistenceManager == null )
        {
            throw new IllegalArgumentException( "PersistenceManager must not be null" );
        }

        this.configurationManager = configurationManager;
        this.persistenceManager = persistenceManager;
        this.baseId = new TargetedPID( baseId );
    }


    ConfigurationManager getConfigurationManager()
    {
        return configurationManager;
    }


    PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }


    TargetedPID getBaseId()
    {
        return baseId;
    }


    /**
     * Returns <code>true</code> if the ConfigurationManager of this
     * configuration is still active.
     */
    boolean isActive()
    {
        return configurationManager.isActive();
    }


    abstract void store() throws IOException;


    void storeSilently()
    {
        try
        {
            this.store();
        }
        catch ( IOException ioe )
        {
            configurationManager.log( LogService.LOG_ERROR, "Persisting ID {0} failed", new Object[]
                { getBaseId(), ioe } );
        }
    }


    static protected void replaceProperty( Dictionary properties, String key, String value )
    {
        if ( value == null )
        {
            properties.remove( key );
        }
        else
        {
            properties.put( key, value );
        }
    }

}
