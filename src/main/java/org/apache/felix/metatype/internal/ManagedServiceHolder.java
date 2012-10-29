/*************************************************************************
*
* ADOBE CONFIDENTIAL
* ___________________
*
*  Copyright 2012 Adobe Systems Incorporated
*  All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
**************************************************************************/
package org.apache.felix.metatype.internal;

import java.util.Arrays;

import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeProvider;

class ManagedServiceHolder
{
    private final ServiceReference ref;
    private final MetaTypeProvider provider;
    private String[] pids;
    private boolean isSingleton;
    private boolean isFactory;


    ManagedServiceHolder( final ServiceReference ref, final MetaTypeProvider provider )
    {
        this.ref = ref;
        this.provider = provider;
        this.pids = ServiceMetaTypeInformation.getServicePids( ref );
        this.isSingleton = ServiceMetaTypeInformation.isService( ref, ManagedServiceTracker.MANAGED_SERVICE );
        this.isFactory = ServiceMetaTypeInformation.isService( ref, ManagedServiceTracker.MANAGED_SERVICE_FACTORY );
    }


    public ServiceReference getRef()
    {
        return ref;
    }


    public MetaTypeProvider getProvider()
    {
        return provider;
    }


    public String[] getPids()
    {
        return pids;
    }


    public boolean isSingleton()
    {
        return isSingleton;
    }


    public boolean isFactory()
    {
        return isFactory;
    }


    void update( final MetaTypeServiceImpl mts )
    {
        final String[] newPids = ServiceMetaTypeInformation.getServicePids( getRef() );
        if ( !Arrays.equals( this.getPids(), newPids ) )
        {
            mts.removeService( this );
            this.pids = newPids;
            mts.addService( this );
        }
    }
}