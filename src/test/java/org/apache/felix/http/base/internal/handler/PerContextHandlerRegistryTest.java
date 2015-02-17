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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Test for the ordering of servlet contexts
 */
public class PerContextHandlerRegistryTest
{

    @Test public void testPathOrdering()
    {
        final List<PerContextHandlerRegistry> list = new ArrayList<PerContextHandlerRegistry>();
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 1L, 0)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/foo", 2L, 0)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 3L, 0)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/bar", 4L, 0)));

        Collections.sort(list);

        assertEquals(2L, list.get(0).getContextServiceId());
        assertEquals(4L, list.get(1).getContextServiceId());
        assertEquals(1L, list.get(2).getContextServiceId());
        assertEquals(3L, list.get(3).getContextServiceId());
    }

    @Test public void testRankingOrdering()
    {
        final List<PerContextHandlerRegistry> list = new ArrayList<PerContextHandlerRegistry>();
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 1L, 0)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 2L, 0)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 3L, -30)));
        list.add(new PerContextHandlerRegistry(createServletContextHelperInfo("/", 4L, 50)));

        Collections.sort(list);

        assertEquals(4L, list.get(0).getContextServiceId());
        assertEquals(1L, list.get(1).getContextServiceId());
        assertEquals(2L, list.get(2).getContextServiceId());
        assertEquals(3L, list.get(3).getContextServiceId());
    }

    private ServletContextHelperInfo createServletContextHelperInfo(final String path,
            final long serviceId,
            final int ranking)
    {
        final ServiceReference<ServletContextHelper> ref = mock(ServiceReference.class);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(ranking);
        when(ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH)).thenReturn(path);
        when(ref.getPropertyKeys()).thenReturn(new String[0]);
        return new ServletContextHelperInfo(ref);
    }
}
