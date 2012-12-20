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

import org.osgi.framework.ServiceReference;

/**
 * @version $Rev:$ $Date:$
 */
public class RefPair<T>
{
    private final ServiceReference<T> ref;
    private T serviceObject;

    private boolean failed;

    public RefPair( ServiceReference<T> ref )
    {
        this.ref = ref;
    }

    public ServiceReference<T> getRef()
    {
        return ref;
    }

    public T getServiceObject()
    {
        return serviceObject;
    }

    public void setServiceObject( T serviceObject )
    {
        this.serviceObject = serviceObject;
        if ( serviceObject != null)
        {
            failed = false;
        }
    }

    public void setFailed( )
    {
        this.failed = true;
    }

    public boolean isFailed()
    {
        return failed;
    }


    @Override
    public String toString()
    {
        return "[RefPair: ref: [" + ref + "] service: [" + serviceObject + "]]";
    }
}
