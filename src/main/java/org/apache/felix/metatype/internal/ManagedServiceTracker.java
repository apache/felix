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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.util.tracker.ServiceTracker;


public class ManagedServiceTracker extends ServiceTracker
{

    static final String MANAGED_SERVICE = "org.osgi.service.cm.ManagedService";

    static final String MANAGED_SERVICE_FACTORY = "org.osgi.service.cm.ManagedServiceFactory";

    private static final String FILTER = "(|(objectClass=" + MANAGED_SERVICE + ")(objectClass="
        + MANAGED_SERVICE_FACTORY + "))";

    private final MetaTypeServiceImpl mts;


    public ManagedServiceTracker( BundleContext bundleContext, MetaTypeServiceImpl mts ) throws InvalidSyntaxException
    {
        super( bundleContext, bundleContext.createFilter( FILTER ), null );

        this.mts = mts;
    }


    public Object addingService( ServiceReference reference )
    {
        Object service = this.context.getService( reference );
        if ( service instanceof MetaTypeProvider )
        {
            final ManagedServiceHolder holder = new ManagedServiceHolder( reference, ( MetaTypeProvider ) service );
            mts.addService( holder );
            return holder;
        }

        // not a MetaTypeProvider implementation, don't track
        this.context.ungetService( reference );
        return null;
    }


    public void modifiedService( ServiceReference reference, Object service )
    {
        ( ( ManagedServiceHolder ) service ).update( this.mts );
    }


    public void removedService( ServiceReference reference, Object service )
    {
        mts.removeService( ( ManagedServiceHolder ) service );
        this.context.ungetService( reference );
    }
}
