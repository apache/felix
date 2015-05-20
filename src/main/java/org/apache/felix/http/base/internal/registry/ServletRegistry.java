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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ResourceDTOBuilder;
import org.apache.felix.http.base.internal.runtime.dto.ServletDTOBuilder;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

/**
 * The servlet registry keeps the mappings for all servlets (by using their pattern)
 * for a single servlet context.
 *
 * TODO - sort active servlet mappings by pattern length, longest first (avoids looping over all)
 *
 */
public final class ServletRegistry
{
    private final Map<String, ServletRegistration> activeServletMappings = new ConcurrentHashMap<String, ServletRegistration>();

    private final Map<String, List<ServletHandler>> inactiveServletMappings = new HashMap<String, List<ServletHandler>>();

    private final Map<ServletInfo, ServletRegistrationStatus> statusMapping = new ConcurrentHashMap<ServletInfo, ServletRegistry.ServletRegistrationStatus>();

    private final Map<String, List<ServletHandler>> servletsByName = new ConcurrentHashMap<String, List<ServletHandler>>();

    public static final class ServletRegistrationStatus
    {
        public final Map<String, Integer> pathToStatus = new ConcurrentHashMap<String, Integer>();
        public ServletHandler handler;
    }

    /**
     * Resolve a request uri
     *
     * @param relativeRequestURI The request uri
     * @return A path resolution if a servlet matched, {@code null} otherwise
     */
    public PathResolution resolve(@Nonnull final String relativeRequestURI)
    {
        PathResolver resolver = null;
        PathResolution candidate = null;
        for(final Map.Entry<String, ServletRegistration> entry : this.activeServletMappings.entrySet())
        {
            final PathResolution pr = entry.getValue().resolve(relativeRequestURI);
            if ( pr != null && (resolver == null || entry.getValue().getPathResolver().compareTo(resolver) < 0) )
            {
                // TODO - we should have all patterns under which this servlet is actively registered
                pr.patterns = new String[] {entry.getKey()};
                candidate = pr;
                resolver = entry.getValue().getPathResolver();
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
            status.handler = handler;

            boolean isActive = false;
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
                            isActive = true;
                            final String oldName = regHandler.getServletHandler().getName();
                            regHandler.getServletHandler().destroy();

                            this.addToInactiveList(pattern, regHandler.getServletHandler(), this.statusMapping.get(regHandler.getServletHandler().getServletInfo()));

                            if ( regHandler.getServletHandler().getServlet() == null )
                            {
                                removeFromNameMapping(oldName, regHandler.getServletHandler());
                            }
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
                        isActive = true;
                    }
                }
            }
            this.statusMapping.put(handler.getServletInfo(), status);
            if ( isActive )
            {
                addToNameMapping(handler);
            }
        }
    }

    private void addToNameMapping(final ServletHandler handler)
    {
        final String servletName = handler.getName();
        List<ServletHandler> list = this.servletsByName.get(servletName);
        if ( list == null )
        {
            list = new ArrayList<ServletHandler>();
            list.add(handler);
        }
        else
        {
            list = new ArrayList<ServletHandler>(list);
            list.add(handler);
            Collections.sort(list);
        }
        this.servletsByName.put(servletName, list);
    }

    private synchronized void removeFromNameMapping(final String servletName, final ServletHandler handler)
    {
        List<ServletHandler> list = this.servletsByName.get(servletName);
        if ( list != null )
        {
            final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
            final Iterator<ServletHandler> i = newList.iterator();
            while ( i.hasNext() )
            {
                final ServletHandler s = i.next();
                if ( s == handler )
                {
                    i.remove();
                    break;
                }
            }
            if ( newList.isEmpty() )
            {
                this.servletsByName.remove(servletName);
            }
            else
            {
                this.servletsByName.put(servletName, newList);
            }
        }
    }

    /**
     * Remove a servlet
     * @param info The servlet info
     */
    public synchronized void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        if ( info.getPatterns() != null )
        {
            this.statusMapping.remove(info);
            ServletHandler cleanupHandler = null;

            for(final String pattern : info.getPatterns())
            {
                final ServletRegistration regHandler = this.activeServletMappings.get(pattern);
                if ( regHandler != null && regHandler.getServletHandler().getServletInfo().equals(info) )
                {
                    cleanupHandler = regHandler.getServletHandler();
                    removeFromNameMapping(cleanupHandler.getName(), cleanupHandler);

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
                            boolean activate = h.getServlet() == null;
                            done = this.tryToActivate(pattern, h, this.statusMapping.get(h.getServletInfo()));
                            if ( !done )
                            {
                                done = inactiveList.isEmpty();
                            }
                            else
                            {
                                if ( activate )
                                {
                                    this.addToNameMapping(h);
                                }
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
                                cleanupHandler = h;
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

            if ( cleanupHandler != null )
            {
                cleanupHandler.dispose();
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
            final ServletRegistration reg = new ServletRegistration(handler, PathResolverFactory.create(pattern));
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

    Map<ServletInfo, ServletRegistrationStatus> getServletStatusMapping()
    {
        return this.statusMapping;
    }

    public ServletHandler resolveByName(final @Nonnull String name)
    {
        final List<ServletHandler> handlerList = this.servletsByName.get(name);
        if ( handlerList != null )
        {
            return handlerList.get(0);
        }
        return null;
    }

    public void getRuntimeInfo(
            final ServletContextDTO servletContextDTO,
            final Collection<FailedServletDTO> allFailedServletDTOs,
            final Collection<FailedResourceDTO> allFailedResourceDTOs)
    {
        final Map<Long, ServletDTO> servletDTOs = new HashMap<Long, ServletDTO>();
        final Map<Long, ResourceDTO> resourceDTOs = new HashMap<Long, ResourceDTO>();

        final Map<Long, FailedServletDTO> failedServletDTOs = new HashMap<Long, FailedServletDTO>();
        final Map<Long, FailedResourceDTO> failedResourceDTOs = new HashMap<Long, FailedResourceDTO>();

        // TODO we could already do some pre calculation in the ServletRegistrationStatus
        for(final Map.Entry<ServletInfo, ServletRegistrationStatus> entry : statusMapping.entrySet())
        {
            final long serviceId = entry.getKey().getServiceId();
            for(final Map.Entry<String, Integer> map : entry.getValue().pathToStatus.entrySet())
            {
                if ( !entry.getKey().isResource() )
                {
                    ServletDTO state = (map.getValue() == -1 ? servletDTOs.get(serviceId) : failedServletDTOs.get(serviceId));
                    if ( state == null )
                    {
                        state = ServletDTOBuilder.build(entry.getValue().handler, map.getValue());
                        if ( map.getValue() == -1 )
                        {
                            servletDTOs.put(serviceId, state);
                        }
                        else
                        {
                            failedServletDTOs.put(serviceId, (FailedServletDTO)state);
                        }
                    }
                    String[] patterns = state.patterns;
                    if ( patterns.length ==  0 )
                    {
                        state.patterns = new String[] {map.getKey()};
                    }
                    else
                    {
                        patterns = new String[patterns.length + 1];
                        System.arraycopy(state.patterns, 0, patterns, 0, patterns.length - 1);
                        patterns[patterns.length - 1] = map.getKey();
                        state.patterns = patterns;
                    }
                }
                else
                {
                    ResourceDTO state = (map.getValue() == -1 ? resourceDTOs.get(serviceId) : failedResourceDTOs.get(serviceId));
                    if ( state == null )
                    {
                        state = ResourceDTOBuilder.build(entry.getValue().handler, map.getValue());
                        if ( map.getValue() == -1 )
                        {
                            resourceDTOs.put(serviceId, state);
                        }
                        else
                        {
                            failedResourceDTOs.put(serviceId, (FailedResourceDTO)state);
                        }
                    }
                    String[] patterns = state.patterns;
                    if ( patterns.length ==  0 )
                    {
                        state.patterns = new String[] {map.getKey()};
                    }
                    else
                    {
                        patterns = new String[patterns.length + 1];
                        System.arraycopy(state.patterns, 0, patterns, 0, patterns.length - 1);
                        patterns[patterns.length - 1] = map.getKey();
                        state.patterns = patterns;
                    }
                }
            }
        }

        final Collection<ServletDTO> servletDTOsArray = servletDTOs.values();
        if ( !servletDTOsArray.isEmpty() )
        {
            servletContextDTO.servletDTOs = servletDTOsArray.toArray(new ServletDTO[servletDTOsArray.size()]);
        }
        final Collection<ResourceDTO> resourceDTOsArray = resourceDTOs.values();
        if ( !resourceDTOsArray.isEmpty() )
        {
            servletContextDTO.resourceDTOs = resourceDTOsArray.toArray(new ResourceDTO[resourceDTOsArray.size()]);
        }
        allFailedResourceDTOs.addAll(failedResourceDTOs.values());
        allFailedServletDTOs.addAll(failedServletDTOs.values());
    }
}
