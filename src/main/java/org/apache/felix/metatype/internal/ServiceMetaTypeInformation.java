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
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;


/**
 * The <code>ServiceMetaTypeInformation</code> extends the
 * {@link MetaTypeInformationImpl} adding support to register and unregister
 * <code>ManagedService</code>s and <code>ManagedServiceFactory</code>s
 * also implementing the <code>MetaTypeProvider</code> interface.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceMetaTypeInformation extends MetaTypeInformationImpl
{

    /**
     * Creates an instance of this class handling services of the given
     * <code>bundle</code>.
     *
     * @param bundle The <code>Bundle</code> whose services are handled by
     *            this class.
     */
    public ServiceMetaTypeInformation( Bundle bundle )
    {
        super( bundle );
    }


    /**
     * Registers the service described by the <code>serviceRef</code> with
     * this instance if the service is a <code>MetaTypeProvider</code>
     * instance and either a <code>service.factoryPid</code> or
     * <code>service.pid</code> property is set in the service registration
     * properties.
     * <p>
     * If the service is registered, this bundle keeps a reference, which is
     * ungot when the service is unregistered or this bundle is stopped.
     *
     * @param serviceRef The <code>ServiceReference</code> describing the
     *            service to be checked and handled.
     */
    protected void addService( String[] pids, boolean isSingleton, boolean isFactory, MetaTypeProvider mtp )
    {
        if ( pids != null )
        {
            if ( isSingleton )
            {
                addSingletonMetaTypeProvider( pids, mtp );
            }

            if ( isFactory )
            {
                addFactoryMetaTypeProvider( pids, mtp );
            }
        }
    }


    /**
     * Unregisters the service described by the <code>serviceRef</code> from
     * this instance. Unregistration just checks for the
     * <code>service.factoryPid</code> and <code>service.pid</code> service
     * properties but does not care whether the service implements the
     * <code>MetaTypeProvider</code> interface. If the service is registered
     * it is simply unregistered.
     * <p>
     * If the service is actually unregistered the reference retrieved by the
     * registration method is ungotten.
     *
     * @param serviceRef The <code>ServiceReference</code> describing the
     *            service to be unregistered.
     */
    protected void removeService( String[] pids, boolean isSingleton, boolean isFactory )
    {
        if ( pids != null )
        {
            if ( isSingleton )
            {
                removeSingletonMetaTypeProvider( pids );
            }

            if ( isFactory )
            {
                removeFactoryMetaTypeProvider( pids );
            }
        }
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


    static boolean isService( final ServiceReference ref, final String type )
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
