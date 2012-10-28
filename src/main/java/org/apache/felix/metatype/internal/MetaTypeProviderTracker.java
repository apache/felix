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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.util.tracker.ServiceTracker;


public class MetaTypeProviderTracker extends ServiceTracker
{

    final MetaTypeServiceImpl mti;


    public MetaTypeProviderTracker( BundleContext context, final String serviceType, final MetaTypeServiceImpl mti )
    {
        super( context, serviceType, null );
        this.mti = mti;
    }


    public Object addingService( ServiceReference reference )
    {
        final Object service = this.context.getService( reference );
        final RegistrationPropertyHolder rph;
        if ( service instanceof MetaTypeProvider )
        {
            rph = new RegistrationPropertyHolder( reference, ( MetaTypeProvider ) service );
            rph.addMetaTypeProvider( this.mti );
        }
        else
        {
            rph = null;
        }

        return rph;
    }


    public void modifiedService( ServiceReference reference, Object service )
    {
        ( ( RegistrationPropertyHolder ) service ).update( this.mti );
    }


    public void removedService( ServiceReference reference, Object service )
    {
        ( ( RegistrationPropertyHolder ) service ).removeMetaTypeProvider( this.mti );
        this.context.ungetService( reference );
    }


    static class RegistrationPropertyHolder
    {
        private String[] pids;
        private String[] factoryPids;

        private final ServiceReference reference;
        private final MetaTypeProvider provider;


        RegistrationPropertyHolder( final ServiceReference reference, final MetaTypeProvider provider )
        {
            this.pids = ServiceMetaTypeInformation.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
            this.factoryPids = ServiceMetaTypeInformation.getStringPlus( reference,
                MetaTypeProvider.METATYPE_FACTORY_PID );

            this.reference = reference;
            this.provider = provider;
        }


        MetaTypeProvider getProvider()
        {
            return provider;
        }


        String[] getPids()
        {
            return pids;
        }


        String[] getFactoryPids()
        {
            return factoryPids;
        }


        void addMetaTypeProvider( final MetaTypeServiceImpl mti )
        {
            if ( pids != null )
            {
                mti.addSingletonMetaTypeProvider( reference.getBundle(), pids, provider );
            }

            if ( factoryPids != null )
            {
                mti.addFactoryMetaTypeProvider( reference.getBundle(), factoryPids, provider );
            }
        }


        void removeMetaTypeProvider( final MetaTypeServiceImpl mti )
        {
            if ( pids != null )
            {
                mti.removeSingletonMetaTypeProvider( reference.getBundle(), pids );
            }

            if ( factoryPids != null )
            {
                mti.removeFactoryMetaTypeProvider( reference.getBundle(), factoryPids );
            }
        }


        void update( final MetaTypeServiceImpl mti )
        {
            String[] pids = ServiceMetaTypeInformation.getStringPlus( this.reference, MetaTypeProvider.METATYPE_PID );
            String[] factoryPids = ServiceMetaTypeInformation.getStringPlus( this.reference,
                MetaTypeProvider.METATYPE_FACTORY_PID );

            if ( !Arrays.equals( pids, this.pids ) || !Arrays.equals( factoryPids, this.factoryPids ) )
            {
                removeMetaTypeProvider( mti );
                this.pids = pids;
                this.factoryPids = factoryPids;
                addMetaTypeProvider( mti );
            }
        }
    }
}
