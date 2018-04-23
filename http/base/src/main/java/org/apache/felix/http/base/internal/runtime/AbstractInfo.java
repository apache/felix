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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Base class for all info objects.
 * Provides support for ranking and ordering of services.
 */
public abstract class AbstractInfo<T> implements Comparable<AbstractInfo<T>>
{
    /** Service ranking. */
    private final int ranking;

    /** Service id. */
    private final long serviceId;

    /** Service reference. */
    private final ServiceReference<T> serviceReference;

    /** Target. */
    private final String target;

    public AbstractInfo(final ServiceReference<T> ref)
    {
        this.serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
        final Object rankingObj = ref.getProperty(Constants.SERVICE_RANKING);
        if (rankingObj instanceof Integer)
        {
            this.ranking = (Integer) rankingObj;
        }
        else
        {
            this.ranking = 0;
        }
        this.serviceReference = ref;
        this.target = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET);
    }

    public AbstractInfo(final int ranking, final long serviceId)
    {
        this.ranking = ranking;
        this.serviceId = serviceId;
        this.serviceReference = null;
        this.target = null;
    }

    public boolean isValid()
    {
        return true;
    }

    /**
     * Compare two info objects based on their ranking (aka reverse ServiceReference ordering)
     */
    @Override
    public int compareTo(final AbstractInfo<T> other)
    {
        if (this.ranking == other.ranking)
        {
            if ( this.serviceId == other.serviceId )
            {
                return this.getClass().getName().compareTo(other.getClass().getName());
            }
            // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
            int reverseOrder = ( this.serviceId >= 0 && other.serviceId >= 0 ) ? 1 : -1;
            return reverseOrder * new Long(this.serviceId).compareTo(other.serviceId);
        }

        int result = new Integer(other.ranking).compareTo(this.ranking);
        return result;
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
        return (value instanceof String) ? ((String) value).trim() : null;
    }

    protected String[] getStringArrayProperty(ServiceReference<T> ref, String key)
    {
        Object value = ref.getProperty(key);

        if (value instanceof String)
        {
            return new String[] { ((String) value).trim() };
        }
        else if (value instanceof String[])
        {
            final String[] arr = (String[]) value;
            String[] values = new String[arr.length];

            for (int i = 0, j = 0; i < arr.length; i++)
            {
                if (arr[i] != null)
                {
                    values[j++] = arr[i].trim();
                }
            }
            return values;
        }
        else if (value instanceof Collection<?>)
        {
            Collection<?> collectionValues = (Collection<?>) value;
            String[] values = new String[collectionValues.size()];

            int i = 0;
            for (Object current : collectionValues)
            {
                values[i++] = current != null ? String.valueOf(current).trim() : null;
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
        final Map<String, String> result = new HashMap<String, String>();
        for (final String key : ref.getPropertyKeys())
        {
            if (key.startsWith(prefix))
            {
                final String paramKey = key.substring(prefix.length());
                final String paramValue = getStringProperty(ref, key);

                if (paramValue != null)
                {
                    result.put(paramKey, paramValue);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public int getRanking()
    {
        return this.ranking;
    }

    public long getServiceId()
    {
        return this.serviceId;
    }

    public String getTarget()
    {
        return this.target;
    }

    public ServiceReference<T> getServiceReference()
    {
        return this.serviceReference;
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) (serviceId ^ (serviceId >>> 32));
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        final AbstractInfo<?> other = (AbstractInfo<?>) obj;
        return serviceId == other.serviceId;
    }
}
