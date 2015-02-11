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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("deprecation")
public class HttpSessionWrapper implements HttpSession
{
    private static final String PREFIX = "org.apache.felix.http.session.context.";

    private static final String SESSION_MAP = "org.apache.felix.http.session.context.map";

    private final HttpSession delegate;
    private final ServletContext context;

    private final String keyPrefix;

    private volatile boolean isInvalid = false;

    private final long created;

    private final long contextId;

    /**
     * Creates a new {@link HttpSessionWrapper} instance.
     */
    public HttpSessionWrapper(final Long contextId, final HttpSession session,
            final ServletContext context)
    {
        this.delegate = session;
        this.context = context;
        this.contextId = (contextId == null ? 0 : contextId);
        this.keyPrefix = this.contextId == 0 ? null : PREFIX + String.valueOf(contextId) + ".";
        synchronized ( this.getClass() )
        {
            @SuppressWarnings("unchecked")
            Map<Long, Long> sessionMap = (Map<Long, Long>)session.getAttribute(SESSION_MAP);
            if ( sessionMap == null )
            {
                sessionMap = new HashMap<Long, Long>();
            }
            if ( !sessionMap.containsKey(contextId) )
            {
                sessionMap.put(contextId, System.currentTimeMillis());
                session.setAttribute(SESSION_MAP, sessionMap);
            }
            this.created = sessionMap.get(contextId);
        }
    }

    private String getKey(final String name)
    {
        return this.keyPrefix == null ? name : this.keyPrefix.concat(name);
    }

    private void checkInvalid()
    {
        if ( this.isInvalid )
        {
            throw new IllegalStateException("Session is invalid.");
        }
    }

    @Override
    public Object getAttribute(final String name)
    {
        this.checkInvalid();
        return this.delegate.getAttribute(this.getKey(name));
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        this.checkInvalid();
        final Enumeration<String> e = this.delegate.getAttributeNames();
        return new Enumeration<String>() {

            String next = peek();

            private String peek()
            {
                while ( e.hasMoreElements() )
                {
                    final String name = e.nextElement();
                    if ( keyPrefix == null )
                    {
                        return name;
                    }
                    if ( name.startsWith(keyPrefix))
                    {
                        return name.substring(keyPrefix.length());
                    }
                }
                return null;
            }

            @Override
            public boolean hasMoreElements() {
                return next != null;
            }

            @Override
            public String nextElement() {
                if ( next == null )
                {
                    throw new NoSuchElementException();
                }
                final String result = next;
                next = this.peek();
                return result;
            }
        };

    }

    @Override
    public long getCreationTime()
    {
        this.checkInvalid();
        return this.created;
    }

    @Override
    public String getId()
    {
        this.checkInvalid();
        return this.delegate.getId() + "-" + String.valueOf(this.contextId);
    }

    @Override
    public long getLastAccessedTime()
    {
        this.checkInvalid();
        // TODO
        return this.delegate.getLastAccessedTime();
    }

    @Override
    public int getMaxInactiveInterval()
    {
        // TODO
        return this.delegate.getMaxInactiveInterval();
    }

    @Override
    public ServletContext getServletContext()
    {
        return this.context;
    }

    @Override
    public HttpSessionContext getSessionContext()
    {
        return this.delegate.getSessionContext();
    }

    @Override
    public Object getValue(String name)
    {
        return this.getAttribute(name);
    }

    @Override
    public String[] getValueNames()
    {
        final List<String> names = new ArrayList<String>();
        final Enumeration<String> e = this.getAttributeNames();
        while ( e.hasMoreElements() )
        {
            names.add(e.nextElement());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public void invalidate()
    {
        this.checkInvalid();
        this.isInvalid = true;
        // TODO
        this.delegate.invalidate();
    }

    @Override
    public boolean isNew()
    {
        this.checkInvalid();
        return this.delegate.isNew();
    }

    @Override
    public void putValue(final String name, final Object value)
    {
        this.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(final String name)
    {
        this.checkInvalid();
        this.delegate.removeAttribute(name);
    }

    @Override
    public void removeValue(final String name)
    {
        this.removeAttribute(this.getKey(name));
    }

    @Override
    public void setAttribute(final String name, final Object value)
    {
        this.checkInvalid();
        this.delegate.setAttribute(this.getKey(name), value);
    }

    @Override
    public void setMaxInactiveInterval(int interval)
    {
        // TODO
        this.delegate.setMaxInactiveInterval(interval);
    }
}
