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
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * The servlet registry keeps the mappings for all servlets (by using their pattern)
 * for a single servlet context.
 *
 * TODO - servlet name handling
 *
 * TODO - sort active servlet mappings by pattern length, longest first (avoids looping over all)
 *
 * TODO - check if add/remove needs syncing
 *
 * TODO - replace patterns with own matchers
 */
public final class ServletRegistry
{
    private final Map<String, ServletRegistration> activeServletMappings = new ConcurrentHashMap<String, ServletRegistration>();

    private final Map<String, List<ServletHandler>> inactiveServletMappings = new HashMap<String, List<ServletHandler>>();

    private final Map<ServletInfo, ServletRegistrationStatus> statusMapping = new ConcurrentHashMap<ServletInfo, ServletRegistry.ServletRegistrationStatus>();

    private final Map<String, List<ServletNameStatus>> servletsByName = new ConcurrentHashMap<String, List<ServletNameStatus>>();

    private static final class ServletNameStatus implements Comparable<ServletNameStatus>
    {
        public volatile boolean isActive = false;
        public final ServletHandler handler;

        public ServletNameStatus(final ServletHandler h)
        {
            this.handler = h;
        }

        @Override
        public int compareTo(final ServletNameStatus o)
        {
            return handler.compareTo(o.handler);
        }
    }

    public static final class ServletRegistrationStatus
    {
        public final Map<String, Integer> pathToStatus = new ConcurrentHashMap<String, Integer>();
    }

    public PathResolution resolve(final String relativeRequestURI)
    {
        int len = -1;
        PathResolution candidate = null;
        for(final Map.Entry<String, ServletRegistration> entry : this.activeServletMappings.entrySet())
        {
            final PathResolution pr = entry.getValue().resolve(relativeRequestURI);
            if ( pr != null && entry.getKey().length() > len )
            {
                candidate = pr;
                len = entry.getKey().length();
            }
        }
        return candidate;
    }

    /**
     * Add a servlet.
     *
     * @param handler The servlet handler
     */
    public void addServlet(@Nonnull final ServletHandler handler)
    {
        // we have to check for every pattern in the info
        // Can be null in case of error-handling servlets...
        if ( handler.getServletInfo().getPatterns() != null )
        {
            final ServletRegistrationStatus status = new ServletRegistrationStatus();

            for(final String pattern : handler.getServletInfo().getPatterns())
            {
                final ServletRegistration regHandler = this.activeServletMappings.get(pattern);
                if ( regHandler != null )
                {
                    if ( regHandler.getServletHandler().getServletInfo().getServiceReference().compareTo(handler.getServletInfo().getServiceReference()) < 0 )
                    {
                        // replace if no error with new servlet
                        if ( this.tryToActivate(pattern, handler, status) )
                        {
//                            nameStatus.isActive = true;
                            regHandler.getServletHandler().destroy();

                            this.addToInactiveList(pattern, regHandler.getServletHandler(), this.statusMapping.get(regHandler.getServletHandler().getServletInfo()));
                        }
                    }
                    else
                    {
                        // add to inactive
                        this.addToInactiveList(pattern, handler, status);
                    }
                }
                else
                {
                    // add to active
                    if ( this.tryToActivate(pattern, handler, status) )
                    {
//                        nameStatus.isActive = true;
                    }
                }
            }
            this.statusMapping.put(handler.getServletInfo(), status);
        }
    }

    /**
     * Remove a servlet
     * @param info The servlet info
     */
    public void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        if ( info.getPatterns() != null )
        {
            this.statusMapping.remove(info);
            ServletHandler cleanuphandler = null;

            for(final String pattern : info.getPatterns())
            {

                final ServletRegistration regHandler = this.activeServletMappings.get(pattern);
                if ( regHandler != null && regHandler.getServletHandler().getServletInfo().equals(info) )
                {
                    cleanuphandler = regHandler.getServletHandler();
                    final List<ServletHandler> inactiveList = this.inactiveServletMappings.get(pattern);
                    if ( inactiveList == null )
                    {
                        this.activeServletMappings.remove(pattern);
                    }
                    else
                    {
                        boolean done = false;
                        while ( !done )
                        {
                            final ServletHandler h = inactiveList.remove(0);
                            done = this.tryToActivate(pattern, h, this.statusMapping.get(h.getServletInfo()));
                            if ( !done )
                            {
                                done = inactiveList.isEmpty();
                            }
                        }
                        if ( inactiveList.isEmpty() )
                        {
                            this.inactiveServletMappings.remove(pattern);
                        }
                    }
                }
                else
                {
                    final List<ServletHandler> inactiveList = this.inactiveServletMappings.get(pattern);
                    if ( inactiveList != null )
                    {
                        final Iterator<ServletHandler> i = inactiveList.iterator();
                        while ( i.hasNext() )
                        {
                            final ServletHandler h = i.next();
                            if ( h.getServletInfo().equals(info) )
                            {
                                i.remove();
                                cleanuphandler = h;
                                break;
                            }
                        }
                        if ( inactiveList.isEmpty() )
                        {
                            this.inactiveServletMappings.remove(pattern);
                        }
                    }
                }
            }

            if ( cleanuphandler != null )
            {
                cleanuphandler.dispose();
            }
        }
    }

    private void addToInactiveList(final String pattern, final ServletHandler handler, final ServletRegistrationStatus status)
    {
        List<ServletHandler> inactiveList = this.inactiveServletMappings.get(pattern);
        if ( inactiveList == null )
        {
            inactiveList = new ArrayList<ServletHandler>();
            this.inactiveServletMappings.put(pattern, inactiveList);
        }
        inactiveList.add(handler);
        Collections.sort(inactiveList);
        status.pathToStatus.put(pattern, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
    }

    private boolean tryToActivate(final String pattern, final ServletHandler handler, final ServletRegistrationStatus status)
    {
        // add to active
        final int result = handler.init();
        if ( result == -1 )
        {
            final Pattern p = Pattern.compile(PatternUtil.convertToRegEx(pattern));
            final ServletRegistration reg = new ServletRegistration(handler, p);
            this.activeServletMappings.put(pattern, reg);

            // add ok
            status.pathToStatus.put(pattern, result);
            return true;
        }
        else
        {
            // add to failure
            status.pathToStatus.put(pattern, result);
            return false;
        }
    }

    public Map<ServletInfo, ServletRegistrationStatus> getServletStatusMapping()
    {
        return this.statusMapping;
    }

    public ServletHandler resolveByName(@Nonnull String name)
    {
        final List<ServletNameStatus> handlerList = this.servletsByName.get(name);
        if ( handlerList != null )
        {
            final ServletNameStatus status = handlerList.get(0);
            if ( status != null && status.isActive )
            {
                return status.handler;
            }
        }
        return null;
    }
}
