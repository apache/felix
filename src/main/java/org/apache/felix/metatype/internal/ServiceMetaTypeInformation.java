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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeProvider;


/**
 * The <code>ServiceMetaTypeInformation</code> extends the
 * {@link MetaTypeInformationImpl} adding support to register and unregister
 * <code>ManagedService</code>s and <code>ManagedServiceFactory</code>s
 * also implementing the <code>MetaTypeProvider</code> interface.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceMetaTypeInformation extends MetaTypeInformationImpl implements ServiceListener
{

    private static final String MANAGED_SERVICE = "org.osgi.service.cm.ManagedService";

    private static final String MANAGED_SERVICE_FACTORY = "org.osgi.service.cm.ManagedServiceFactory";

    /**
     * The filter specification to find <code>ManagedService</code>s and
     * <code>ManagedServiceFactory</code>s as well as to register a service
     * listener for those services (value is
     * "(|(objectClass=org.osgi.service.cm.ManagedService)(objectClass=org.osgi.service.cm.ManagedServiceFactory))").
     * We use the hard coded class name here to not create a dependency on the
     * ConfigurationAdmin service, which may not be available.
     */
    private static final String FILTER = "(|(objectClass=" + MANAGED_SERVICE + ")(objectClass="
        + MANAGED_SERVICE_FACTORY + "))";

    /**
     * The <code>BundleContext</code> used to get and unget services which
     * have to be registered and unregistered with the base class.
     */
    private final BundleContext bundleContext;


    /**
     * Creates an instance of this class handling services of the given
     * <code>bundle</code>.
     *
     * @param bundleContext The <code>BundleContext</code> used to get and
     *            unget services.
     * @param bundle The <code>Bundle</code> whose services are handled by
     *            this class.
     */
    public ServiceMetaTypeInformation( BundleContext bundleContext, Bundle bundle )
    {
        super( bundleContext, bundle );

        this.bundleContext = bundleContext;

        // register for service events for the bundle
        try
        {
            bundleContext.addServiceListener( this, FILTER );
        }
        catch ( InvalidSyntaxException ise )
        {
            Activator.log( LogService.LOG_ERROR, "ServiceMetaTypeInformation: Cannot register for service events", ise );
        }

        // prepare the filter to select existing services
        Filter filter;
        try
        {
            filter = bundleContext.createFilter( FILTER );
        }
        catch ( InvalidSyntaxException ise )
        {
            Activator.log( LogService.LOG_ERROR, "ServiceMetaTypeInformation: Cannot create filter '" + FILTER + "'",
                ise );
            return;
        }

        // add current services of the bundle
        ServiceReference[] sr = bundle.getRegisteredServices();
        if ( sr != null )
        {
            for ( int i = 0; i < sr.length; i++ )
            {
                if ( filter.match( sr[i] ) )
                {
                    addService( sr[i] );
                }
            }
        }
    }


    void dispose()
    {
        this.bundleContext.removeServiceListener( this );
        super.dispose();
    }


    // ---------- ServiceListener ----------------------------------------------

    /**
     * Handles service registration and unregistration events ignoring all
     * services not belonging to the <code>Bundle</code> which is handled by
     * this instance.
     *
     * @param event The <code>ServiceEvent</code>
     */
    public void serviceChanged( ServiceEvent event )
    {
        // only care for services of our bundle
        if ( !getBundle().equals( event.getServiceReference().getBundle() ) )
        {
            return;
        }

        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            addService( event.getServiceReference() );
        }
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            removeService( event.getServiceReference() );
        }
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
    protected void addService( ServiceReference serviceRef )
    {
        Object srv = bundleContext.getService( serviceRef );

        boolean ungetService = true;

        if ( srv instanceof MetaTypeProvider )
        {
            MetaTypeProvider mtp = ( MetaTypeProvider ) srv;

            // 1. check for a service factory PID
            String factoryPid = ( String ) serviceRef.getProperty( SERVICE_FACTORYPID );
            if ( factoryPid != null )
            {
                addFactoryMetaTypeProvider( new String[]
                    { factoryPid }, mtp );
                ungetService = false;
            }
            else
            {
                // 2. check for a service PID
                String[] pids = getServicePids( serviceRef );
                if ( pids != null )
                {
                    if ( isService( serviceRef, MANAGED_SERVICE ) )
                    {
                        addSingletonMetaTypeProvider( pids, mtp );
                        ungetService = false;
                    }

                    if ( isService( serviceRef, MANAGED_SERVICE_FACTORY ) )
                    {
                        addFactoryMetaTypeProvider( pids, mtp );
                        ungetService = false;
                    }
                }
            }
        }

        if ( ungetService )
        {
            bundleContext.ungetService( serviceRef );
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
    protected void removeService( ServiceReference serviceRef )
    {
        boolean ungetService = false;

        // 1. check for a service factory PID
        String factoryPid = ( String ) serviceRef.getProperty( SERVICE_FACTORYPID );
        if ( factoryPid != null )
        {
            ungetService = removeFactoryMetaTypeProvider( new String[]
                { factoryPid } );
        }
        else
        {
            // 2. check for a service PID
            String[] pids = getServicePids( serviceRef );
            if ( pids != null )
            {
                if ( isService( serviceRef, MANAGED_SERVICE ) )
                {
                    ungetService |= removeSingletonMetaTypeProvider( pids );
                }

                if ( isService( serviceRef, MANAGED_SERVICE_FACTORY ) )
                {
                    ungetService |= removeFactoryMetaTypeProvider( pids );
                }
            }
        }

        // 3. drop the service reference
        if ( ungetService )
        {
            bundleContext.ungetService( serviceRef );
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
