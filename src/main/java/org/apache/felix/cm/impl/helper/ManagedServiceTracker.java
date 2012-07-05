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
package org.apache.felix.cm.impl.helper;


import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;


public class ManagedServiceTracker extends BaseTracker<ManagedService>
{

    private static final Dictionary<String, ?> INITIAL_MARKER = new Hashtable<String, Object>( 0 );


    public ManagedServiceTracker( ConfigurationManager cm )
    {
        super( cm, false );
    }


    @Override
    protected ConfigurationMap<?> createConfigurationMap( String[] pids )
    {
        return new ManagedServiceConfigurationMap( pids );
    }


    /**
     * Provides the given configuration to the managed service.
     * <p>
     * Depending on targeted PIDs this configuration may not actually be
     * provided if the service already has more strictly binding
     * configuration from a targeted configuration bound.
     * <p>
     * If the revision is a negative value, the provided configuration
     * is assigned to the ManagedService in any case without further
     * checks. This allows a replacement configuration for a deleted
     * or invisible configuration to be assigned without first removing
     * the deleted or invisible configuration.
     */
    @Override
    public void provideConfiguration( ServiceReference<ManagedService> service, TargetedPID configPid,
        TargetedPID factoryPid, Dictionary<String, ?> properties, long revision )
    {
        Dictionary<String, ?> supplied = ( properties == null ) ? INITIAL_MARKER : properties;
        updateService( service, configPid, supplied, revision );
    }


    @Override
    public void removeConfiguration( ServiceReference<ManagedService> service, TargetedPID configPid,
        TargetedPID factoryPid )
    {
        updateService( service, configPid, null, -1 );
    }


    private void updateService( ServiceReference<ManagedService> service, final TargetedPID configPid,
        Dictionary<String, ?> properties, long revision )
    {
        final ManagedService srv = this.getRealService( service );
        final ConfigurationMap configs = this.getService( service );
        if ( srv != null && configs != null )
        {
            boolean doUpdate = false;
            if ( properties == null )
            {
                doUpdate = configs.removeConfiguration( configPid, null );
            }
            else if ( properties == INITIAL_MARKER )
            {
                // initial call to ManagedService may supply null properties
                properties = null;
                revision = -1;
                doUpdate = true;
            }
            else if ( revision < 0 || configs.shallTake( configPid, null, revision ) )
            {
                // run the plugins and cause the update
                properties = getProperties( properties, service, configPid.toString(), null );
                doUpdate = true;
                revision = Math.abs( revision );
            }
            else
            {
                // new configuration is not a better match, don't update
                doUpdate = false;
            }

            if ( doUpdate )
            {
                try
                {
                    srv.updated( properties );
                    configs.record( configPid, null, revision );
                }
                catch ( Throwable t )
                {
                    this.handleCallBackError( t, service, configPid );
                }
                finally
                {
                    this.ungetRealService( service );
                }
            }
        }
   }
}