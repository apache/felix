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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.junit.Test;

/**
 * Test for the ordering of servlet contexts
 */
public class PerContextHandlerRegistryTest
{

    @Test
    public void testPathOrdering()
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

    @Test
    public void testRankingOrdering()
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

    @Test
    public void testOrderingSymetry()
    {
        testSymetry("/", "/foo", 1L, 2L, 0, 0);
        testSymetry("/", "/", 1L, 2L, 0, 10);
        testSymetry("/", "/", 1L, 2L, 0, 0);
        testSymetry("/", "/", 1L, -2L, 0, 0);
        testSymetry("/", "/", -1L, -2L, 0, 0);
        testSymetry("/", "/", 0L, -1L, 0, 0);
        testSymetry("/", "/", 0L, 1L, 0, 0);
    }

    private void testSymetry(String path, String otherPath, long id, long otherId, int ranking, int otherRanking)
    {
        PerContextHandlerRegistry handlerRegistry = new PerContextHandlerRegistry(createServletContextHelperInfo(path, id, ranking));
        PerContextHandlerRegistry other = new PerContextHandlerRegistry(createServletContextHelperInfo(otherPath, otherId, otherRanking));

        assertEquals(handlerRegistry.compareTo(other), -other.compareTo(handlerRegistry));
    }

    @Test
    public void testOrderingTransitivity()
    {
        testTransitivity("/", "/foo", "/barrr", 1L, 2L, 3L, 0, 0, 0);
        testTransitivity("/", "/", "/", 1L, 2L, 3L, 1, 2, 3);
        testTransitivity("/", "/", "/", 2L, 1L, 0L, 1, 2, 3);
        testTransitivity("/", "/", "/", 2L, 1L, 0L, 0, 0, 0);
        testTransitivity("/", "/", "/", 1L, -1L, 0L, 0, 0, 0);
        testTransitivity("/", "/", "/", -2L, -1L, 0L, 0, 0, 0);
    }

    private void testTransitivity(String highPath, String midPath, String lowPath, long highId, long midId, long lowId, int highRanking, int midRanking, int lowRanking)
    {
        PerContextHandlerRegistry high = new PerContextHandlerRegistry(createServletContextHelperInfo(highPath, highId, highRanking));
        PerContextHandlerRegistry mid = new PerContextHandlerRegistry(createServletContextHelperInfo(midPath, midId, midRanking));
        PerContextHandlerRegistry low = new PerContextHandlerRegistry(createServletContextHelperInfo(lowPath, lowId, lowRanking));

        assertTrue(high.compareTo(mid) > 0);
        assertTrue(mid.compareTo(low) > 0);
        assertTrue(high.compareTo(low) > 0);
    }

    private ServletContextHelperInfo createServletContextHelperInfo(final String path, final long serviceId, final int ranking)
    {
        return new ServletContextHelperInfo(ranking, serviceId, "", path, null);
    }
}
