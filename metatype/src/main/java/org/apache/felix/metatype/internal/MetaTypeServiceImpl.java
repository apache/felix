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


import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;


/**
 * The <code>MetaTypeServiceImpl</code> class is the implementation of the
 * <code>MetaTypeService</code> interface of the OSGi Metatype Service
 * Specification 1.1.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class MetaTypeServiceImpl implements MetaTypeService, SynchronousBundleListener
{

    private final Map bundleMetaTypeInformation;

    private final ManagedServiceTracker managedServiceTracker;

    private final MetaTypeProviderTracker providerTracker;

    /**
     * Creates an instance of this class.
     *
     * @param bundleContext The <code>BundleContext</code> ultimately used to
     *      access services if there are no meta type documents.
     */
    MetaTypeServiceImpl( BundleContext bundleContext )
    {
        this.bundleMetaTypeInformation = new ConcurrentHashMap();

        bundleContext.addBundleListener( this );

        ManagedServiceTracker mst = null;
        try
        {
            mst = new ManagedServiceTracker( bundleContext, this );
            mst.open();
        }
        catch ( InvalidSyntaxException e )
        {
            // this is really not expected !
        }
        this.managedServiceTracker = mst;

        this.providerTracker = new MetaTypeProviderTracker( bundleContext, this );
        this.providerTracker.open();
    }


    void dispose()
    {
        this.providerTracker.close();
        this.managedServiceTracker.close();
        this.bundleMetaTypeInformation.clear();
    }


    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.STOPPING )
        {
            SoftReference mtir = ( SoftReference ) this.bundleMetaTypeInformation.remove( new Long( event.getBundle()
                .getBundleId() ) );
            if ( mtir != null )
            {
                MetaTypeInformationImpl mti = ( MetaTypeInformationImpl ) mtir.get();
                if ( mti != null )
                {
                    mti.dispose();
                }
            }
        }
    }


    /**
     * Looks for meta type documents in the given <code>bundle</code>. If no
     * such documents exist, a <code>MetaTypeInformation</code> object is
     * returned handling the services of the bundle.
     * <p>
     * According to the specification, the services of the bundle are ignored
     * if at least one meta type document exists.
     *
     * @param bundle The <code>Bundle</code> for which a
     *      <code>MetaTypeInformation</code> is to be returned.
     */
    public MetaTypeInformation getMetaTypeInformation( Bundle bundle )
    {
        // no information for fragments
        if ( bundle.getHeaders().get( Constants.FRAGMENT_HOST ) != null )
        {
            return null;
        }

        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( bundle );
        if ( mti == null )
        {
            mti = fromDocuments( bundle );
            if ( mti == null )
            {
                mti = new ServiceMetaTypeInformation( bundle );
            }

            MetaTypeInformationImpl impl = null;
            if ( bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.RESOLVED )
            {
                putMetaTypeInformationInternal( bundle, mti );
            }
            else
            {
                impl = mti;
                mti = null;
            }

            if ( impl != null )
            {
                impl.dispose();
            }
        }

        return mti;
    }


    private MetaTypeInformationImpl fromDocuments( Bundle bundle )
    {
        MetaDataReader reader = new MetaDataReader();

        // get the descriptors, return nothing if none
        Enumeration docs = bundle.findEntries( METATYPE_DOCUMENTS_LOCATION, "*.xml", false );
        if ( docs == null || !docs.hasMoreElements() )
        {
            return null;
        }

        MetaTypeInformationImpl cmti = new MetaTypeInformationImpl( bundle );
        while ( docs.hasMoreElements() )
        {
            URL doc = ( URL ) docs.nextElement();
            try
            {
                MetaData metaData = reader.parse( doc );
                if ( metaData != null )
                {
                    cmti.addMetaData( metaData );
                }
            }
            catch ( IOException ioe )
            {
                Activator.log( LogService.LOG_ERROR, "fromDocuments: Error accessing document " + doc, ioe );
            }
        }
        return cmti;
    }

    //-- register and unregister MetaTypeProvider services

    protected void addService( final MetaTypeProviderHolder holder )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( holder.getReference().getBundle() );
        if ( mti != null )
        {
            if ( holder.getPids() != null )
            {
                mti.addSingletonMetaTypeProvider( holder.getPids(), holder.getProvider() );
            }

            if ( holder.getFactoryPids() != null )
            {
                mti.addFactoryMetaTypeProvider( holder.getFactoryPids(), holder.getProvider() );
            }
        }
    }


    protected void removeService( final MetaTypeProviderHolder holder )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( holder.getReference().getBundle() );
        if ( mti != null )
        {
            if ( holder.getPids() != null )
            {
                mti.removeSingletonMetaTypeProvider( holder.getPids() );
            }

            if ( holder.getFactoryPids() != null )
            {
                mti.removeFactoryMetaTypeProvider( holder.getFactoryPids() );
            }
        }
    }


    //-- register and unregister ManagedService[Factory] services implementing MetaTypeProvider

    protected void addService( final ManagedServiceHolder holder )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( holder.getReference().getBundle() );
        if ( mti != null )
        {
            mti.addService( holder.getPids(), holder.isSingleton(), holder.isFactory(), holder.getProvider() );
        }
    }


    protected void removeService( final ManagedServiceHolder holder )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( holder.getReference().getBundle() );
        if ( mti != null )
        {
            mti.removeService( holder.getPids(), holder.isSingleton(), holder.isFactory() );
        }
    }


    private void putMetaTypeInformationInternal( final Bundle bundle, final MetaTypeInformationImpl mti )
    {
        // initial ManagedService[Factory] implements MetaTypeProvider
        final ServiceReference msRefs[] = this.managedServiceTracker.getServiceReferences();
        if ( msRefs != null )
        {
            for ( int i = 0; i < msRefs.length; i++ )
            {
                ServiceReference ref = msRefs[i];
                if ( bundle.equals( ref.getBundle() ) )
                {
                    final ManagedServiceHolder holder = (ManagedServiceHolder) this.managedServiceTracker.getService( ref );
                    mti.addService( holder.getPids(), holder.isSingleton(), holder.isFactory(), holder.getProvider() );
                }
            }
        }

        // initial MetaTypeProvider
        final ServiceReference refs[] = this.providerTracker.getServiceReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                ServiceReference ref = refs[i];
                if ( bundle.equals( ref.getBundle() ) )
                {
                    final MetaTypeProviderHolder holder = ( MetaTypeProviderHolder ) this.providerTracker
                        .getService( ref );
                    if ( holder.getPids() != null )
                    {
                        mti.addSingletonMetaTypeProvider( holder.getPids(), holder.getProvider() );
                    }
                    if ( holder.getFactoryPids() != null )
                    {
                        mti.addFactoryMetaTypeProvider( holder.getFactoryPids(), holder.getProvider() );
                    }
                }
            }
        }

        this.bundleMetaTypeInformation.put( new Long( bundle.getBundleId() ),
            new SoftReference( mti ) );
    }


    private MetaTypeInformationImpl getMetaTypeInformationInternal( final Bundle bundle )
    {
        SoftReference mtir = ( SoftReference ) this.bundleMetaTypeInformation.get( new Long( bundle.getBundleId() ) );
        return ( MetaTypeInformationImpl ) ( ( mtir == null ) ? null : mtir.get() );
    }
}
