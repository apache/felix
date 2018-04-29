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

<<<<<<< HEAD
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;

/**
 * @version $Rev:$ $Date:$
 */
public class RefPair<T>
{
    private final ServiceReference<T> ref;
    private AtomicReference<T> serviceObjectRef = new AtomicReference<T>();

    private boolean failed;
=======
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * @version $Rev$ $Date$
 */
public abstract class RefPair<S, T>
{
    private final ServiceReference<T> ref;

    boolean failed;
    volatile boolean deleted;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    public RefPair( ServiceReference<T> ref )
    {
        this.ref = ref;
    }

    public ServiceReference<T> getRef()
    {
        return ref;
    }

<<<<<<< HEAD
    public T getServiceObject()
    {
        return serviceObjectRef.get();
    }

    public boolean setServiceObject( T serviceObject )
    {
        boolean set = serviceObjectRef.compareAndSet( null, serviceObject );
        if ( serviceObject != null)
        {
            failed = false;
        }
        return set;
    }
    
    public T unsetServiceObject()
    {
        return serviceObjectRef.getAndSet( null );
    }
=======
    public ServiceObjects<T> getServiceObjects()
    {
        return null;
    }

    public abstract boolean getServiceObject( ComponentContextImpl<S> key, BundleContext context );

    public abstract T getServiceObject(ComponentContextImpl<S> key);

    public abstract boolean setServiceObject( ComponentContextImpl<S> key, T serviceObject );

    public abstract T unsetServiceObject(ComponentContextImpl<S> key);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    public void setFailed( )
    {
        this.failed = true;
    }

    public boolean isFailed()
    {
        return failed;
    }

<<<<<<< HEAD

    @Override
    public String toString()
    {
        return "[RefPair: ref: [" + ref + "] service: [" + serviceObjectRef.get() + "]]";
=======
    public boolean isDeleted()
    {
        return deleted;
    }

    public void markDeleted()
    {
        this.deleted = true;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
