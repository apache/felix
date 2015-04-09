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
import java.util.TreeSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.HandlerRankingMultimap.Update;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ErrorPageRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FilterRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    private final Map<Filter, FilterHandler> filterMap = new HashMap<Filter, FilterHandler>();

    private volatile HandlerMapping<FilterHandler> filterMapping = new HandlerMapping<FilterHandler>();
    private volatile ErrorsMapping errorsMapping = new ErrorsMapping();

    private final HandlerRankingMultimap<String> registeredErrorPages = new HandlerRankingMultimap<String>();

    private final long serviceId;

    private final int ranking;

    private final String path;

    private final String prefix;

    public PerContextHandlerRegistry()
    {
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
            this.prefix = null;
        }
        else
        {
            this.prefix = this.path + "/";
        }
    }

    @Override
    public int compareTo(final PerContextHandlerRegistry other)
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

    public synchronized void addFilter(FilterHandler handler) throws RegistrationFailureException
    {
        if (this.filterMapping.contains(handler))
        {
            throw new RegistrationFailureException(handler.getFilterInfo(), FAILURE_REASON_SERVICE_ALREAY_USED,
                "Filter instance " + handler.getName() + " already registered");
        }

        try
        {
            handler.init();
        }
        catch (ServletException e)
        {
            throw new RegistrationFailureException(handler.getFilterInfo(), FAILURE_REASON_EXCEPTION_ON_INIT, e);
        }

        this.filterMapping = this.filterMapping.add(handler);
        this.filterMap.put(handler.getFilter(), handler);
    }


    void addErrorPage(ServletHandler handler, String[] errorPages) throws RegistrationFailureException
    {
        Update<String> update = this.registeredErrorPages.add(errorPages, handler);
        initHandlers(update.getInit());
        this.errorsMapping = this.errorsMapping.update(update.getActivated(), update.getDeactivated());
        destroyHandlers(update.getDestroy());
    }

    public synchronized void removeAll()
    {
        Collection<ServletHandler> errorPageHandlers = this.errorsMapping.values();
        Collection<FilterHandler> filterHandlers = this.filterMapping.values();

        this.filterMapping = new HandlerMapping<FilterHandler>();
        this.errorsMapping = new ErrorsMapping();

        destroyHandlers(filterHandlers);
        destroyHandlers(errorPageHandlers);

        this.filterMap.clear();
        this.registeredErrorPages.clear();
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
        for(final FilterHandler handler : this.filterMap.values())
        {
            if (handler.getFilterInfo().compareTo(filterInfo) == 0)
            {
                return handler;
            }
        }
        return null;
    }

    void removeErrorPage(ServletInfo info) throws RegistrationFailureException
    {
        ServletHandler handler = getServletHandler(info);
        if (handler == null)
        {
            return;
        }
        String[] errorPages = handler.getServletInfo().getErrorPage();
        Update<String> update = this.registeredErrorPages.remove(errorPages, handler);
        initHandlers(update.getInit());
        this.errorsMapping = this.errorsMapping.update(update.getActivated(), update.getDeactivated());
        destroyHandlers(update.getDestroy());
    }

    private ServletHandler getServletHandler(final ServletInfo servletInfo)
    {
        ServletHandler servletHandler = getServletHandler(servletInfo, this.registeredErrorPages.getActiveValues());
        if (servletHandler == null)
        {
            return getServletHandler(servletInfo, this.registeredErrorPages.getShadowedValues());
        }
        return servletHandler;
    }

    private ServletHandler getServletHandler(final ServletInfo servletInfo, Collection<ServletHandler> values)
    {
        Iterator<ServletHandler> it = values.iterator();
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

    public ErrorsMapping getErrorsMapping()
    {
        return this.errorsMapping;
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

    public synchronized ContextRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder) {
        Collection<FilterRuntime> filterRuntimes = new TreeSet<FilterRuntime>(FilterRuntime.COMPARATOR);
        for (FilterRuntime filterRuntime : this.filterMap.values())
        {
            filterRuntimes.add(filterRuntime);
        }

        Collection<ErrorPageRuntime> errorPages = new TreeSet<ErrorPageRuntime>(ServletRuntime.COMPARATOR);
        for (ServletHandler servletHandler : this.registeredErrorPages.getActiveValues())
        {
            errorPages.add(ErrorPageRuntime.fromServletRuntime(servletHandler));
        }

        addShadowedHandlers(failureRuntimeBuilder, this.registeredErrorPages.getShadowedValues());

        return new ContextRuntime(filterRuntimes,
                errorPages,
                this.serviceId);
    }

    private void addShadowedHandlers(FailureRuntime.Builder failureRuntimeBuilder, Collection<ServletHandler> handlers)
    {
        for (ServletHandler handler : handlers)
        {
            failureRuntimeBuilder.add(handler.getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        }
    }
}
