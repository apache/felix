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


<<<<<<< HEAD
public class MetaTypeProviderTracker extends ServiceTracker
=======
public class MetaTypeProviderTracker extends ServiceTracker<MetaTypeProvider, MetaTypeProviderHolder>
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
{

    final MetaTypeServiceImpl mti;


    public MetaTypeProviderTracker( BundleContext context, final MetaTypeServiceImpl mti )
    {
        super( context, MetaTypeProvider.class.getName(), null );
        this.mti = mti;
    }


<<<<<<< HEAD
    public Object addingService( ServiceReference reference )
    {
        final MetaTypeProvider provider = ( MetaTypeProvider ) this.context.getService( reference );
=======
    @Override
    public MetaTypeProviderHolder addingService( ServiceReference<MetaTypeProvider> reference )
    {
        final MetaTypeProvider provider = this.context.getService( reference );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        final MetaTypeProviderHolder holder = new MetaTypeProviderHolder( reference, provider );
        mti.addService( holder );
        return holder;
    }


<<<<<<< HEAD
    public void modifiedService( ServiceReference reference, Object service )
    {
        ( ( MetaTypeProviderHolder ) service ).update( this.mti );
    }


    public void removedService( ServiceReference reference, Object service )
    {
        mti.removeService( ( MetaTypeProviderHolder ) service );
=======
    @Override
    public void modifiedService( ServiceReference<MetaTypeProvider> reference, MetaTypeProviderHolder service )
    {
        service.update( this.mti );
    }


    @Override
    public void removedService( ServiceReference<MetaTypeProvider> reference, MetaTypeProviderHolder service )
    {
        mti.removeService( service );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.context.ungetService( reference );
    }
}
