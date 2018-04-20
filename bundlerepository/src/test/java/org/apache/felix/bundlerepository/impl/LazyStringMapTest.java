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
package org.apache.felix.bundlerepository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import org.apache.felix.bundlerepository.impl.LazyStringMap.LazyValue;

public class LazyStringMapTest extends TestCase
{
    public void testLazyHashMap() {
        final AtomicInteger lv1Computed = new AtomicInteger(0);
        LazyValue<Long> lv1 = new LazyValue<Long>() {
            public Long compute() {
                lv1Computed.incrementAndGet();
                return 24L;
            }
        };

        final AtomicInteger lv2Computed = new AtomicInteger(0);
        LazyValue<Long> lv2 = new LazyValue<Long>() {
            public Long compute() {
                lv2Computed.incrementAndGet();
                return 0L;
            }
        };

        Collection<LazyValue<Long>> lazyValues = new ArrayList<LazyValue<Long>>();
        lazyValues.add(lv1);
        lazyValues.add(lv2);
        LazyStringMap<Long> lhm = new LazyStringMap<Long>();
        lhm.put("1", 2L);
        lhm.putLazy("42", lv1);
        lhm.putLazy("zero", lv2);

        assertEquals(new Long(2L), lhm.get("1"));
        assertEquals("No computation should have happened yet", 0, lv1Computed.get());
        assertEquals("No computation should have happened yet", 0, lv2Computed.get());

        assertEquals(new Long(24L), lhm.get("42"));
        assertEquals("lv1 should have been computed", 1, lv1Computed.get());
        assertEquals("No computation should have happened yet for lv2", 0, lv2Computed.get());

        lhm.put("zero", -1L);
        assertEquals(new Long(-1L), lhm.get("zero"));
        assertEquals("lv1 should have been computed", 1, lv1Computed.get());
        assertEquals("No computation should have happened for lv2, as we put a value in for it",
                0, lv2Computed.get());
    }
}
