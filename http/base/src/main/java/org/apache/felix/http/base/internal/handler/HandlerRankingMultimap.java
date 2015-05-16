/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.handler;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public final class HandlerRankingMultimap<K>
{
    private final Map<ServletHandler, Integer> useCounts = new TreeMap<ServletHandler, Integer>();

    private final Map<K, PriorityQueue<ServletHandler>> handlerMultimap;
    private final Comparator<ServletHandler> handlerComparator;
    private final Comparator<K> keyComparator;

    private int size = 0;

    public HandlerRankingMultimap()
    {
        this.handlerMultimap = new HashMap<K, PriorityQueue<ServletHandler>>();
        this.keyComparator = null;
        this.handlerComparator = null;
    }

    public HandlerRankingMultimap(Comparator<K> keyComparator)
    {
        this(keyComparator, null);
    }

    public HandlerRankingMultimap(Comparator<K> keyComparator, Comparator<ServletHandler> handlerComparator)
    {
        this.keyComparator = keyComparator;
        this.handlerMultimap = new TreeMap<K, PriorityQueue<ServletHandler>>(keyComparator);
        this.handlerComparator = handlerComparator;
    }

    boolean isActive(ServletHandler handler)
    {
        return useCounts.containsKey(handler);
    }

    public Update<K> add(K[] keys, ServletHandler handler)
    {
        return add(asList(keys), handler);
    }

    Update<K> add(Collection<K> keys, ServletHandler handler)
    {
        if (handler == null)
        {
            throw new NullPointerException("handler must not be null");
        }
        Map<K, ServletHandler> activate = createMap();
        Map<K, ServletHandler> deactivate = createMap();
        Set<ServletHandler> destroy = new TreeSet<ServletHandler>();
        for (K key : keys)
        {
            PriorityQueue<ServletHandler> queue = getQueue(key);

            if (queue.isEmpty() || compareHandlers(queue.peek(), handler) > 0)
            {
                activateEntry(key, handler, activate, null);

                if (!queue.isEmpty())
                {
                    ServletHandler currentHead = queue.peek();
                    deactivateEntry(key, currentHead, deactivate, destroy);
                }
            }

            queue.add(handler);
        }

        size += 1;

        return Update.forAdd(activate, deactivate, destroy);
    }

    public Update<K> remove(K[] keys, ServletHandler handler)
    {
        return remove(asList(keys), handler);
    }

    Update<K> remove(Collection<K> keys, ServletHandler handler)
    {
        Map<K, ServletHandler> activate = createMap();
        Map<K, ServletHandler> deactivate = createMap();
        Set<ServletHandler> init = new TreeSet<ServletHandler>();
        for (K key : keys)
        {
            PriorityQueue<ServletHandler> queue = handlerMultimap.get(key);
            if (queue == null)
            {
                throw new IllegalArgumentException("Map does not contain key: " + key);
            }

            boolean isDeactivate = !queue.isEmpty() && compareHandlers(queue.peek(), handler) == 0;
            queue.remove(handler);

            if (isDeactivate)
            {
                deactivateEntry(key, handler, deactivate, null);

                if (!queue.isEmpty())
                {
                    ServletHandler newHead = queue.peek();
                    activateEntry(key, newHead, activate, init);
                }
            }

            if (queue.isEmpty())
            {
                handlerMultimap.remove(key);
            }
        }

        size -= 1;

        return Update.forRemove(activate, deactivate, init);
    }

    private PriorityQueue<ServletHandler> getQueue(K key)
    {
        PriorityQueue<ServletHandler> queue = handlerMultimap.get(key);
        if (queue == null)
        {
            queue = new PriorityQueue<ServletHandler>(1, handlerComparator);
            handlerMultimap.put(key, queue);
        }
        return queue;
    }

    private void activateEntry(K key, ServletHandler handler, Map<K, ServletHandler> activate, Set<ServletHandler> init)
    {
        activate.put(key, handler);
        if (incrementUseCount(handler) == 1 && init != null)
        {
            init.add(handler);
        };
    }

    private void deactivateEntry(K key, ServletHandler handler, Map<K, ServletHandler> deactivate, Set<ServletHandler> destroy)
    {
        deactivate.put(key, handler);
        if (decrementUseCount(handler) == 0 && destroy != null)
        {
            destroy.add(handler);
        }
    }

    private int incrementUseCount(ServletHandler handler)
    {
        Integer currentCount = useCounts.get(handler);
        Integer newCount = currentCount == null ? 1 : currentCount + 1;

        useCounts.put(handler, newCount);

        return newCount;
    }

    private int decrementUseCount(ServletHandler handler)
    {
        int currentCount = useCounts.get(handler);
        if (currentCount == 1)
        {
            useCounts.remove(handler);
            return 0;
        }

        int newCount = currentCount - 1;
        useCounts.put(handler, newCount);
        return newCount;
    }

    public void clear()
    {
        handlerMultimap.clear();
        useCounts.clear();
    }

    public Collection<ServletHandler> getActiveValues()
    {
        TreeSet<ServletHandler> activeValues = new TreeSet<ServletHandler>();
        for (PriorityQueue<ServletHandler> queue : handlerMultimap.values())
        {
            activeValues.add(queue.peek());
        }
        return activeValues;
    }

    public Collection<ServletHandler> getShadowedValues()
    {
        TreeSet<ServletHandler> shadowedValues = new TreeSet<ServletHandler>();
        for (PriorityQueue<ServletHandler> queue : handlerMultimap.values())
        {
            ServletHandler head = queue.element();
            for (ServletHandler value : queue)
            {
                if (compareHandlers(value, head) != 0)
                {
                    shadowedValues.add(value);
                }
            }
        }
        return shadowedValues;
    }

    int size()
    {
        return size;
    }

    private int compareHandlers(ServletHandler one, ServletHandler two)
    {
        if (handlerComparator == null)
        {
            return one.compareTo(two);
        }
        return handlerComparator.compare(one, two);
    }

    private Map<K,ServletHandler> createMap()
    {
        return keyComparator == null ? new HashMap<K, ServletHandler>() : new TreeMap<K, ServletHandler>(keyComparator);
    }

    public static final class Update<K>
    {
        private final Map<K, ServletHandler> activate;
        private final Map<K, ServletHandler> deactivate;
        private final Collection<ServletHandler> init;
        private final Collection<ServletHandler> destroy;

        Update(Map<K, ServletHandler> activate,
                Map<K, ServletHandler> deactivate,
                Collection<ServletHandler> init,
                Collection<ServletHandler> destroy)
        {
            this.activate = activate;
            this.deactivate = deactivate;
            this.init = init;
            this.destroy = destroy;
        }

        private static <K> Update<K> forAdd(Map<K, ServletHandler> activate,
                Map<K, ServletHandler> deactivate,
                Collection<ServletHandler> destroy)
        {
            // activate contains at most one value, mapped to multiple keys
            Collection<ServletHandler> init = valueAsCollection(activate);
            return new Update<K>(activate, deactivate, init, destroy);
        }

        private static <K> Update<K> forRemove(Map<K, ServletHandler> activate,
                Map<K, ServletHandler> deactivate,
                Collection<ServletHandler> init)
        {
            // deactivate contains at most one value, mapped to multiple keys
            Collection<ServletHandler> destroy = valueAsCollection(deactivate);
            return new Update<K>(activate, deactivate, init, destroy);
        }

        private static <K> Collection<ServletHandler> valueAsCollection(Map<K, ServletHandler> valueMap)
        {
            if (valueMap.isEmpty())
            {
                return Collections.emptyList();
            }

            Collection<ServletHandler> valueSet = new ArrayList<ServletHandler>(1);
            valueSet.add(valueMap.values().iterator().next());
            return valueSet;
        }

        public Map<K, ServletHandler> getActivated()
        {
            return activate;
        }

        public Map<K, ServletHandler> getDeactivated()
        {
            return deactivate;
        }

        public Collection<ServletHandler> getInit()
        {
            return init;
        }

        public Collection<ServletHandler> getDestroy()
        {
            return destroy;
        }
    }
}
