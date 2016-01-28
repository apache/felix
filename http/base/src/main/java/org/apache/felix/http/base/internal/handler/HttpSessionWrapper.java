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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;

import org.apache.felix.http.base.internal.context.ExtServletContext;

/**
 * The session wrapper keeps track of the internal session, manages their attributes
 * separately and also handles session timeout.
 */
@SuppressWarnings("deprecation")
public class HttpSessionWrapper implements HttpSession
{
    /** All special attributes are prefixed with this prefix. */
    private static final String PREFIX = "org.apache.felix.http.session.context.";

    /** For each internal session, the attributes are prefixed with this followed by the context id */
    private static final String ATTR_PREFIX = PREFIX + "attr.";

    /** The created time for the internal session (appended with context id) */
    private static final String ATTR_CREATED = PREFIX + "created.";

    /** The last accessed time for the internal session (appended with context id), as Epoch time (milliseconds). */
    private static final String ATTR_LAST_ACCESSED = PREFIX + "lastaccessed.";

    /** The max inactive time (appended with context id), in seconds. */
    private static final String ATTR_MAX_INACTIVE = PREFIX + "maxinactive.";

    /** The underlying container session. */
    private final HttpSession delegate;

    /** The corresponding servlet context. */
    private final ExtServletContext context;

    /** The id for this session. */
    private final String sessionId;

    /** The key prefix for attributes belonging to this session. */
    private final String keyPrefix;

    /** Flag to handle the validity of this session. */
    private volatile boolean isInvalid = false;

    /** The time this has been created. */
    private final long created;

    /** The time this has been last accessed. */
    private final long lastAccessed;

    /** The max timeout interval. */
    private int maxTimeout;

    /**
     * Is this a new session?
     */
    private final boolean isNew;

    public static boolean hasSession(final long contextId, final HttpSession session)
    {
        final String sessionId = String.valueOf(contextId);
        return session.getAttribute(ATTR_CREATED + sessionId) != null;
    }

