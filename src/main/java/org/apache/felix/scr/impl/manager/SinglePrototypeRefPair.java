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

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @version $Rev$ $Date$
 */
public class SinglePrototypeRefPair<S, T> extends SingleRefPair<S, T>
{
    private final ServiceObjects<T> serviceObjects;

    public SinglePrototypeRefPair( BundleContext context, ServiceReference<T> ref )
    {
        super(ref);
        this.serviceObjects = context.getServiceObjects(ref);
    }

    @Override
    public ServiceObjects<T> getServiceObjects()
    {
        return serviceObjects;
    }

    @Override
    public String toString()
    {
        return "[SinglePrototypeRefPair: ref: [" + getRef() + "] service: [" + getServiceObject(null) + "]]";
    }

    @Override
    public boolean getServiceObject(ComponentContextImpl<S> key, BundleContext context,
        SimpleLogger logger)
    {
    	final T service = key.getComponentServiceObjectsHelper().getPrototypeRefInstance(this.getRef(), serviceObjects);
        if ( service == null )
        {
            setFailed();
            logger.log(
                 LogService.LOG_WARNING,
                 "Could not get service from serviceobjects for ref {0}", new Object[] {getRef()}, null );
            return false;
        }
        if (!setServiceObject(key, service))
        {
            // Another thread got the service before, so unget our
            serviceObjects.ungetService( service );
        }
        return true;
    }

}
