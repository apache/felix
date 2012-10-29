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
import java.util.Collection;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;


public class BaseProviderHolder
{

    private final ServiceReference reference;
    private final MetaTypeProvider provider;


    BaseProviderHolder( final ServiceReference reference, final MetaTypeProvider provider )
    {
        this.reference = reference;
        this.provider = provider;
    }


    public ServiceReference getReference()
    {
        return reference;
    }


    public MetaTypeProvider getProvider()
    {
        return provider;
    }


    static String[] getServicePids( final ServiceReference ref )
    {
        return getStringPlus( ref, Constants.SERVICE_PID );
    }


    static String[] getStringPlus( final ServiceReference ref, final String propertyName )
    {
        final String[] res;
        Object prop = ref.getProperty( propertyName );
        if ( prop == null )
        {
            res = null;
        }
        else if ( prop instanceof String )
        {
            res = new String[]
                { ( String ) prop };
        }
        else if ( prop instanceof Collection )
        {
            final Object[] col = ( ( Collection ) prop ).toArray();
            res = new String[col.length];
            for ( int i = 0; i < res.length; i++ )
            {
                res[i] = String.valueOf( col[i] );
            }
        }
        else if ( prop.getClass().isArray() && String.class.equals( prop.getClass().getComponentType() ) )
        {
            res = ( String[] ) prop;
        }
        else
        {
            // unsupported type of property
            res = null;
        }

        if ( res != null )
        {
            Arrays.sort( res );
        }

        return res;
    }
}
