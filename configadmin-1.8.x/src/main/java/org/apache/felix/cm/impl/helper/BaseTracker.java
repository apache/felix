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
package org.apache.felix.cm.impl.helper;


import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.cm.impl.RankingComparator;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>BaseTracker</code> is the base class for tracking
 * <code>ManagedService</code> and <code>ManagedServiceFactory</code>
 * services. It maps their <code>ServiceRegistration</code> to the
 * {@link ConfigurationMap} mapping their service PIDs to provided
 * configuration.
 */
public abstract class BaseTracker<S> extends ServiceTracker<S, ConfigurationMap<?>>
{
    protected final ConfigurationManager cm;

    private final boolean managedServiceFactory;

    protected BaseTracker( final ConfigurationManager cm, final boolean managedServiceFactory )
    {
        super( cm.getBundleContext(), ( managedServiceFactory ? ManagedServiceFactory.class.getName()
            : ManagedService.class.getName() ), null );
        this.cm = cm;
        this.managedServiceFactory = managedServiceFactory;
        open();
    }


    public ConfigurationMap<?> addingService( ServiceReference<S> reference )
    {
        this.cm.log( LogService.LOG_DEBUG, "Registering service {0}", new String[]
            { ConfigurationManager.toString( reference ) } );

        final String[] pids = getServicePid( reference );
        final ConfigurationMap<?> configurations = createConfigurationMap( pids );
        configure( reference, pids, configurations );
        return configurations;
    }


    @Override
    public void modifiedService( ServiceReference<S> reference, ConfigurationMap<?> service )
    {
        this.cm.log( LogService.LOG_DEBUG, "Modified service {0}", new String[]
            { ConfigurationManager.toString( reference ) } );

        String[] pids = getServicePid( reference );
        if ( service.isDifferentPids( pids ) )
        {
            service.setConfiguredPids( pids );
            configure( reference, pids, service );
        }
    }


    @Override
    public void removedService( ServiceReference<S> reference, ConfigurationMap<?> service )
    {
        // just log
        this.cm.log( LogService.LOG_DEBUG, "Unregistering service {0}", new String[]
            { ConfigurationManager.toString( reference ) } );
    }


    private void configure( ServiceReference<S> reference, String[] pids, ConfigurationMap<?> configurations )
    {
        if ( pids != null )
        {
            this.cm.configure( pids, reference, managedServiceFactory, configurations );
        }
    }


    public final List<ServiceReference<S>> getServices( final TargetedPID pid )
    {
        ServiceReference<S>[] refs = this.getServiceReferences();
        if ( refs != null )
        {
            ArrayList<ServiceReference<S>> result = new ArrayList<ServiceReference<S>>( refs.length );
            for ( ServiceReference<S> ref : refs )
            {
                ConfigurationMap map = this.getService( ref );
                if ( map != null
                    && ( map.accepts( pid.getRawPid() ) || ( map.accepts( pid.getServicePid() ) && pid
                        .matchesTarget( ref ) ) ) )
                {
                    result.add( ref );
                }
            }

            if ( result.size() > 1 )
            {
                Collections.sort( result, RankingComparator.SRV_RANKING );
            }

            return result;
        }

        return Collections.emptyList();
    }


    protected abstract ConfigurationMap<?> createConfigurationMap( String[] pids );

    /**
     * Returns the String to be used as the PID of the service PID for the
     * {@link TargetedPID pid} retrieved from the configuration.
     * <p>
     * This method will return {@link TargetedPID#getServicePid()} most of
     * the time except if the service PID used for the consumer's service
     * registration contains one or more pipe symbols (|). In this case
     * {@link TargetedPID#getRawPid()} might be returned.
     *
     * @param service The reference ot the service for which the service
     *      PID is to be returned.
     * @param pid The {@link TargetedPID} for which to return the service
     *      PID.
     * @return The service PID or <code>null</code> if the service does not
     *      respond to the targeted PID at all.
     */
    public abstract String getServicePid( ServiceReference<S> service, TargetedPID pid );


