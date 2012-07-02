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

import org.apache.felix.cm.impl.ConfigurationImpl;
import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;

public class ManagedServiceTracker extends BaseTracker<ManagedService>
{


    public ManagedServiceTracker( ConfigurationManager cm )
    {
        super( cm, ManagedService.class );
    }


    @Override
    protected boolean isFactory()
    {
        return false;
    }


    @Override
    public void provideConfiguration( ServiceReference<ManagedService> service, final ConfigurationImpl config, Dictionary<String, ?> properties )
    {
        ManagedService srv = this.getRealService( service );
        if ( srv != null )
        {
            try
            {
                Dictionary props = getProperties( properties, config.getPid(), service );
                srv.updated( props );
            }
            catch ( Throwable t )
            {
                this.cm.handleCallBackError( t, service, config );
            }
            finally
            {
                this.ungetRealService( service );
            }
        }
    }

    @Override
    public void removeConfiguration( ServiceReference<ManagedService> service, final ConfigurationImpl config )
    {
        ManagedService srv = this.getRealService( service );
        try
        {
            srv.updated( null );
        }
        catch ( Throwable t )
        {
            this.cm.handleCallBackError( t, service, config );
        }
        finally
        {
            this.ungetRealService( service );
        }
    }
}