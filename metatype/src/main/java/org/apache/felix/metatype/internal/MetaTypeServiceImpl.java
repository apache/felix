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
import org.apache.felix.metatype.internal.MetaTypeProviderTracker.RegistrationPropertyHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
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

    /** The <code>BundleContext</code> of the providing this service. */
    private final BundleContext bundleContext;

    private final Map bundleMetaTypeInformation;

    private final MetaTypeProviderTracker providerTracker;

    /**
     * Creates an instance of this class.
     *
     * @param bundleContext The <code>BundleContext</code> ultimately used to
     *      access services if there are no meta type documents.
     */
    MetaTypeServiceImpl( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.bundleMetaTypeInformation = new ConcurrentHashMap();

        bundleContext.addBundleListener( this );

        this.providerTracker = new MetaTypeProviderTracker( bundleContext, MetaTypeProvider.class.getName(), this );
        this.providerTracker.open();
    }


    void dispose()
    {
        this.providerTracker.close();
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
                mti = new ServiceMetaTypeInformation( bundleContext, bundle );
            }

            MetaTypeInformationImpl impl = null;
            if ( bundle.getState() == Bundle.ACTIVE )
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

        MetaTypeInformationImpl cmti = new MetaTypeInformationImpl( bundleContext, bundle );
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


    protected void addSingletonMetaTypeProvider( final Bundle bundle, final String[] pids, MetaTypeProvider mtp )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( bundle );
        if ( mti != null )
        {
            mti.addSingletonMetaTypeProvider( pids, mtp );
        }
    }


    protected void addFactoryMetaTypeProvider( final Bundle bundle, final String[] factoryPids, MetaTypeProvider mtp )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( bundle );
        if ( mti != null )
        {
            mti.addFactoryMetaTypeProvider( factoryPids, mtp );
        }
    }


    protected boolean removeSingletonMetaTypeProvider( final Bundle bundle, final String[] pids )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( bundle );
        if ( mti != null )
        {
            return mti.removeSingletonMetaTypeProvider( pids );
        }
        return false;
    }


    protected boolean removeFactoryMetaTypeProvider( final Bundle bundle, final String[] factoryPids )
    {
        MetaTypeInformationImpl mti = getMetaTypeInformationInternal( bundle );
        if ( mti != null )
        {
            return mti.removeFactoryMetaTypeProvider( factoryPids );
        }
        return false;
    }


    private void putMetaTypeInformationInternal( final Bundle bundle, final MetaTypeInformationImpl mti )
    {
        final ServiceReference refs[] = this.providerTracker.getServiceReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                ServiceReference ref = refs[i];
                if ( bundle.equals( ref.getBundle() ) )
                {
                    final MetaTypeProviderTracker.RegistrationPropertyHolder holder = ( RegistrationPropertyHolder ) this.providerTracker
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
