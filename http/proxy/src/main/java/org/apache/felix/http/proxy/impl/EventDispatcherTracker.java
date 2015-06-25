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

import java.util.EventListener;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @since 3.0.0
 */
public final class EventDispatcherTracker
    extends BridgeServiceTracker<EventListener>
{
    private HttpSessionListener sessionListener;

    private HttpSessionIdListener sessionIdListener;

    private HttpSessionAttributeListener sessionAttributeListener;

    public EventDispatcherTracker(final BundleContext context)
        throws InvalidSyntaxException
    {
        super(context, EventListener.class);
    }

    @Override
    protected void setService(final EventListener service)
    {
        if ( service instanceof HttpSessionListener )
        {
            this.sessionListener = (HttpSessionListener)service;
        }
        if ( service instanceof HttpSessionIdListener )
        {
            this.sessionIdListener = (HttpSessionIdListener)service;
        }
        if ( service instanceof HttpSessionAttributeListener )
        {
            this.sessionAttributeListener = (HttpSessionAttributeListener)service;
        }
    }

    public HttpSessionListener getHttpSessionListener()
    {
        return this.sessionListener;
    }

    public HttpSessionIdListener getHttpSessionIdListener()
    {
        return this.sessionIdListener;
    }

    public HttpSessionAttributeListener getHttpSessionAttributeListener()
    {
        return this.sessionAttributeListener;
    }

    @Override
    protected void unsetService()
    {
        sessionListener = null;
        sessionIdListener = null;
        sessionAttributeListener = null;
    }
}