    public static Set<Long> getExpiredSessionContextIds(final HttpSession session)
    {
        final long now = System.currentTimeMillis();

        final Set<Long> ids = new HashSet<Long>();
        final Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements())
        {
            final String name = names.nextElement();
            if (name.startsWith(ATTR_LAST_ACCESSED))
            {
                final String id = name.substring(ATTR_LAST_ACCESSED.length());

                final long lastAccess = (Long) session.getAttribute(name);
                final long maxTimeout = 1000L * ((Integer) session.getAttribute(ATTR_MAX_INACTIVE + id));

                if ((maxTimeout > 0) && (lastAccess + maxTimeout) < now)
                {
                    ids.add(Long.valueOf(id));
                }
            }
        }
        return ids;
    }

    public static Set<Long> getSessionContextIds(final HttpSession session)
    {
        final Set<Long> ids = new HashSet<Long>();
        final Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements())
        {
            final String name = names.nextElement();
            if (name.startsWith(ATTR_LAST_ACCESSED))
            {
                final String id = name.substring(ATTR_LAST_ACCESSED.length());
                ids.add(Long.valueOf(id));
            }
        }
        return ids;
    }

    /**
     * Creates a new {@link HttpSessionWrapper} instance.
     */
    public HttpSessionWrapper(final long contextId,
            final HttpSession session,
            final ExtServletContext context,
            final boolean terminate)
    {
        this.delegate = session;
        this.context = context;
        this.sessionId = String.valueOf(contextId);
        this.keyPrefix = ATTR_PREFIX + this.sessionId + ".";

        if ( this.keyPrefix != null )
        {
            final long now = System.currentTimeMillis();
            if ( session.getAttribute(ATTR_CREATED + this.sessionId) == null )
            {
                this.created = now;
                this.maxTimeout = session.getMaxInactiveInterval();
                this.isNew = true;

                session.setAttribute(ATTR_CREATED + this.sessionId, this.created);
                session.setAttribute(ATTR_MAX_INACTIVE + this.sessionId, this.maxTimeout);

                if ( context.getHttpSessionListener() != null )
                {
                    context.getHttpSessionListener().sessionCreated(new HttpSessionEvent(this));
                }
            }
            else
            {
                this.created = (Long)session.getAttribute(ATTR_CREATED + this.sessionId);
                this.maxTimeout = (Integer)session.getAttribute(ATTR_MAX_INACTIVE + this.sessionId);
                this.isNew = false;
            }

            this.lastAccessed = now;
            if ( !terminate )
            {
                session.setAttribute(ATTR_LAST_ACCESSED + this.sessionId, this.lastAccessed);
            }
        }
        else
        {
            this.isNew = session.isNew();
            this.lastAccessed = session.getLastAccessedTime();
            this.created = session.getCreationTime();
        }
    }

    /**
     * Helper method to get the real key within the real session.
     */
    private String getKey(final String name)
    {
        return this.keyPrefix == null ? name : this.keyPrefix.concat(name);
    }

    /**
     * Check whether this session is still valid.
     * @throws IllegalStateException if session is not valid anymore
     */
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
        Object result = this.delegate.getAttribute(this.getKey(name));
        if ( result instanceof SessionBindingValueListenerWrapper )
        {
            result = ((SessionBindingValueListenerWrapper)result).getHttpSessionBindingListener();
        }
        return result;
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
                    if ( keyPrefix == null && !name.startsWith(PREFIX) )
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
        return this.delegate.getId() + "-" + this.sessionId;
    }

    @Override
    public long getLastAccessedTime()
    {
        this.checkInvalid();
        return this.lastAccessed;
    }

    @Override
    public int getMaxInactiveInterval()
    {
        // no validity check conforming to the javadocs
        return this.maxTimeout;
    }

    @Override
    public ServletContext getServletContext()
    {
        // no validity check conforming to the javadocs
        return this.context;
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

        // session listener must be called before the session is invalidated
        if (context.getHttpSessionListener() != null) {
            context.getHttpSessionListener().sessionDestroyed(new HttpSessionEvent(this));
        }

        if ( this.keyPrefix != null )
        {
            this.delegate.removeAttribute(ATTR_CREATED + this.sessionId);
            this.delegate.removeAttribute(ATTR_LAST_ACCESSED + this.sessionId);
            this.delegate.removeAttribute(ATTR_MAX_INACTIVE + this.sessionId);

            final Enumeration<String> names = this.delegate.getAttributeNames();
            while ( names.hasMoreElements() )
            {
                final String name = names.nextElement();

                if ( name.startsWith(this.keyPrefix) ) {
                    this.removeAttribute(name.substring(this.keyPrefix.length()));
                }
            }
        }

        // if the session is empty we can invalidate
        final Enumeration<String> names = this.delegate.getAttributeNames();
        if ( !names.hasMoreElements() )
        {
            this.delegate.invalidate();
        }

        this.isInvalid = true;
    }

    @Override
    public boolean isNew()
    {
        this.checkInvalid();
        return this.isNew;
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
        final Object oldValue = this.getAttribute(name);
        if ( oldValue != null )
        {
            this.delegate.removeAttribute(this.getKey(name));
            if ( this.keyPrefix != null && oldValue instanceof HttpSessionBindingListener )
            {
                ((HttpSessionBindingListener)oldValue).valueUnbound(new HttpSessionBindingEvent(this, name));
            }
            if ( this.context.getHttpSessionAttributeListener() != null )
            {
                this.context.getHttpSessionAttributeListener().attributeRemoved(new HttpSessionBindingEvent(this, name, oldValue));
            }
        }
    }

    @Override
    public void removeValue(final String name)
    {
        this.removeAttribute(name);
    }

    @Override
    public void setAttribute(final String name, final Object value)
    {
        this.checkInvalid();
        if ( value == null )
        {
            this.removeAttribute(name);
            return;
        }

        final Object oldValue = this.getAttribute(name);
        // wrap http session binding listener to avoid container calling it!
        if ( this.keyPrefix != null && value instanceof HttpSessionBindingListener )
        {
            this.delegate.setAttribute(this.getKey(name),
                    new SessionBindingValueListenerWrapper((HttpSessionBindingListener)value));
        }
        else
        {
            this.delegate.setAttribute(this.getKey(name), value);
        }
        if ( this.keyPrefix != null && value instanceof HttpSessionBindingListener )
        {
            ((HttpSessionBindingListener)value).valueBound(new HttpSessionBindingEvent(this, name));
        }

        if ( this.context.getHttpSessionAttributeListener() != null )
        {
            if ( oldValue != null )
            {
                this.context.getHttpSessionAttributeListener().attributeReplaced(new HttpSessionBindingEvent(this, name, oldValue));
            }
            else
            {
                this.context.getHttpSessionAttributeListener().attributeAdded(new HttpSessionBindingEvent(this, name, value));
            }
        }
    }

    @Override
    public void setMaxInactiveInterval(final int interval)
    {
        if ( this.delegate.getMaxInactiveInterval() < interval )
        {
            this.delegate.setMaxInactiveInterval(interval);
        }
        if ( this.keyPrefix != null )
        {
            this.maxTimeout = interval;
            this.delegate.setAttribute(ATTR_MAX_INACTIVE + this.sessionId, interval);
        }
    }

    @Override
    public HttpSessionContext getSessionContext()
    {
        // no need to check validity conforming to the javadoc
        return this.delegate.getSessionContext();
    }

    private static final class SessionBindingValueListenerWrapper implements Serializable
    {

        private static final long serialVersionUID = 4009563108883768425L;

        private final HttpSessionBindingListener listener;

        public SessionBindingValueListenerWrapper(final HttpSessionBindingListener listener)
        {
            this.listener = listener;
        }

        public HttpSessionBindingListener getHttpSessionBindingListener()
        {
            return listener;
        }
    }
}
