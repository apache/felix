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
package org.apache.felix.http.base.internal.handler;

import static java.util.Arrays.asList;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_ALREAY_USED;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.HandlerRankingMultimap.Update;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.apache.felix.http.base.internal.util.PatternUtil.PatternComparator;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

final class ServletHandlerRegistry
{
    private final HandlerRankingMultimap<String> registeredServletHandlers;
    private final SortedSet<ServletHandler> allServletHandlers = new TreeSet<ServletHandler>();

    private volatile ContextRegistry contextRegistry;

    ServletHandlerRegistry()
    {
        this.contextRegistry = new ContextRegistry();
        this.registeredServletHandlers = new HandlerRankingMultimap<String>(null, new ServletHandlerComparator());
    }

    /**
     * Register default context registry for Http Service
     */
    synchronized void init()
    {
        contextRegistry = contextRegistry.add(0L, new ContextRanking());
    }

    synchronized void shutdown()
    {
        removeAll();
    }

    synchronized void add(@Nonnull ServletContextHelperInfo info)
    {
        contextRegistry = contextRegistry.add(info);
    }

    synchronized void remove(@Nonnull ServletContextHelperInfo info)
    {
        contextRegistry = contextRegistry.remove(info);
    }

    synchronized void addServlet(final ServletHandler handler) throws RegistrationFailureException
    {
        if (this.allServletHandlers.contains(handler))
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVICE_ALREAY_USED,
                "Filter instance " + handler.getName() + " already registered");
        }

        registerServlet(handler);

        this.allServletHandlers.add(handler);
    }

    private void registerServlet(ServletHandler handler) throws RegistrationFailureException
    {
        String contextPath = contextRegistry.getPath(handler.getContextServiceId());
        if (contextPath == null)
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVLET_CONTEXT_FAILURE);
        }
        contextPath = contextPath.equals("/") ? "" : contextPath;
        List<String> patterns = new ArrayList<String>(asList(handler.getServletInfo().getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            patterns.set(i, contextPath + patterns.get(i));
        }
        Update<String> update = this.registeredServletHandlers.add(patterns, handler);
        initHandlers(update.getInit());
        contextRegistry = contextRegistry.updateServletMapping(update, handler.getContextServiceId());
        destroyHandlers(update.getDestroy());
    }

    synchronized void removeAll()
    {
        Collection<ServletHandler> servletHandlers = this.contextRegistry.getServletHandlers();

        this.contextRegistry = new ContextRegistry();

        destroyHandlers(servletHandlers);

        this.allServletHandlers.clear();
        this.registeredServletHandlers.clear();
    }

    synchronized Servlet removeServlet(ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(0L, servletInfo, true);
    }

    synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(contextId, servletInfo, true);
    }

    synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo, final boolean destroy) throws RegistrationFailureException
    {
        ServletHandler handler = getServletHandler(servletInfo);
        if (handler == null)
        {
            return null;
        }

        Servlet servlet = handler.getServlet();

        removeServlet(handler, destroy);

        if (destroy)
        {
            handler.destroy();
        }


        return servlet;
    }

    private ServletHandler getServletHandler(final ServletInfo servletInfo)
    {
        Iterator<ServletHandler> it = this.allServletHandlers.iterator();
        while (it.hasNext())
        {
            ServletHandler handler = it.next();
            if (handler.getServletInfo().compareTo(servletInfo) == 0)
            {
                return handler;
            }
        }
        return null;
    }

    synchronized void removeServlet(Servlet servlet, final boolean destroy) throws RegistrationFailureException
    {
        Iterator<ServletHandler> it = this.allServletHandlers.iterator();
        List<ServletHandler> removals = new ArrayList<ServletHandler>();
        while (it.hasNext())
        {
            ServletHandler handler = it.next();
            if (handler.getServlet() != null && handler.getServlet() == servlet)
            {
                removals.add(handler);
            }
        }

        for (ServletHandler servletHandler : removals)
        {
            removeServlet(0L, servletHandler.getServletInfo(), destroy);
        }
    }

    private void removeServlet(ServletHandler handler, boolean destroy) throws RegistrationFailureException
    {
        String contextPath = contextRegistry.getPath(handler.getContextServiceId());
        contextPath = contextPath.equals("/") ? "" : contextPath;
        List<String> patterns = new ArrayList<String>(asList(handler.getServletInfo().getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            patterns.set(i, contextPath + patterns.get(i));
        }
        Update<String> update = this.registeredServletHandlers.remove(patterns, handler);
        initHandlers(update.getInit());
        contextRegistry = contextRegistry.updateServletMapping(update, handler.getContextServiceId());
        if (destroy)
        {
            destroyHandlers(update.getDestroy());
        }
        this.allServletHandlers.remove(handler);
    }

    private void initHandlers(Collection<ServletHandler> handlers) throws RegistrationFailureException
    {
        for (ServletHandler servletHandler : handlers)
        {
            try
            {
                servletHandler.init();
            }
            catch (ServletException e)
            {
                // TODO we should collect this cases and not throw an exception immediately
                throw new RegistrationFailureException(servletHandler.getServletInfo(), FAILURE_REASON_EXCEPTION_ON_INIT, e);
            }
        }
    }

    private void destroyHandlers(Collection<? extends AbstractHandler<?>> servletHandlers)
    {
        for (AbstractHandler<?> handler : servletHandlers)
        {
            handler.destroy();
        }
    }

    ServletHandler getServletHandler(String requestURI)
    {
        return contextRegistry.getServletHandler(requestURI);
    }

    ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        HandlerMapping<ServletHandler> servletMapping = contextRegistry.getServletMapping(contextId);
        return servletMapping != null ? servletMapping.getByName(name) : null;
    }

    synchronized ServletRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        addShadowedHandlers(failureRuntimeBuilder, this.registeredServletHandlers.getShadowedValues());

        Collection<ServletRuntime> servletRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletRuntime> resourceRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        for (ServletHandler activeHandler : this.registeredServletHandlers.getActiveValues())
        {
            if (activeHandler.getServletInfo().isResource())
            {
                resourceRuntimes.add(activeHandler);
            }
            else
            {
                servletRuntimes.add(activeHandler);
            }
        }
        return new ServletRegistryRuntime(servletRuntimes, resourceRuntimes);
    }

    private void addShadowedHandlers(FailureRuntime.Builder failureRuntimeBuilder, Collection<ServletHandler> handlers)
    {
        for (ServletHandler handler : handlers)
        {
            failureRuntimeBuilder.add(handler.getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        }
    }

    private class ServletHandlerComparator implements Comparator<ServletHandler>
    {
        @Override
        public int compare(ServletHandler o1, ServletHandler o2)
        {
            ContextRanking contextRankingOne = contextRegistry.getContextRanking(o1.getContextServiceId());
            ContextRanking contextRankingTwo = contextRegistry.getContextRanking(o2.getContextServiceId());
            int contextComparison = contextRankingOne.compareTo(contextRankingTwo);
            return contextComparison == 0 ? o1.compareTo(o2) : contextComparison;
        }
    }

    private static class ContextRegistry
    {
        private final Map<Long, ContextRanking> contextRankingsPerId;
        private final TreeSet<ContextRanking> contextRankings;
        private final Map<Long, HandlerMapping<ServletHandler>> servletMappingsPerContext;

        ContextRegistry()
        {
            this(new HashMap<Long, ContextRanking>(),
                new TreeSet<ContextRanking>(),
                new HashMap<Long, HandlerMapping<ServletHandler>>());
        }

        ContextRegistry(Map<Long, ContextRanking> contextRankingsPerId, TreeSet<ContextRanking> contextRankings, Map<Long, HandlerMapping<ServletHandler>> servletMappingsPerContext)
        {
            this.contextRankingsPerId = contextRankingsPerId;
            this.contextRankings = contextRankings;
            this.servletMappingsPerContext = servletMappingsPerContext;
        }

        ContextRanking getContextRanking(long contextServiceId)
        {
            return contextRankingsPerId.get(contextServiceId);
        }

        ServletHandler getServletHandler(String requestURI)
        {
            List<Long> contextIds = getContextId(requestURI);
            for (Long contextId : contextIds)
            {
                HandlerMapping<ServletHandler> servletMapping = this.servletMappingsPerContext.get(contextId);
                if (servletMapping != null)
                {
                    ServletHandler bestMatch = servletMapping.getBestMatch(requestURI);
                    if (bestMatch != null)
                    {
                        return bestMatch;
                    }
                }
            }
            return null;
        }

        HandlerMapping<ServletHandler> getServletMapping(Long contextId)
        {
            return servletMappingsPerContext.get(contextId);
        }

        ContextRegistry add(long id, ContextRanking contextRanking)
        {
            Map<Long, ContextRanking> newContextRankingsPerId = new HashMap<Long, ContextRanking>(contextRankingsPerId);
            TreeSet<ContextRanking> newContextRankings = new TreeSet<ContextRanking>(contextRankings);
            Map<Long, HandlerMapping<ServletHandler>> newServletMappingsPerContext = new HashMap<Long, HandlerMapping<ServletHandler>>(servletMappingsPerContext);
            newContextRankingsPerId.put(id, contextRanking);
            newContextRankings.add(contextRanking);
            newServletMappingsPerContext.put(id, new HandlerMapping<ServletHandler>());

            return new ContextRegistry(newContextRankingsPerId, newContextRankings, newServletMappingsPerContext);
        }

        ContextRegistry add(ServletContextHelperInfo info)
        {
            return add(info.getServiceId(), new ContextRanking(info));
        }

        ContextRegistry remove(ServletContextHelperInfo info)
        {
            Map<Long, ContextRanking> newContextRankingsPerId = new HashMap<Long, ContextRanking>(contextRankingsPerId);
            TreeSet<ContextRanking> newContextRankings = new TreeSet<ContextRanking>(contextRankings);
            Map<Long, HandlerMapping<ServletHandler>> newServletMappingsPerContext = new HashMap<Long, HandlerMapping<ServletHandler>>(servletMappingsPerContext);
            newContextRankingsPerId.remove(info.getServiceId());
            newContextRankings.remove(new ContextRanking(info));
            newServletMappingsPerContext.remove(info.getServiceId());

            return new ContextRegistry(newContextRankingsPerId, newContextRankings, newServletMappingsPerContext);
        }

        String getPath(long contextId)
        {
            return contextRankingsPerId.get(contextId).path;
        }

        private List<Long> getContextId(String path)
        {
            List<Long> ids = new ArrayList<Long>();
            for (ContextRanking contextRanking : contextRankings)
            {
                if (contextRanking.isMatching(path))
                {
                    ids.add(contextRanking.serviceId);
                }
            }
            return ids;
        }

        ContextRegistry updateServletMapping(Update<String> update, long contextId)
        {
            Map<Long, HandlerMapping<ServletHandler>> newServletMappingsPerContext = new HashMap<Long, HandlerMapping<ServletHandler>>(servletMappingsPerContext);
            HandlerMapping<ServletHandler> servletMapping = newServletMappingsPerContext.get(contextId);
            if (servletMapping == null)
            {
                servletMapping = new HandlerMapping<ServletHandler>();
            }
            newServletMappingsPerContext.put(contextId, servletMapping.update(convert(update.getActivated()), convert(update.getDeactivated())));
            return new ContextRegistry(contextRankingsPerId, contextRankings, newServletMappingsPerContext);
        }

        private Map<Pattern, ServletHandler> convert(Map<String, ServletHandler> mapping)
        {
            TreeMap<Pattern, ServletHandler> converted = new TreeMap<Pattern, ServletHandler>(PatternComparator.INSTANCE);
            for (Map.Entry<String, ServletHandler> entry : mapping.entrySet())
            {
                Pattern pattern = Pattern.compile(PatternUtil.convertToRegEx(entry.getKey()));
                converted.put(pattern, entry.getValue());
            }
            return converted;
        }

        Collection<ServletHandler> getServletHandlers()
        {
            Collection<ServletHandler> servletHandlers = new ArrayList<ServletHandler>();
            for (HandlerMapping<ServletHandler> servletMapping : this.servletMappingsPerContext.values())
            {
                servletHandlers.addAll(servletMapping.values());
            }

            return servletHandlers;
        }
    }

    // TODO combine with PerContextHandlerRegistry
    private static class ContextRanking implements Comparable<ContextRanking>
    {
        private final long serviceId;
        private final int ranking;
        private final String path;

        ContextRanking()
        {
            this.serviceId = 0;
            this.ranking = Integer.MAX_VALUE;
            this.path = "/";
        }

        ContextRanking(ServletContextHelperInfo info)
        {
            this.serviceId = info.getServiceId();
            this.ranking = info.getRanking();
            this.path = info.getPath();
        }

        @Override
        public int compareTo(ContextRanking other)
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

        boolean isMatching(final String requestURI)
        {
            return "".equals(path) || "/".equals(path) || requestURI.startsWith(path);
        }
    }
}
