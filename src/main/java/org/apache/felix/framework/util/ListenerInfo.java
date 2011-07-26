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
package org.apache.felix.framework.util;

import java.util.EventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.hooks.service.ListenerHook;


public class ListenerInfo implements ListenerHook.ListenerInfo
{
    private final Bundle m_bundle;
    private final Class m_listenerClass;
    private final EventListener m_listener;
    private final Filter m_filter;
    private final Object m_acc;
    private final boolean m_removed;

    public ListenerInfo(
        Bundle bundle, Class listenerClass, EventListener listener,
        Filter filter, Object acc, boolean removed)
    {
        m_bundle = bundle;
        m_listenerClass = listenerClass;
        m_listener = listener;
        m_filter = filter;
        m_acc = acc;
        m_removed = removed;
    }

    public ListenerInfo(ListenerInfo info, boolean removed)
    {
        m_bundle = info.m_bundle;
        m_listenerClass = info.m_listenerClass;
        m_listener = info.m_listener;
        m_filter = info.m_filter;
        m_acc = info.m_acc;
        m_removed = removed;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public BundleContext getBundleContext()
    {
        return m_bundle.getBundleContext();
    }

    public Class getListenerClass()
    {
        return m_listenerClass;
    }

    public EventListener getListener()
    {
        return m_listener;
    }

    public Filter getParsedFilter()
    {
        return m_filter;
    }

    public String getFilter()
    {
        if (m_filter != null)
        {
            return m_filter.toString();
        }
        return null;
    }

    public Object getSecurityContext()
    {
        return m_acc;
    }

    public boolean isRemoved()
    {
        return m_removed;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof ListenerInfo))
        {
            return false;
        }

        ListenerInfo other = (ListenerInfo) obj;
        return other.m_listener == m_listener &&
            (m_filter == null ? other.m_filter == null : m_filter.equals(other.m_filter));
    }

    @Override
    public int hashCode()
    {
        int rc = 17;

        rc = 37 * rc + m_listener.hashCode();
        if (m_filter != null)
        {
            rc = 37 * rc + m_filter.hashCode();
        }
        return rc;
    }
}