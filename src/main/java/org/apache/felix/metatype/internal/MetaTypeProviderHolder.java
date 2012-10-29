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
package org.apache.felix.metatype.internal;


import java.util.Arrays;

import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;


class MetaTypeProviderHolder extends BaseProviderHolder
{
    private String[] pids;
    private String[] factoryPids;


    MetaTypeProviderHolder( final ServiceReference reference, final MetaTypeProvider provider )
    {
        super( reference, provider );

        this.pids = BaseProviderHolder.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
        this.factoryPids = BaseProviderHolder.getStringPlus( reference, MetaTypeProvider.METATYPE_FACTORY_PID );
    }


    String[] getPids()
    {
        return pids;
    }


    String[] getFactoryPids()
    {
        return factoryPids;
    }


    void update( final MetaTypeServiceImpl mti )
    {
        String[] pids = BaseProviderHolder.getStringPlus( this.getReference(), MetaTypeProvider.METATYPE_PID );
        String[] factoryPids = BaseProviderHolder.getStringPlus( this.getReference(),
            MetaTypeProvider.METATYPE_FACTORY_PID );

        if ( !Arrays.equals( pids, this.pids ) || !Arrays.equals( factoryPids, this.factoryPids ) )
        {
            mti.removeService( this );
            this.pids = pids;
            this.factoryPids = factoryPids;
            mti.addService( this );
        }
    }
}