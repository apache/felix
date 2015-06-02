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

import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Info object for registered listeners.
 */
public class ListenerInfo extends WhiteboardServiceInfo<EventListener>
{
    private static final Set<String> ALLOWED_INTERFACES;
    static {
        ALLOWED_INTERFACES = new HashSet<String>();
        ALLOWED_INTERFACES.add(HttpSessionAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(HttpSessionIdListener.class.getName());
        ALLOWED_INTERFACES.add(HttpSessionListener.class.getName());
        ALLOWED_INTERFACES.add(ServletContextAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(ServletContextListener.class.getName());
        ALLOWED_INTERFACES.add(ServletRequestAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(ServletRequestListener.class.getName());
    }

    private final String enabled;

    private final String[] types;

    public ListenerInfo(final ServiceReference<EventListener> ref)
    {
        super(ref);
        this.enabled = this.getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
        final String[] objectClass = (String[])ref.getProperty(Constants.OBJECTCLASS);
        final Set<String> names = new HashSet<String>();
        for(final String name : objectClass)
        {
            if ( ALLOWED_INTERFACES.contains(name) )
            {
                names.add(name);
            }
        }
        this.types = names.toArray(new String[names.size()]);
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && "true".equalsIgnoreCase(this.enabled);
    }

    public String[] getListenerTypes()
    {
        return this.types;
    }

    public boolean isListenerType(@Nonnull final String className)
    {
        for(final String t : this.types)
        {
            if ( t.equals(className) )
            {
                return true;
            }
        }
        return false;
    }
}
