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

import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;

public class ManagedServiceFactoryTracker extends BaseTracker<ManagedServiceFactory>
{

    public ManagedServiceFactoryTracker( ConfigurationManager cm )
    {
        super( cm, true );
    }


    @Override
    protected ConfigurationMap<?> createConfigurationMap( String[] pids )
    {
        return new ManagedServiceFactoryConfigurationMap( pids );
    }


    @Override
    public void provideConfiguration( ServiceReference<ManagedServiceFactory> reference, TargetedPID configPid,
        TargetedPID factoryPid, Dictionary<String, ?> properties, long revision )
    {
        ManagedServiceFactory service = getRealService( reference );
        final ConfigurationMap configs = this.getService( reference );
        if ( service != null && configs != null )
        {
            if ( configs.shallTake( configPid, factoryPid, revision ) )
            {
                try
                {
                    Dictionary props = getProperties( properties, reference, configPid.toString(),
                        factoryPid.toString() );
                    service.updated( configPid.toString(), props );
                    configs.record( configPid, factoryPid, revision );
                }
                catch ( Throwable t )
                {
                    this.handleCallBackError( t, reference, configPid );
                }
                finally
                {
                    this.ungetRealService( reference );
                }
            }
        }
    }


    @Override
    public void removeConfiguration( ServiceReference<ManagedServiceFactory> reference, TargetedPID configPid,
        TargetedPID factoryPid )
    {
        final ManagedServiceFactory service = this.getRealService( reference );
        final ConfigurationMap configs = this.getService( reference );
        if ( service != null && configs != null)
        {
            if ( configs.removeConfiguration( configPid, factoryPid ) )
            {
                try
                {
                    service.deleted( configPid.toString() );
                    configs.record( configPid, factoryPid, -1 );
                }
                catch ( Throwable t )
                {
                    this.handleCallBackError( t, reference, configPid );
                }
                finally
                {
                    this.ungetRealService( reference );
                }
            }
        }
    }
}