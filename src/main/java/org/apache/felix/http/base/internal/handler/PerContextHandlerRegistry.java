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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime.ErrorPage;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;

public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    private final Map<Servlet, ServletHandler> servletMap = new HashMap<Servlet, ServletHandler>();
    private final Map<Filter, FilterHandler> filterMap = new HashMap<Filter, FilterHandler>();
    private final Map<String, Servlet> servletPatternMap = new HashMap<String, Servlet>();
    private volatile HandlerMapping<ServletHandler> servletMapping = new HandlerMapping<ServletHandler>();
    private volatile HandlerMapping<FilterHandler> filterMapping = new HandlerMapping<FilterHandler>();
    private final ErrorsMapping errorsMapping = new ErrorsMapping();

    private final long serviceId;

    private final int ranking;

    private final String path;

    private final String prefix;

    public PerContextHandlerRegistry() {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
    }

    public PerContextHandlerRegistry(final ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        if ( this.path.equals("/") )
        {
        	prefix = null;
        }
        else
        {
        	prefix = this.path + "/";
        }
    }

    public synchronized void addFilter(FilterHandler handler) throws ServletException
    {
        if (this.filterMap.containsKey(handler.getFilter()))
        {
            throw new ServletException("Filter instance already registered");
        }

        handler.init();
        this.filterMap.put(handler.getFilter(), handler);

        updateFilterMapping();
    }

    @Override
    public int compareTo(final PerContextHandlerRegistry other)
    {
        final int result = Integer.compare(other.path.length(), this.path.length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId >= 0 && other.serviceId >= 0 ) ? 1 : -1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }

    /**
     * Add a new servlet.
     */
    public synchronized void addServlet(final ServletHandler handler) throws ServletException
    {
        // Can be null in case of error-handling servlets...
        String[] patterns = handler.getServletInfo().getPatterns();
        int length = patterns == null ? 0 : patterns.length;

        for (int i = 0; i < length; i++)
        {
            final String pattern = patterns[i];
            if (this.servletPatternMap.containsKey(pattern))
            {
                throw new ServletException("Servlet instance " + handler.getName() + " already registered");
            }
            this.servletPatternMap.put(pattern, handler.getServlet());
        }

        patterns = handler.getServletInfo().getErrorPage();
        if ( patterns != null )
        {
            for(final String errorPage : patterns)
            {
                this.errorsMapping.addErrorServlet(errorPage, handler);
            }
        }
        handler.init();
        this.servletMap.put(handler.getServlet(), handler);

        updateServletMapping();
    }

    public ErrorsMapping getErrorsMapping()
    {
        return this.errorsMapping;
    }

    public FilterHandler[] getFilterHandlers(ServletHandler servletHandler, DispatcherType dispatcherType, String requestURI)
    {
        // See Servlet 3.0 specification, section 6.2.4...
        List<FilterHandler> result = new ArrayList<FilterHandler>();
        result.addAll(this.filterMapping.getAllMatches(requestURI));

        // TODO this is not the most efficient/fastest way of doing this...
        Iterator<FilterHandler> iter = result.iterator();
        while (iter.hasNext())
        {
            if (!referencesDispatcherType(iter.next(), dispatcherType))
            {
                iter.remove();
            }
        }

        String servletName = (servletHandler != null) ? servletHandler.getName() : null;
        // TODO this is not the most efficient/fastest way of doing this...
        for (FilterHandler filterHandler : this.filterMapping.getAllElements())
        {
            if (referencesServletByName(filterHandler, servletName))
            {
                result.add(filterHandler);
            }
        }

        // TODO - we should already check for the context when building up the result set
        final Iterator<FilterHandler> i = result.iterator();
        while ( i.hasNext() )
        {
            final FilterHandler handler = i.next();
            if ( handler.getContextServiceId() != servletHandler.getContextServiceId() )
            {
                i.remove();
            }
        }
        return result.toArray(new FilterHandler[result.size()]);
    }

    public ServletHandler getServletHandlerByName(String name)
    {
        return this.servletMapping.getByName(name);
    }

    public ServletHandler getServletHander(String requestURI)
    {
        return this.servletMapping.getBestMatch(requestURI);
    }

    public synchronized void removeAll()
    {
        for (Iterator<ServletHandler> it = servletMap.values().iterator(); it.hasNext(); )
        {
            ServletHandler handler = it.next();
            it.remove();
            handler.destroy();
        }

        for (Iterator<FilterHandler> it = filterMap.values().iterator(); it.hasNext(); )
        {
            FilterHandler handler = it.next();
            it.remove();
            handler.destroy();
        }

        this.servletMap.clear();
        this.filterMap.clear();
        this.servletPatternMap.clear();
        this.errorsMapping.clear();

        updateServletMapping();
        updateFilterMapping();
    }

    public synchronized void removeFilter(Filter filter, final boolean destroy)
    {
        FilterHandler handler = this.filterMap.remove(filter);
        if (handler != null)
        {
            updateFilterMapping();
            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    public synchronized Filter removeFilter(final FilterInfo filterInfo, final boolean destroy)
    {
        for(final FilterHandler handler : this.filterMap.values())
        {
            if ( handler.getFilterInfo().compareTo(filterInfo) == 0)
            {
                this.filterMap.remove(handler.getFilter());
                updateFilterMapping();
                if (destroy)
                {
                    handler.destroy();
                }
                return handler.getFilter();
            }
        }
        return null;
    }

    public synchronized Servlet removeServlet(ServletInfo servletInfo, final boolean destroy)
    {
        for(final ServletHandler handler : this.servletMap.values())
        {
            if ( handler.getServletInfo().compareTo(servletInfo) == 0 )
            {
                this.servletMap.remove(handler.getServlet());
                updateServletMapping();

                // Can be null in case of error-handling servlets...
                String[] patterns = handler.getServletInfo().getPatterns();
                int length = patterns == null ? 0 : patterns.length;

                for (int i = 0; i < length; i++)
                {
                    this.servletPatternMap.remove(patterns[i]);
                }

                this.errorsMapping.removeServlet(handler.getServlet());

                if (destroy)
                {
                    handler.destroy();
                }
                return handler.getServlet();
            }
        }
        return null;
    }

    public synchronized void removeServlet(Servlet servlet, final boolean destroy)
    {
        ServletHandler handler = this.servletMap.remove(servlet);
        if (handler != null)
        {
            updateServletMapping();

            // Can be null in case of error-handling servlets...
            String[] patterns = handler.getServletInfo().getPatterns();
            int length = patterns == null ? 0 : patterns.length;

            for (int i = 0; i < length; i++)
            {
                this.servletPatternMap.remove(patterns[i]);
            }

            this.errorsMapping.removeServlet(servlet);

            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    private boolean referencesDispatcherType(FilterHandler handler, DispatcherType dispatcherType)
    {
        return Arrays.asList(handler.getFilterInfo().getDispatcher()).contains(dispatcherType);
    }

    private boolean referencesServletByName(FilterHandler handler, String servletName)
    {
        if (servletName == null)
        {
            return false;
        }
        String[] names = handler.getFilterInfo().getServletNames();
        if (names != null && names.length > 0)
        {
            return Arrays.asList(names).contains(servletName);
        }
        return false;
    }

    private void updateFilterMapping()
    {
        this.filterMapping = new HandlerMapping<FilterHandler>(this.filterMap.values());
    }

    private void updateServletMapping()
    {
        this.servletMapping = new HandlerMapping<ServletHandler>(this.servletMap.values());
    }

    public String isMatching(final String requestURI)
    {
        if ( requestURI.equals(this.path) )
        {
        	return "";
        }
        if ( this.prefix == null )
        {
        	return requestURI;
        }
        if ( requestURI.startsWith(this.prefix) )
        {
        	return requestURI.substring(this.prefix.length() - 1);
        }
        return null;
    }

    public long getContextServiceId()
    {
        return this.serviceId;
    }

    public synchronized HandlerRuntime getRuntime() {
        List<ServletHandler> servletHandlers = new ArrayList<ServletHandler>(servletMap.values());
        List<FilterHandler> filterHandlers = new ArrayList<FilterHandler>(filterMap.values());

        Collection<ErrorPage> errorPages = new ArrayList<HandlerRuntime.ErrorPage>();
        Collection<ServletHandler> errorHandlers = errorsMapping.getMappedHandlers();
        for (ServletHandler servletHandler : errorHandlers)
        {
            errorPages.add(errorsMapping.getErrorPage(servletHandler));
        }
        servletHandlers.removeAll(errorHandlers);

        return new HandlerRuntime(servletHandlers, filterHandlers, errorPages, serviceId);
    }
}
