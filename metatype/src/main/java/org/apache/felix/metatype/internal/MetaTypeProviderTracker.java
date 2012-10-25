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

        MetaTypeProvider mtp = ( MetaTypeProvider ) super.addingService( reference );

        final String[] pids = ServiceMetaTypeInformation.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
        if ( pids != null )
        {
            mti.addSingletonMetaTypeProvider( pids, mtp );
        }

        final String[] factoryPids = ServiceMetaTypeInformation.getStringPlus( reference,
            MetaTypeProvider.METATYPE_FACTORY_PID );
        if ( factoryPids != null )
        {
            mti.addFactoryMetaTypeProvider( factoryPids, mtp );
        }

        return mtp;
    }


    public void modifiedService( ServiceReference reference, Object service )
    {
        // TODO This is tricky: we must know the registration before the update !!
        super.modifiedService( reference, service );
    }


    public void removedService( ServiceReference reference, Object service )
    {
        final String[] pids = ServiceMetaTypeInformation.getStringPlus( reference, MetaTypeProvider.METATYPE_PID );
        if ( pids != null )
        {
            mti.removeSingletonMetaTypeProvider( pids );
        }

        final String[] factoryPids = ServiceMetaTypeInformation.getStringPlus( reference,
            MetaTypeProvider.METATYPE_FACTORY_PID );
        if ( factoryPids != null )
        {
            mti.removeFactoryMetaTypeProvider( factoryPids );
        }

        super.removedService( reference, service );
    }
}
