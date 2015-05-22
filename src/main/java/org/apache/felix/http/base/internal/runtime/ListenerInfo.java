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
package org.apache.felix.http.base.internal.runtime;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Info object for registered listeners.
 */
public abstract class ListenerInfo<T> extends WhiteboardServiceInfo<T>
{

    private final String enabled;

    public ListenerInfo(final ServiceReference<T> ref)
    {
        super(ref);
        this.enabled = this.getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && "true".equalsIgnoreCase(this.enabled);
    }


    public T getService(final Bundle bundle)
    {
        if (this.getServiceReference() != null)
        {
            final BundleContext bctx = bundle.getBundleContext();
            if ( bctx != null )
            {
                final ServiceObjects<T> so = bctx.getServiceObjects(this.getServiceReference());
                if (so != null)
                {
                    return so.getService();
                }
            }
        }
        return null;
    }

    public void ungetService(final Bundle bundle, final T service)
    {
        if (this.getServiceReference() != null)
        {
            final BundleContext bctx = bundle.getBundleContext();
            if ( bctx != null )
            {
                final ServiceObjects<T> so = bctx.getServiceObjects(this.getServiceReference());
                if (so != null)
                {
                    so.ungetService(service);
                }
            }
        }
    }
}
