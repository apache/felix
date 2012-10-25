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

    final MetaTypeInformationImpl mti;


    public MetaTypeProviderTracker( BundleContext context, final MetaTypeInformationImpl mti )
    {
        super( context, MetaTypeProvider.class, null );
        this.mti = mti;
    }


    public Object addingService( ServiceReference reference )
    {
        // only care for services of our bundle
        if ( !this.mti.getBundle().equals( reference.getBundle() ) )
        {
            return null;
        }

        final MetaTypeProvider mtp = ( MetaTypeProvider ) this.context.getService( reference );
        final RegistrationPropertyHolder rph = new RegistrationPropertyHolder( reference, mtp );

        rph.addMetaTypeProvider( this.mti );

        return rph;
    }


    public void modifiedService( ServiceReference reference, Object service )
    {
        RegistrationPropertyHolder rph = ( RegistrationPropertyHolder ) service;
        rph.update( this.mti, reference );
    }


    public void removedService( ServiceReference reference, Object service )
    {
        RegistrationPropertyHolder rph = ( RegistrationPropertyHolder ) service;
        rph.removeMetaTypeProvider( this.mti );
        this.context.ungetService( reference );
    }

    private static class RegistrationPropertyHolder
    {
        private String[] pids;
        private String[] factoryPids;
        private final MetaTypeProvider provider;


        RegistrationPropertyHolder( final ServiceReference reference, final MetaTypeProvider provider )
        {
            this.pids = ServiceMetaTypeInformation.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
            this.factoryPids = ServiceMetaTypeInformation.getStringPlus( reference,
                MetaTypeProvider.METATYPE_FACTORY_PID );
            this.provider = provider;
        }


        MetaTypeProvider getProvider()
        {
            return provider;
        }


        void addMetaTypeProvider( final MetaTypeInformationImpl mti )
        {
            if ( pids != null )
            {
                mti.addSingletonMetaTypeProvider( pids, provider );
            }

            if ( factoryPids != null )
            {
                mti.addFactoryMetaTypeProvider( factoryPids, provider );
            }
        }


        void removeMetaTypeProvider( final MetaTypeInformationImpl mti )
        {
            if ( pids != null )
            {
                mti.removeSingletonMetaTypeProvider( pids );
            }

            if ( factoryPids != null )
            {
                mti.removeFactoryMetaTypeProvider( factoryPids );
            }
        }


        void update( final MetaTypeInformationImpl mti, final ServiceReference reference )
        {
            String[] pids = ServiceMetaTypeInformation.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
            String[] factoryPids = ServiceMetaTypeInformation.getStringPlus( reference,
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
