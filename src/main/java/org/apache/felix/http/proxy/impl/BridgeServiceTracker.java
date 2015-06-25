/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.proxy.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 3.0.0
 */
public abstract class BridgeServiceTracker<T>
    extends ServiceTracker<T, T>
{
    private final static String DEFAULT_FILTER = "(http.felix.dispatcher=*)";

    private volatile T usedService;

    public BridgeServiceTracker(final BundleContext context, final Class<?> objectClass)
        throws InvalidSyntaxException
    {
        super(context, createFilter(context, objectClass), null);
    }


    protected abstract void setService(final T service);

    protected abstract void unsetService();


    @Override
    public T addingService(final ServiceReference<T> ref)
    {
        final T service = super.addingService(ref);
        if ( usedService == null )
        {
            this.usedService = service;
            this.setService(service);
        }

        return service;
    }

    @Override
    public void removedService(final ServiceReference<T> ref, final T service)
    {
        if ( service == usedService )
        {
            this.unsetService();
        }

        super.removedService(ref, service);
    }

    private static Filter createFilter(final BundleContext context, final Class<?> objectClass)
        throws InvalidSyntaxException
    {
        StringBuffer str = new StringBuffer();
        str.append("(&(").append(Constants.OBJECTCLASS).append("=");
        str.append(objectClass.getName()).append(")");
        str.append(DEFAULT_FILTER).append(")");
        return context.createFilter(str.toString());
    }
}
