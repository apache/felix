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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.util.PatternUtil;

/**
 * This registry keeps track of all processing components per context:
 * - servlets
 * - filters
 * - error pages
 */
public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    /** Service id of the context. */
    private final long serviceId;

    /** Ranking of the context. */
    private final int ranking;

    /** The context path. */
    private final String path;

    /** The context prefix. */
    private final String prefix;

    private Map<String, ServletHandler> activateServletMappings = new ConcurrentHashMap<String, ServletHandler>();

    private Map<String, List<ServletHolder>> inactivateServletMappings = new HashMap<String, List<ServletHolder>>();

    /**
     * Default http service registry
     */
    public PerContextHandlerRegistry()
    {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
    }

    /**
     * Registry for a servlet context helper (whiteboard support)
     * @param info The servlet context helper info
     */
    public PerContextHandlerRegistry(@Nonnull final ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        if ( this.path.equals("/") )
        {
            this.prefix = null;
        }
        else
        {
            this.prefix = this.path + "/";
        }
    }

    @Override
    public int compareTo(@Nonnull final PerContextHandlerRegistry other)
    {
        // the context of the HttpService is the least element
        if (this.serviceId == 0 ^ other.serviceId == 0)
        {
            return this.serviceId == 0 ? -1 : 1;
        }

        final int result = Integer.compare(other.path.length(), this.path.length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId <= 0 && other.serviceId <= 0 ) ? -1 : 1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }

    public String isMatching(final String requestURI)
    {
        if (requestURI.equals(this.path))
        {
            return "";
        }
        if (this.prefix == null)
        {
            return requestURI;
        }
        if (requestURI.startsWith(this.prefix))
        {
            return requestURI.substring(this.prefix.length() - 1);
        }
        return null;
    }

    public PathResolution resolve(final String relativeRequestURI)
    {
        int len = -1;
        PathResolution candidate = null;
        for(final Map.Entry<String, ServletHandler> entry : this.activateServletMappings.entrySet())
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
     * @param info The servlet info
     */
    public void addServlet(@Nonnull final ServletHolder holder)
    {
        // we have to check for every pattern in the info
        // Can be null in case of error-handling servlets...
        final String[] patternStrings = holder.getServletInfo().getPatterns();
        if ( patternStrings != null )
        {
            final int length = patternStrings.length;
            for (int i = 0; i < length; i++)
            {
                final String pattern = patternStrings[i];

                final ServletHandler regHandler = this.activateServletMappings.get(pattern);
                if ( regHandler != null )
                {
                    if ( regHandler.getServletHolder().getServletInfo().getServiceReference().compareTo(holder.getServletInfo().getServiceReference()) < 0 )
                    {
                        // replace if no error with new servlet
                        if ( holder.init() == -1 )
                        {
                            final Pattern p = Pattern.compile(PatternUtil.convertToRegEx(pattern));
                            final ServletHandler handler = new ServletHandler(holder, p);
                            this.activateServletMappings.put(pattern, handler);

                            regHandler.getServletHolder().destroy();

                            this.addToInactiveList(pattern, regHandler.getServletHolder());
                        }
                        else
                        {
                            // TODO - add to failure
                        }
                    }
                    else
                    {
                        // add to inactive
                        this.addToInactiveList(pattern, holder);
                    }
                }
                else
                {
                    // add to active
                    if ( holder.init() == -1 )
                    {
                        final Pattern p = Pattern.compile(PatternUtil.convertToRegEx(pattern));
                        final ServletHandler handler = new ServletHandler(holder, p);
                        this.activateServletMappings.put(pattern, handler);
                    }
                    else
                    {
                        // TODO - add to failure
                    }
                }
            }
        }
    }

    private void addToInactiveList(final String pattern, final ServletHolder holder)
    {
        List<ServletHolder> inactiveList = this.inactivateServletMappings.get(pattern);
        if ( inactiveList == null )
        {
            inactiveList = new ArrayList<ServletHolder>(inactiveList);
            this.inactivateServletMappings.put(pattern, inactiveList);
        }
        inactiveList.add(holder);
        Collections.sort(inactiveList);
    }
}
