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

package org.apache.felix.http.base.internal.handler;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("deprecation")
public class HttpSessionWrapper implements HttpSession
{
    private final HttpSession delegate;
    private final ServletContext context;

    /**
     * Creates a new {@link HttpSessionWrapper} instance.
     */
    public HttpSessionWrapper(HttpSession session, ServletContext context)
    {
        this.delegate = session;
        this.context = context;
    }

    public Object getAttribute(String name)
    {
        return this.delegate.getAttribute(name);
    }

    public Enumeration<String> getAttributeNames()
    {
        return this.delegate.getAttributeNames();
    }

    public long getCreationTime()
    {
        return this.delegate.getCreationTime();
    }

    public String getId()
    {
        return this.delegate.getId();
    }

    public long getLastAccessedTime()
    {
        return this.delegate.getLastAccessedTime();
    }

    public int getMaxInactiveInterval()
    {
        return this.delegate.getMaxInactiveInterval();
    }

    public ServletContext getServletContext()
    {
        return this.context;
    }

    public HttpSessionContext getSessionContext()
    {
        return this.delegate.getSessionContext();
    }

    public Object getValue(String name)
    {
        return this.delegate.getValue(name);
    }

    public String[] getValueNames()
    {
        return this.delegate.getValueNames();
    }

    public void invalidate()
    {
        this.delegate.invalidate();
    }

    public boolean isNew()
    {
        return this.delegate.isNew();
    }

    public void putValue(String name, Object value)
    {
        this.delegate.putValue(name, value);
    }

    public void removeAttribute(String name)
    {
        this.delegate.removeAttribute(name);
    }

    public void removeValue(String name)
    {
        this.delegate.removeValue(name);
    }

    public void setAttribute(String name, Object value)
    {
        this.delegate.setAttribute(name, value);
    }

    public void setMaxInactiveInterval(int interval)
    {
        this.delegate.setMaxInactiveInterval(interval);
    }
}
