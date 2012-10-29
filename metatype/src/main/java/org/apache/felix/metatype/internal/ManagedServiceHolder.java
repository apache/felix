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

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;


class ManagedServiceHolder extends BaseProviderHolder
{
    private String[] pids;
    private boolean isSingleton;
    private boolean isFactory;


    ManagedServiceHolder( final ServiceReference reference, final MetaTypeProvider provider )
    {
        super( reference, provider );
        this.pids = BaseProviderHolder.getServicePids( reference );
        this.isSingleton = ManagedServiceHolder.isService( reference, ManagedServiceTracker.MANAGED_SERVICE );
        this.isFactory = ManagedServiceHolder.isService( reference, ManagedServiceTracker.MANAGED_SERVICE_FACTORY );
    }


    String[] getPids()
    {
        return pids;
    }


    boolean isSingleton()
    {
        return isSingleton;
    }


    boolean isFactory()
    {
        return isFactory;
    }


    void update( final MetaTypeServiceImpl mts )
    {
        final String[] newPids = BaseProviderHolder.getServicePids( getReference() );
        if ( !Arrays.equals( this.getPids(), newPids ) )
        {
            mts.removeService( this );
            this.pids = newPids;
            mts.addService( this );
        }
    }


    private static boolean isService( final ServiceReference ref, final String type )
    {
        String[] oc = ( String[] ) ref.getProperty( Constants.OBJECTCLASS );
        if ( oc != null )
        {
            for ( int i = 0; i < oc.length; i++ )
            {
                if ( oc[i].equals( type ) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}