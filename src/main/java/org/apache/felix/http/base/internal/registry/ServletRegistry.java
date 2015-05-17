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

import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * The servlet registry keeps the mappings for all servlets (by using their pattern)
 * for a single servlet context.
 */
public final class ServletRegistry
{
    private final Map<String, ServletHandler> activateServletMapping = new ConcurrentHashMap<String, ServletHandler>();

    private final Map<String, List<ServletHolder>> inactivateServletMapping = new HashMap<String, List<ServletHolder>>();

    private final Map<ServletInfo, ServletRegistrationStatus> statusMapping = new ConcurrentHashMap<ServletInfo, ServletRegistry.ServletRegistrationStatus>();

    public static final class ServletRegistrationStatus
    {
        public final Map<String, Integer> pathToStatus = new ConcurrentHashMap<String, Integer>();
    }

    public PathResolution resolve(final String relativeRequestURI)
    {
        int len = -1;
        PathResolution candidate = null;
        for(final Map.Entry<String, ServletHandler> entry : this.activateServletMapping.entrySet())
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
     * Add a servlet
     * @param holder The servlet holder
     */
    public void addServlet(@Nonnull final ServletHolder holder)
    {
        final ServletRegistrationStatus status = new ServletRegistrationStatus();

        // we have to check for every pattern in the info
        // Can be null in case of error-handling servlets...
        if ( holder.getServletInfo().getPatterns() != null )
        {
            for(final String pattern : holder.getServletInfo().getPatterns())
            {
                final ServletHandler regHandler = this.activateServletMapping.get(pattern);
                if ( regHandler != null )
                {
                    if ( regHandler.getServletHolder().getServletInfo().getServiceReference().compareTo(holder.getServletInfo().getServiceReference()) < 0 )
                    {
                        // replace if no error with new servlet
                        if ( this.tryToActivate(pattern, holder, status) )
                        {
                            regHandler.getServletHolder().destroy();

                            this.addToInactiveList(pattern, regHandler.getServletHolder(), this.statusMapping.get(regHandler.getServletHolder().getServletInfo()));
                        }
                    }
                    else
                    {
                        // add to inactive
                        this.addToInactiveList(pattern, holder, status);
                    }
                }
                else
                {
                    // add to active
                    this.tryToActivate(pattern, holder, status);
                }
            }
            this.statusMapping.put(holder.getServletInfo(), status);
        }
    }

    /**
     * Remove a servlet
     * @param info The servlet info
     */
    public void removeServlet(@Nonnull final ServletInfo info)
    {
        if ( info.getPatterns() != null )
        {
            this.statusMapping.remove(info);
            ServletHolder cleanupHolder = null;

            for(final String pattern : info.getPatterns())
            {

                final ServletHandler regHandler = this.activateServletMapping.get(pattern);
                if ( regHandler != null && regHandler.getServletHolder().getServletInfo().equals(info) )
                {
                    cleanupHolder = regHandler.getServletHolder();
                    final List<ServletHolder> inactiveList = this.inactivateServletMapping.get(pattern);
                    if ( inactiveList == null )
                    {
                        this.activateServletMapping.remove(pattern);
                    }
                    else
                    {
                        boolean done = false;
                        while ( !done )
                        {
                            final ServletHolder h = inactiveList.remove(0);
                            done = this.tryToActivate(pattern, h, this.statusMapping.get(h.getServletInfo()));
                            if ( !done )
                            {
                                done = inactiveList.isEmpty();
                            }
                        }
                        if ( inactiveList.isEmpty() )
                        {
                            this.inactivateServletMapping.remove(pattern);
                        }
                    }
                }
                else
                {
                    final List<ServletHolder> inactiveList = this.inactivateServletMapping.get(pattern);
                    if ( inactiveList != null )
                    {
                        final Iterator<ServletHolder> i = inactiveList.iterator();
                        while ( i.hasNext() )
                        {
                            final ServletHolder h = i.next();
                            if ( h.getServletInfo().equals(info) )
                            {
                                i.remove();
                                cleanupHolder = h;
                                break;
                            }
                        }
                        if ( inactiveList.isEmpty() )
                        {
                            this.inactivateServletMapping.remove(pattern);
                        }
                    }
                }
            }

            if ( cleanupHolder != null )
            {
                cleanupHolder.dispose();
            }
        }
    }

    private void addToInactiveList(final String pattern, final ServletHolder holder, final ServletRegistrationStatus status)
    {
        List<ServletHolder> inactiveList = this.inactivateServletMapping.get(pattern);
        if ( inactiveList == null )
        {
            inactiveList = new ArrayList<ServletHolder>();
            this.inactivateServletMapping.put(pattern, inactiveList);
        }
        inactiveList.add(holder);
        Collections.sort(inactiveList);
        status.pathToStatus.put(pattern, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
    }

    private boolean tryToActivate(final String pattern, final ServletHolder holder, final ServletRegistrationStatus status)
    {
        // add to active
        final int result = holder.init();
        if ( result == -1 )
        {
            final Pattern p = Pattern.compile(PatternUtil.convertToRegEx(pattern));
            final ServletHandler handler = new ServletHandler(holder, p);
            this.activateServletMapping.put(pattern, handler);

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
}
