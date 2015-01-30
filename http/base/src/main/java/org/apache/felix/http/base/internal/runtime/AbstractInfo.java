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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Base class for all info objects.
 * Provides support for ranking and ordering of services.
 */
public abstract class AbstractInfo<T> implements Comparable<AbstractInfo<T>>
{
    /** Service id for services not provided through the service registry. */
    private static final AtomicLong serviceIdCounter = new AtomicLong(-1);

    /** Service ranking. */
    private final int ranking;

    /** Service id. */
    private final long serviceId;

    /** The context selection. */
    private final String contextSelection;

    private final Filter filter;

    /** Service reference. */
    private final ServiceReference<T> serviceReference;

    public AbstractInfo(final ServiceReference<T> ref)
    {
        this.serviceId = (Long)ref.getProperty(Constants.SERVICE_ID);
        final Object rankingObj = ref.getProperty(Constants.SERVICE_RANKING);
        if ( rankingObj instanceof Integer )
        {
            this.ranking = (Integer)rankingObj;
        }
        else
        {
            this.ranking = 0;
        }
        this.serviceReference = ref;
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
            f = ref.getBundle().getBundleContext().createFilter(this.contextSelection);
        }
        catch ( final InvalidSyntaxException ise)
        {
            // ignore
            f = null;
        }
        this.filter = f;
    }

    public AbstractInfo(final int ranking)
    {
        this(ranking, serviceIdCounter.getAndDecrement());

    }

    public AbstractInfo(final int ranking, final long serviceId)
    {
        this.ranking = ranking;
        this.serviceId = serviceId;
        this.serviceReference = null;
        this.contextSelection = null;
        this.filter = null;
    }

    public boolean isValid() {
        return this.filter != null || this.serviceReference == null;
    }

    /**
     * Compare two info objects based on their ranking (aka ServiceReference ordering)
     */
    @Override
    public int compareTo(final AbstractInfo<T> other)
    {
        if (other.ranking == this.ranking)
        {
            if (other.serviceId == this.serviceId)
            {
                return 0;
            }
            return other.serviceId > this.serviceId ? -1 : 1;
        }

        return (other.ranking > this.ranking) ? 1 : -1;
    }

    protected boolean isEmpty(final String value)
    {
        return value == null || value.length() == 0;
    }

    protected boolean isEmpty(final String[] value)
    {
        return value == null || value.length == 0;
    }

    protected String getStringProperty(final ServiceReference<T> ref, final String key)
    {
        final Object value = ref.getProperty(key);
        return (value instanceof String) ? (String) value : null;
    }

    protected String[] getStringArrayProperty(ServiceReference<T> ref, String key)
    {
        Object value = ref.getProperty(key);

        if (value instanceof String)
        {
            return new String[] { (String) value };
        }
        else if (value instanceof String[])
        {
            return (String[]) value;
        }
        else if (value instanceof Collection<?>)
        {
            Collection<?> collectionValues = (Collection<?>) value;
            String[] values = new String[collectionValues.size()];

            int i = 0;
            for (Object current : collectionValues)
            {
                values[i++] = current != null ? String.valueOf(current) : null;
            }

            return values;
        }

        return null;
    }

    protected boolean getBooleanProperty(ServiceReference<T> ref, String key)
    {
        Object value = ref.getProperty(key);
        if (value instanceof String)
        {
            return Boolean.valueOf((String) value);
        }
        else if (value instanceof Boolean)
        {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    /**
     * Get the init parameters.
     */
    protected Map<String, String> getInitParams(final ServiceReference<T> ref, final String prefix)
    {
        Map<String, String> result = null;
        for (final String key : ref.getPropertyKeys())
        {
            if ( key.startsWith(prefix))
            {
                final String paramKey = key.substring(prefix.length());
                final String paramValue = getStringProperty(ref, key);

                if (paramValue != null)
                {
                    if ( result == null )
                    {
                        result = new HashMap<String, String>();
                    }
                    result.put(paramKey, paramValue);
                }
            }
        }
        return result;
    }

    public int getRanking()
    {
        return this.ranking;
    }

    public long getServiceId()
    {
        return this.serviceId;
    }

    public ServiceReference<T> getServiceReference()
    {
        return this.serviceReference;
    }

    public String getContextSelection()
    {
        return this.contextSelection;
    }

    public Filter getContextSelectionFilter()
    {
        return this.filter;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ranking;
        result = prime * result + (int) (serviceId ^ (serviceId >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        final AbstractInfo<T> other = (AbstractInfo<T>) obj;
        if (ranking != other.ranking)
            return false;
        if (serviceId != other.serviceId)
            return false;
        return true;
    }
}
