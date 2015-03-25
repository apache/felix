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

import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_ALREAY_USED;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ErrorPageRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FilterRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.whiteboard.ResourceServlet;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;

public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    private final BundleContext bundleContext;

    private final Map<Filter, FilterHandler> filterMap = new HashMap<Filter, FilterHandler>();

    private volatile HandlerMapping<ServletHandler> servletMapping = new HandlerMapping<ServletHandler>();
    private volatile HandlerMapping<FilterHandler> filterMapping = new HandlerMapping<FilterHandler>();
    private final ErrorsMapping errorsMapping = new ErrorsMapping();

    private final SortedMap<Pattern, SortedSet<ServletHandler>> patternToServletHandler = new TreeMap<Pattern, SortedSet<ServletHandler>>(PatternUtil.PatternComparator.INSTANCE);
    private final Map<ServletHandler, Integer> servletHandlerToUses = new HashMap<ServletHandler, Integer>();
    private final SortedSet<ServletHandler> allServletHandlers = new TreeSet<ServletHandler>();

    private final long serviceId;

    private final int ranking;

    private final String path;

    private final String prefix;

    public PerContextHandlerRegistry(BundleContext bundleContext)
    {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
        this.bundleContext = bundleContext;
    }

    public PerContextHandlerRegistry(final ServletContextHelperInfo info, BundleContext bundleContext)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        this.bundleContext = bundleContext;
        if (this.path.equals("/"))
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
        if (this.filterMapping.contains(handler))
        {
            throw new RegistrationFailureException(handler.getFilterInfo(), FAILURE_REASON_SERVICE_ALREAY_USED, "Filter instance " + handler.getName() + " already registered");
        }

        handler.init();
        this.filterMapping = this.filterMapping.add(handler);
        this.filterMap.put(handler.getFilter(), handler);
    }

    @Override
    public int compareTo(final PerContextHandlerRegistry other)
    {
        final int result = Integer.compare(other.path.length(), this.path.length());
        if (result == 0)
        {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = (this.serviceId >= 0 && other.serviceId >= 0) ? 1 : -1;
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
        Pattern[] patterns = handler.getPatterns();
        String[] errorPages = handler.getServletInfo().getErrorPage();

        if (patterns.length > 0 && errorPages != null)
        {
            throw new ServletException("Servlet instance " + handler.getName() + " has both patterns and errorPage set");
        }

        SortedMap<Pattern, ServletHandler> toAdd = new TreeMap<Pattern, ServletHandler>(PatternUtil.PatternComparator.INSTANCE);
        SortedMap<Pattern, ServletHandler> toRemove = new TreeMap<Pattern, ServletHandler>(PatternUtil.PatternComparator.INSTANCE);

        this.servletHandlerToUses.put(handler, new Integer(0));

        for (Pattern p : patterns)
        {
            ServletHandler prevHandler = null;

            if (!this.patternToServletHandler.containsKey(p))
            {
                this.patternToServletHandler.put(p, new TreeSet<ServletHandler>());
            }
            else
            {
                prevHandler = this.patternToServletHandler.get(p).first();
            }

            this.patternToServletHandler.get(p).add(handler);

            if (handler.equals(this.patternToServletHandler.get(p).first()))
            {
                useServletHandler(handler);
                if (!handler.isWhiteboardService())
                {
                    handler.init();
                }
                increaseUseCount(handler);

                if (prevHandler != null)
                {
                    decreaseUseCount(prevHandler);
                    toRemove.put(p, prevHandler);
                }
                toAdd.put(p, handler);
            }
        }

        this.servletMapping = this.servletMapping.remove(toRemove);
        this.servletMapping = this.servletMapping.add(toAdd);
        this.allServletHandlers.add(handler);

        if (errorPages != null)
        {
            for (String errorPage : errorPages)
            {
                useServletHandler(handler);
                this.errorsMapping.addErrorServlet(errorPage, handler);
            }
        }
    }

    /**
     * Ensures the servlet handler contains a valid servlet object.
     * It gets one from the ServiceRegistry if the servlet handler was added by the whiteboard implementation
     * and the object was not yet retrieved.
     * 
     * @param handler
     * @throws ServletException
     */
    private void useServletHandler(ServletHandler handler) throws ServletException
    {
        if ((!handler.isWhiteboardService()) || (handler.getServlet() != null))
        {
            return;
        }

        // isWhiteboardService && servlet == null
        boolean isResource = handler.getServletInfo().isResource();
        final ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(handler.getServletInfo().getServiceReference());

        Servlet servlet = getServiceObject(so, handler, isResource);
        handler.setServlet(servlet);

        try
        {
            handler.init();
        }
        catch (ServletException e)
        {
            ungetServiceObject(so, servlet, isResource);
            throw e;
        }
    }

    private Servlet getServiceObject(ServiceObjects<Servlet> so, ServletHandler handler, boolean isResource)
    {
        if (isResource)
        {
            return new ResourceServlet(handler.getServletInfo().getPrefix());
        }
        if (so != null)
        {
            return so.getService();
        }
        return null;
    }

    private void ungetServiceObject(ServiceObjects<Servlet> so, Servlet servlet, boolean isResource)
    {
        if (isResource || (so == null))
        {
            return;
        }
        so.ungetService(servlet);
    }

    private void increaseUseCount(ServletHandler handler)
    {
        Integer uses = this.servletHandlerToUses.get(handler);
        if (uses != null)
        {
            int newUsesValue = uses.intValue() + 1;
            this.servletHandlerToUses.put(handler, new Integer(newUsesValue));
        }
    }

    private void decreaseUseCount(@Nonnull ServletHandler handler)
    {
        Integer uses = this.servletHandlerToUses.get(handler);
        if (uses != null)
        {
            int newUsesValue = uses.intValue() - 1;
            if (newUsesValue == 0 && handler.isWhiteboardService())
            {
                // if the servlet is no longer used and it is registered as a whiteboard service
                // call destroy, unget the service object and set the servlet in the handler to null
                handler.destroy();
                ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(handler.getServletInfo().getServiceReference());
                ungetServiceObject(so, handler.getServlet(), handler.getServletInfo().isResource());
                handler.setServlet(null);
            }
            this.servletHandlerToUses.put(handler, new Integer(newUsesValue));
        }
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
        for (FilterHandler filterHandler : this.filterMapping.values())
        {
            if (referencesServletByName(filterHandler, servletName))
            {
                result.add(filterHandler);
            }
        }

        // TODO - we should already check for the context when building up the result set
        final Iterator<FilterHandler> i = result.iterator();
        while (i.hasNext())
        {
            final FilterHandler handler = i.next();
            if (handler.getContextServiceId() != servletHandler.getContextServiceId())
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
        Collection<ServletHandler> servletHandlers = servletMapping.values();
        Collection<FilterHandler> filterHandlers = filterMapping.values();

        this.servletMapping = new HandlerMapping<ServletHandler>();
        this.filterMapping = new HandlerMapping<FilterHandler>();

        for (ServletHandler handler : servletHandlers)
        {
            handler.destroy();
        }

        for (FilterHandler handler : filterHandlers)
        {
            handler.destroy();
        }

        this.errorsMapping.clear();
        this.allServletHandlers.clear();
        //this.servletMap.clear();
        this.filterMap.clear();
    }

    public synchronized void removeFilter(Filter filter, final boolean destroy)
    {
        FilterHandler handler = this.filterMap.remove(filter);
        if (handler != null)
        {
            this.filterMapping = this.filterMapping.remove(handler);
            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    public synchronized Filter removeFilter(final FilterInfo filterInfo, final boolean destroy)
    {
        FilterHandler handler = getFilterHandler(filterInfo);

        if (handler == null)
        {
            return null;
        }

        this.filterMapping = this.filterMapping.remove(handler);

        if (destroy)
        {
            handler.destroy();
        }
        return handler.getFilter();
    }

    private FilterHandler getFilterHandler(final FilterInfo filterInfo)
    {
        for (final FilterHandler handler : this.filterMap.values())
        {
            if (handler.getFilterInfo().compareTo(filterInfo) == 0)
            {
                return handler;
            }
        }
        return null;
    }

    public synchronized Servlet removeServlet(ServletInfo servletInfo, final boolean destroy) throws RegistrationFailureException
    {
        ServletHandler handler = getServletHandler(servletInfo);

        Pattern[] patterns = (handler == null) ? new Pattern[0] : handler.getPatterns();
        SortedMap<Pattern, ServletHandler> toAdd = new TreeMap<Pattern, ServletHandler>(PatternUtil.PatternComparator.INSTANCE);
        SortedMap<Pattern, ServletHandler> toRemove = new TreeMap<Pattern, ServletHandler>(PatternUtil.PatternComparator.INSTANCE);

        for (Pattern p : patterns)
        {
            SortedSet<ServletHandler> handlers = this.patternToServletHandler.get(p);
            if (handlers != null && (!handlers.isEmpty()))
            {
                if (handlers.first().equals(handler))
                {
                    toRemove.put(p, handler);
                }
                handlers.remove(handler);

                ServletHandler activeHandler = null;
                if (!handlers.isEmpty())
                {
                    activeHandler = handlers.first();

                    try
                    {
                        useServletHandler(activeHandler);
                        increaseUseCount(activeHandler);
                        toAdd.put(p, activeHandler);
                    }
                    catch (ServletException e)
                    {
                        throw new RegistrationFailureException(activeHandler.getServletInfo(), FAILURE_REASON_EXCEPTION_ON_INIT, e);
                    }
                }
                else
                {
                    this.patternToServletHandler.remove(p);
                }
            }
        }

        Servlet servlet = null;
        if (handler != null && handler.getServlet() != null)
        {
            servlet = handler.getServlet();
            if (destroy)
            {
                servlet.destroy();
            }
            if (handler.isWhiteboardService())
            {
                ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(handler.getServletInfo().getServiceReference());
                ungetServiceObject(so, servlet, servletInfo.isResource());
            }
        }

        this.servletHandlerToUses.remove(handler);

        this.servletMapping = this.servletMapping.remove(toRemove);
        this.servletMapping = this.servletMapping.add(toAdd);

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

    public synchronized void removeServlet(Servlet servlet, final boolean destroy) throws RegistrationFailureException
    {
        Iterator<ServletHandler> it = this.allServletHandlers.iterator();
        while (it.hasNext())
        {
            ServletHandler handler = it.next();
            if (handler.getServlet() == servlet)
            {
                removeServlet(handler.getServletInfo(), destroy);
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

    public long getContextServiceId()
    {
        return this.serviceId;
    }

    public synchronized ContextRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        Collection<ErrorPageRuntime> errorPages = new TreeSet<ErrorPageRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletHandler> errorHandlers = errorsMapping.getMappedHandlers();
        for (ServletHandler servletHandler : errorHandlers)
        {
            errorPages.add(errorsMapping.getErrorPage(servletHandler));
        }

        Collection<FilterRuntime> filterRuntimes = new TreeSet<FilterRuntime>(FilterRuntime.COMPARATOR);
        for (FilterRuntime filterRuntime : filterMap.values())
        {
            filterRuntimes.add(filterRuntime);
        }

        Collection<ServletRuntime> servletRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletRuntime> resourceRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);

        for (Set<ServletHandler> patternHandlers : patternToServletHandler.values())
        {
            Iterator<ServletHandler> itr = patternHandlers.iterator();
            ServletHandler activeHandler = itr.next();
            if (activeHandler.getServletInfo().isResource())
            {
                resourceRuntimes.add(activeHandler);
            }
            else
            {
                servletRuntimes.add(activeHandler);
            }
            while (itr.hasNext())
            {
                failureRuntimeBuilder.add(itr.next().getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
            }
        }

        return new ContextRuntime(servletRuntimes, filterRuntimes, resourceRuntimes, errorPages, serviceId);
    }
}
