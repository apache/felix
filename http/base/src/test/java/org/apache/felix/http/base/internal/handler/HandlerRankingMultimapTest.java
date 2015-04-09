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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.HandlerRankingMultimap.Update;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class HandlerRankingMultimapTest
{
    private HandlerRankingMultimap<String> handlerMultimap;

    @Before
    public void setup()
    {
        handlerMultimap = new HandlerRankingMultimap<String>();
    }

    @Test
    public void addedInfoIsUsed() throws ServletException
    {
        TestHandler<String> handler = TestHandler.create(asList("a"), 0);
        Update<String> update = handlerMultimap.add(handler.getKeys(), handler);

        assertEquals(1, handlerMultimap.size());
        assertTrue(handlerMultimap.isActive(handler));

        assertEquals(1, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", handler));
        assertTrue(update.getDeactivated().isEmpty());

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(handler));
        assertTrue(update.getDestroy().isEmpty());
    }

    @Test
    public void highestPriorityServiceIsUsed() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a"), 0);

        handlerMultimap.add(lower.getKeys(), lower);

        Update<String> updateAddingHigher = handlerMultimap.add(higher.getKeys(), higher);

        assertTrue(handlerMultimap.isActive(higher));
        assertFalse(handlerMultimap.isActive(lower));

        assertEquals(1, updateAddingHigher.getActivated().size());
        assertThat(updateAddingHigher.getActivated(), hasEntry("a", higher));
        assertEquals(1, updateAddingHigher.getDeactivated().size());
        assertThat(updateAddingHigher.getDeactivated(), hasEntry("a", lower));

        assertEquals(1, updateAddingHigher.getInit().size());
        assertThat(updateAddingHigher.getInit(), contains(higher));
        assertEquals(1, updateAddingHigher.getDestroy().size());
        assertThat(updateAddingHigher.getDestroy(), contains(lower));
    }

    @Test
    public void removeHighestPriorityService() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a"), 0);

        handlerMultimap.add(lower.getKeys(), lower);
        handlerMultimap.add(higher.getKeys(), higher);

        Update<String> update = handlerMultimap.remove(higher.getKeys(), higher);

        assertFalse(handlerMultimap.isActive(higher));
        assertTrue(handlerMultimap.isActive(lower));

        assertEquals(1, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", lower));
        assertEquals(1, update.getDeactivated().size());
        assertThat(update.getDeactivated(), hasEntry("a", higher));

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(lower));
        assertEquals(1, update.getDestroy().size());
        assertThat(update.getDestroy(), contains(higher));
    }

    @Test
    public void removeLowerPriorityService() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a"), 0);

        handlerMultimap.add(lower.getKeys(), higher);
        handlerMultimap.add(higher.getKeys(), lower);

        Update<String> update = handlerMultimap.remove(lower.getKeys(), lower);

        assertTrue(handlerMultimap.isActive(higher));
        assertFalse(handlerMultimap.isActive(lower));

        assertTrue(update.getActivated().isEmpty());
        assertTrue(update.getDeactivated().isEmpty());

        assertTrue(update.getInit().isEmpty());
        assertTrue(update.getDestroy().isEmpty());
    }

    @Test
    public void addServiceWithMultipleKeys() throws ServletException
    {
        TestHandler<String> handler = TestHandler.create(asList("a", "b"), 0);

        Update<String> update = handlerMultimap.add(handler.getKeys(), handler);

        assertTrue(handlerMultimap.isActive(handler));

        assertEquals(2, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", handler));
        assertThat(update.getActivated(), hasEntry("b", handler));
        assertTrue(update.getDeactivated().isEmpty());

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(handler));
    }

    @Test
    public void addServiceWithMultipleKeysShadowsAllKeys() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a", "b"), 0);

        handlerMultimap.add(lower.getKeys(), lower);

        Update<String> updateWithHigher = handlerMultimap.add(higher.getKeys(), higher);

        assertTrue(handlerMultimap.isActive(higher));
        assertFalse(handlerMultimap.isActive(lower));

        assertEquals(3, updateWithHigher.getActivated().size());
        assertThat(updateWithHigher.getActivated(), hasEntry("a", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("b", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("c", higher));
        assertEquals(2, updateWithHigher.getDeactivated().size());
        assertThat(updateWithHigher.getDeactivated(), hasEntry("a", lower));
        assertThat(updateWithHigher.getDeactivated(), hasEntry("b", lower));

        assertEquals(1, updateWithHigher.getInit().size());
        assertThat(updateWithHigher.getInit(), contains(higher));
        assertEquals(1, updateWithHigher.getDestroy().size());
        assertThat(updateWithHigher.getDestroy(), contains(lower));
    }

    @Test
    public void addServiceWithMultipleKeysShadowsPartially() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a", "c"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a", "b"), 0);

        handlerMultimap.add(lower.getKeys(), lower);

        Update<String> updateWithHigher = handlerMultimap.add(higher.getKeys(), higher);

        assertTrue(handlerMultimap.isActive(higher));
        assertTrue(handlerMultimap.isActive(lower));

        assertEquals(2, updateWithHigher.getActivated().size());
        assertThat(updateWithHigher.getActivated(), hasEntry("a", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("c", higher));
        assertEquals(1, updateWithHigher.getDeactivated().size());
        assertThat(updateWithHigher.getDeactivated(), hasEntry("a", lower));

        assertEquals(1, updateWithHigher.getInit().size());
        assertThat(updateWithHigher.getInit(), contains(higher));
        assertTrue(updateWithHigher.getDestroy().isEmpty());
    }

    @Test
    public void addServiceWithMultipleKeysIsCompletelyShadowed() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a", "b"), 0);

        handlerMultimap.add(higher.getKeys(), higher);

        Update<String> updateWithLower = handlerMultimap.add(lower.getKeys(), lower);

        assertTrue(handlerMultimap.isActive(higher));
        assertFalse(handlerMultimap.isActive(lower));

        assertTrue(updateWithLower.getActivated().isEmpty());
        assertTrue(updateWithLower.getDeactivated().isEmpty());

        assertTrue(updateWithLower.getInit().isEmpty());
        assertTrue(updateWithLower.getDestroy().isEmpty());
    }

    @Test
    public void sizeReturnsAllEntries() throws ServletException
    {
        TestHandler<String> higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler<String> lower = TestHandler.create(asList("a", "b"), 0);
        TestHandler<String> third = TestHandler.create(asList("d"), 3);

        assertEquals(0 , handlerMultimap.size());

        handlerMultimap.add(lower.getKeys(), lower);

        assertEquals(1, handlerMultimap.size());

        handlerMultimap.add(higher.getKeys(), higher);

        assertEquals(2, handlerMultimap.size());

        handlerMultimap.add(third.getKeys(), third);

        assertEquals(3, handlerMultimap.size());

        handlerMultimap.remove(lower.getKeys(), lower);

        assertEquals(2, handlerMultimap.size());

        handlerMultimap.remove(higher.getKeys(), higher);

        assertEquals(1, handlerMultimap.size());

        handlerMultimap.remove(third.getKeys(), third);

        assertEquals(0, handlerMultimap.size());
    }

    public void getActiveValuesContainsAllHeads()
    {
        TestHandler<String> one = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler<String> two = TestHandler.create(asList("a", "b"), 0);
        TestHandler<String> three = TestHandler.create(asList("c, e"), 3);
        TestHandler<String> four = TestHandler.create(asList("d"), 3);

        handlerMultimap.add(one.getKeys(), one);
        handlerMultimap.add(two.getKeys(), two);
        handlerMultimap.add(three.getKeys(), three);
        handlerMultimap.add(four.getKeys(), four);

        assertEquals(3, handlerMultimap.getActiveValues());
        assertTrue(handlerMultimap.getActiveValues().contains(one));
        assertTrue(handlerMultimap.getActiveValues().contains(three));
        assertTrue(handlerMultimap.getActiveValues().contains(four));
    }

    public void getShadowedValuesContainsAllTails()
    {
        TestHandler<String> one = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler<String> two = TestHandler.create(asList("a", "b"), 0);
        TestHandler<String> three = TestHandler.create(asList("a"), -1);
        TestHandler<String> four = TestHandler.create(asList("c, e"), 3);
        TestHandler<String> five = TestHandler.create(asList("d"), 3);

        handlerMultimap.add(one.getKeys(), one);
        handlerMultimap.add(two.getKeys(), two);
        handlerMultimap.add(three.getKeys(), three);
        handlerMultimap.add(four.getKeys(), four);
        handlerMultimap.add(five.getKeys(), five);

        assertEquals(3, handlerMultimap.getActiveValues());
        assertTrue(handlerMultimap.getActiveValues().contains(one));
        assertTrue(handlerMultimap.getActiveValues().contains(two));
        assertTrue(handlerMultimap.getActiveValues().contains(three));
    }

    public void keyComparatorIsUsed()
    {
        final Object keyOne = new Object();
        final Object keyTwo = new Object();

        Comparator<Object> keyComparator = new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                if (o1 == o2)
                {
                    return 0;
                }
                if (o1 == keyOne && o2 == keyTwo)
                {
                    return 1;
                }
                if (o1 == keyTwo && o2 == keyOne)
                {
                    return -1;
                }
                throw new IllegalArgumentException();
            }
        };

        HandlerRankingMultimap<Object> multimap = new HandlerRankingMultimap<Object>(keyComparator);

        TestHandler<Object> handlerOne = TestHandler.create(asList(keyOne), 1);
        TestHandler<Object> handlerTwo = TestHandler.create(asList(keyOne, keyTwo), 0);

        multimap.add(handlerOne.getKeys(), handlerOne);
        multimap.add(handlerTwo.getKeys(), handlerTwo);

        assertEquals(2, multimap.size());
        assertEquals(2, multimap.getActiveValues().size());
        assertTrue(multimap.getActiveValues().contains(keyOne));
        assertTrue(multimap.getActiveValues().contains(keyTwo));
        assertEquals(1, multimap.getShadowedValues().size());
        assertTrue(multimap.getShadowedValues().contains(keyTwo));
    }

    private static Matcher<Map<? extends String, ? extends ServletHandler>> hasEntry(String key, ServletHandler servletHandler)
    {
        return Matchers.hasEntry(key, servletHandler);
    }

    private static Matcher<Iterable<? extends ServletHandler>> contains(ServletHandler servletHandler)
    {
        return Matchers.contains(servletHandler);
    }

    private static abstract class TestHandler<T> extends ServletHandler
    {
        static int idCount = 0;

        TestHandler(List<T> keys, int ranking)
        {
            super(0L, null, null);
        }

        static <T> TestHandler<T> create(List<T> keys, int ranking)
        {
            TestHandler<T> testHandler = mock(TestHandler.class);
            when(testHandler.getId()).thenReturn(++idCount);
            when(testHandler.getRanking()).thenReturn(ranking);
            when(testHandler.getKeys()).thenReturn(keys);
            when(testHandler.compareTo(any(TestHandler.class))).thenCallRealMethod();
            return testHandler;
        }

        @Override
        public int compareTo(ServletHandler o)
        {
            TestHandler<T> other = (TestHandler<T>) o;
            int rankCompare = Integer.compare(other.getRanking(), getRanking());
            return rankCompare != 0 ? rankCompare : Integer.compare(getId(), other.getId());
        }

        abstract int getRanking();

        abstract int getId();

        abstract List<T> getKeys();
    }
}
