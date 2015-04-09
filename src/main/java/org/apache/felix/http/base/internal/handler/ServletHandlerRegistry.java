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

public final class ServletHandlerRegistry
{
    private volatile HandlerMapping<ServletHandler> servletMapping = new HandlerMapping<ServletHandler>();
    private final HandlerRankingMultimap<String> registeredServletHandlers;
    private final SortedSet<ServletHandler> allServletHandlers = new TreeSet<ServletHandler>();

    private final ContextServletHandlerComparator handlerComparator;

    public ServletHandlerRegistry()
    {
        PatternComparator keyComparator = PatternComparator.INSTANCE;
        handlerComparator = new ContextServletHandlerComparator();
        this.registeredServletHandlers = new HandlerRankingMultimap<String>(null, handlerComparator);
    }

    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        handlerComparator.contextRankings.put(0L, new ContextRanking());
    }

    public void shutdown()
    {
        removeAll();
    }

    public void add(@Nonnull ServletContextHelperInfo info)
    {
        handlerComparator.contextRankings.put(info.getServiceId(), new ContextRanking(info));
    }

    public void remove(@Nonnull ServletContextHelperInfo info)
    {
        handlerComparator.contextRankings.remove(info.getServiceId());
    }

    public ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        return servletMapping.getByName(name);
    }

    public synchronized void addServlet(final ServletHandler handler) throws RegistrationFailureException
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
        String contextPath = handlerComparator.getPath(handler.getContextServiceId());
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
        this.servletMapping = this.servletMapping.update(convert(update.getActivated()), convert(update.getDeactivated()));
        destroyHandlers(update.getDestroy());
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

    public synchronized void removeAll()
    {
        Collection<ServletHandler> servletHandlers = this.servletMapping.values();

        this.servletMapping = new HandlerMapping<ServletHandler>();

        destroyHandlers(servletHandlers);

        this.allServletHandlers.clear();
        this.registeredServletHandlers.clear();
    }

    synchronized Servlet removeServlet(ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(0L, servletInfo, true);
    }

    public synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(contextId, servletInfo, true);
    }

    public synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo, final boolean destroy) throws RegistrationFailureException
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
        String contextPath = handlerComparator.getPath(handler.getContextServiceId());
        contextPath = contextPath.equals("/") ? "" : contextPath;
        List<String> patterns = new ArrayList<String>(asList(handler.getServletInfo().getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            patterns.set(i, contextPath + patterns.get(i));
        }
        Update<String> update = this.registeredServletHandlers.remove(patterns, handler);
        initHandlers(update.getInit());
        this.servletMapping = this.servletMapping.update(convert(update.getActivated()), convert(update.getDeactivated()));
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

    public ServletHandler getServletHandler(String requestURI)
    {
        return this.servletMapping.getBestMatch(requestURI);
    }

    public ServletHandler getServletHandlerByName(String name)
    {
        return this.servletMapping.getByName(name);
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


    public synchronized ServletRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
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

    private static class ContextServletHandlerComparator implements Comparator<ServletHandler>
    {
        private final Map<Long, ContextRanking> contextRankings = new HashMap<Long, ContextRanking>();

        @Override
        public int compare(ServletHandler o1, ServletHandler o2)
        {
            ContextRanking contextRankingOne = contextRankings.get(o1.getContextServiceId());
            ContextRanking contextRankingTwo = contextRankings.get(o2.getContextServiceId());
            int contextComparison = contextRankingOne.compareTo(contextRankingTwo);
            return contextComparison == 0 ? o1.compareTo(o2) : contextComparison;
        }

       String getPath(long contextId)
       {
           return contextRankings.get(contextId).path;
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
    }
}
