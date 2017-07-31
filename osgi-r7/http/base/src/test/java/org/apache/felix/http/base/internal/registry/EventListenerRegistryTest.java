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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceFilterHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class EventListenerRegistryTest {

    private final FilterRegistry reg = new FilterRegistry();

    private void assertEmpty(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        assertNull(dto.filterDTOs);
        assertTrue(holder.failedFilterDTOs.isEmpty());
    }

    private void clear(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        dto.filterDTOs = null;
        holder.failedFilterDTOs.clear();
    }

    @Test public void testSingleFilter() throws InvalidSyntaxException, ServletException
    {
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        // check DTO
        reg.getRuntimeInfo(dto, holder.failedFilterDTOs);
        assertEmpty(dto, holder);

        // register filter
        final FilterHandler h1 = createFilterHandler(1L, 0, "/foo");
        reg.addFilter(h1);

        verify(h1.getFilter()).init(Matchers.any(FilterConfig.class));

        // one entry in DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedFilterDTOs);
        assertTrue(holder.failedFilterDTOs.isEmpty());
        assertNotNull(dto.filterDTOs);
        assertEquals(1, dto.filterDTOs.length);
        assertEquals(1, dto.filterDTOs[0].patterns.length);
        assertEquals("/foo", dto.filterDTOs[0].patterns[0]);

        // remove filter
        final Filter f = h1.getFilter();
        reg.removeFilter(h1.getFilterInfo(), true);
        verify(f).destroy();

        // empty again
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedFilterDTOs);
        assertEmpty(dto, holder);
    }

    @Test public void testFilterOrdering() throws InvalidSyntaxException
    {
        final FilterHandler h1 = createFilterHandler(1L, 20, "/foo");
        reg.addFilter(h1);
        final FilterHandler h2 = createFilterHandler(2L, 10, "/foo");
        reg.addFilter(h2);
        final FilterHandler h3 = createFilterHandler(3L, 30, "/foo");
        reg.addFilter(h3);
        final FilterHandler h4 = createFilterHandler(4L, 0, "/other");
        reg.addFilter(h4);
        final FilterHandler h5 = createFilterHandler(5L, 90, "/foo");
        reg.addFilter(h5);

        final FilterHandler[] handlers = reg.getFilterHandlers(null, DispatcherType.REQUEST, "/foo");
        assertEquals(4, handlers.length);
        assertEquals(h5.getFilterInfo(), handlers[0].getFilterInfo());
        assertEquals(h3.getFilterInfo(), handlers[1].getFilterInfo());
        assertEquals(h1.getFilterInfo(), handlers[2].getFilterInfo());
        assertEquals(h2.getFilterInfo(), handlers[3].getFilterInfo());

        // cleanup
        reg.removeFilter(h1.getFilterInfo(), true);
        reg.removeFilter(h2.getFilterInfo(), true);
        reg.removeFilter(h3.getFilterInfo(), true);
        reg.removeFilter(h4.getFilterInfo(), true);
        reg.removeFilter(h5.getFilterInfo(), true);
    }

    private static FilterInfo createFilterInfo(final long id, final int ranking, final String... paths) throws InvalidSyntaxException
    {
        final BundleContext bCtx = mock(BundleContext.class);
        when(bCtx.createFilter(Matchers.anyString())).thenReturn(null);
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(bCtx);

        final ServiceReference<Filter> ref = mock(ServiceReference.class);
        when(ref.getBundle()).thenReturn(bundle);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(id);
        when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(ranking);
        when(ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN)).thenReturn(paths);
        when(ref.getPropertyKeys()).thenReturn(new String[0]);
        final FilterInfo si = new FilterInfo(ref);

        return si;
    }

    private static FilterHandler createFilterHandler(final long id, final int ranking, final String... paths) throws InvalidSyntaxException
    {
        final FilterInfo si = createFilterInfo(id, ranking, paths);
        final ExtServletContext ctx = mock(ExtServletContext.class);
        final Filter filter = mock(Filter.class);

        return new HttpServiceFilterHandler(ctx, si, filter);
    }
}
