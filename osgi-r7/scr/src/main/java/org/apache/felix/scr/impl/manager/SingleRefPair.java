/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.felix.scr.impl.manager;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @version $Rev$ $Date$
 */
public class SingleRefPair<S, T> extends RefPair<S, T>
{
    private AtomicReference<T> serviceObjectRef = new AtomicReference<T>();

    public SingleRefPair( ServiceReference<T> ref )
    {
        super(ref);
    }

    @Override
    public T getServiceObject(ComponentContextImpl<S> key)
    {
        return serviceObjectRef.get();
    }

    @Override
    public boolean setServiceObject( ComponentContextImpl<S> key, T serviceObject )
    {
        boolean set = serviceObjectRef.compareAndSet( null, serviceObject );
        if ( serviceObject != null)
        {
            failed = false;
        }
        return set;
    }

    @Override
    public T unsetServiceObject(ComponentContextImpl<S> key)
    {
        return serviceObjectRef.getAndSet( null );
    }

    @Override
    public String toString()
    {
        return "[RefPair: ref: [" + getRef() + "] service: [" + serviceObjectRef.get() + "]]";
    }

    @Override
    public boolean getServiceObject(ComponentContextImpl<S> key, BundleContext context)
    {
        T service = context.getService( getRef() );
        if ( service == null )
        {
            setFailed();
            key.getLogger().log(
                 LogService.LOG_WARNING,
                 "Could not get service from ref {0}", new Object[] {getRef()}, null );
            return false;
        }
        if (!setServiceObject(key, service))
        {
            // Another thread got the service before, so unget our
            context.ungetService( getRef() );
        }
        return true;
    }
}