    /**
     * Updates the given service with the provided configuration.
     * <p>
     * See the implementations of this method for more information.
     *
     * @param service The reference to the service to update
     * @param configPid The targeted configuration PID
     * @param factoryPid The targeted factory PID or <code>null</code> for
     *      a non-factory configuration
     * @param properties The configuration properties, which may be
     *      <code>null</code> if this is the provisioning call upon
     *      service registration of a ManagedService
     * @param revision The configuration revision or -1 if there is no
     *      configuration actually to provide.
     * @param configurationMap The PID to configuration map for PIDs
     *      used by the service to update
     *
     * @see ManagedServiceTracker#provideConfiguration(ServiceReference, TargetedPID, TargetedPID, Dictionary, long, ConfigurationMap)
     * @see ManagedServiceFactoryTracker#provideConfiguration(ServiceReference, TargetedPID, TargetedPID, Dictionary, long, ConfigurationMap)
     */
    public abstract void provideConfiguration( ServiceReference<S> service, TargetedPID configPid,
        TargetedPID factoryPid, Dictionary<String, ?> properties, long revision,
        ConfigurationMap<?> configurationMap);


    /**
     * Remove the configuration indicated by the {@code configPid} from
     * the service.
     *
     * @param service The reference to the service from which the
     *      configuration is to be removed.
     * @param configPid The {@link TargetedPID} of the configuration
     * @param factoryPid The {@link TargetedPID factory PID} of the
     *      configuration. This may be {@code null} for a non-factory
     *      configuration.
     */
    public abstract void removeConfiguration( ServiceReference<S> service, TargetedPID configPid, TargetedPID factoryPid);


    protected final S getRealService( ServiceReference<S> reference )
    {
        return this.context.getService( reference );
    }


    protected final void ungetRealService( ServiceReference<S> reference )
    {
        this.context.ungetService( reference );
    }


    protected final Dictionary getProperties( Dictionary<String, ?> rawProperties, ServiceReference service,
        String configPid, String factoryPid )
    {
        Dictionary props = new CaseInsensitiveDictionary( rawProperties );
        this.cm.callPlugins( props, service, configPid, factoryPid );
        return props;
    }


    protected final void handleCallBackError( final Throwable error, final ServiceReference target, final TargetedPID pid )
    {
        if ( error instanceof ConfigurationException )
        {
            final ConfigurationException ce = ( ConfigurationException ) error;
            if ( ce.getProperty() != null )
            {
                this.cm.log( LogService.LOG_ERROR,
                    "{0}: Updating property {1} of configuration {2} caused a problem: {3}", new Object[]
                        { ConfigurationManager.toString( target ), ce.getProperty(), pid, ce.getReason(), ce } );
            }
            else
            {
                this.cm.log( LogService.LOG_ERROR, "{0}: Updating configuration {1} caused a problem: {2}",
                    new Object[]
                        { ConfigurationManager.toString( target ), pid, ce.getReason(), ce } );
            }
        }
        else
        {
            {
                this.cm.log( LogService.LOG_ERROR, "{0}: Unexpected problem updating configuration {1}", new Object[]
                    { ConfigurationManager.toString( target ), pid, error } );
            }

        }
    }


    /**
     * Returns the <code>service.pid</code> property of the service reference as
     * an array of strings or <code>null</code> if the service reference does
     * not have a service PID property.
     * <p>
     * The service.pid property may be a single string, in which case a single
     * element array is returned. If the property is an array of string, this
     * array is returned. If the property is a collection it is assumed to be a
     * collection of strings and the collection is converted to an array to be
     * returned. Otherwise (also if the property is not set) <code>null</code>
     * is returned.
     *
     * @throws NullPointerException
     *             if reference is <code>null</code>
     * @throws ArrayStoreException
     *             if the service pid is a collection and not all elements are
     *             strings.
     */
    private static String[] getServicePid( ServiceReference reference )
    {
        Object pidObj = reference.getProperty( Constants.SERVICE_PID );
        if ( pidObj instanceof String )
        {
            return new String[]
                { ( String ) pidObj };
        }
        else if ( pidObj instanceof String[] )
        {
            return ( String[] ) pidObj;
        }
        else if ( pidObj instanceof Collection )
        {
            Collection pidCollection = ( Collection ) pidObj;
            return ( String[] ) pidCollection.toArray( new String[pidCollection.size()] );
        }

        return null;
    }


    protected AccessControlContext getAccessControlContext( final Object ref )
    {
        return new AccessControlContext( new ProtectionDomain[]
            { ref.getClass().getProtectionDomain() } );
    }
}