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

import org.apache.felix.http.base.internal.util.InternalIdFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Base class for all info objects.
 * Provides support for ranking and ordering of services.
 */
public abstract class WhiteboardServiceInfo<T> extends AbstractInfo<T>
{
    /** The context selection. */
    private final String contextSelection;

    private final Filter filter;

    public WhiteboardServiceInfo(final ServiceReference<T> ref)
    {
        super(ref);
        String sel = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
        if ( isEmpty(sel) )
        {
            this.contextSelection = "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "="
                                   + HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")";
        }
        else
        {
            this.contextSelection = sel;
        }
        Filter f = null;
        try
        {
            final Bundle bundle = ref.getBundle();
            if ( bundle != null )
            {
                final BundleContext ctx = bundle.getBundleContext();
                if ( ctx != null )
                {
                    f = ctx.createFilter(this.contextSelection);
                }
            }
        }
        catch ( final InvalidSyntaxException ise)
        {
            // ignore
            f = null;
        }
        this.filter = f;
    }

    public WhiteboardServiceInfo(final int ranking)
    {
        this(ranking, InternalIdFactory.INSTANCE.next());

    }

    public WhiteboardServiceInfo(final int ranking, final long serviceId)
    {
        super(ranking, serviceId);
        this.contextSelection = null;
        this.filter = null;
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && (this.filter != null || this.getServiceReference() == null);
    }

    public String getContextSelection()
    {
        return this.contextSelection;
    }

    public Filter getContextSelectionFilter()
    {
        return this.filter;
    }
}
