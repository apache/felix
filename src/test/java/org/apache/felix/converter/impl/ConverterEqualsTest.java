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
package org.apache.felix.converter.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConverterEqualsTest {
    @Test
    public void testEquals() {
        Converter c = new StandardConverter();

        Dictionary<String, Object> o1 = new Hashtable<>();
        o1.put("foo", 12);

        Map<String, Object> o2 = new HashMap<>();
        o2.put("foo", "12");
        assertTrue(c.equals(o1, o2));

        o2.put("bar", true);
        assertFalse(c.equals(o1, o2));

        o1.put("bar", true);
        assertTrue(c.equals(o1, o2));

        Map<Object,Object> hm = new HashMap<>();
        o1.put("submap", hm);
        assertFalse(c.equals(o1, o2));

        Dictionary<String,Long> dm = new Hashtable<>();
        o2.put("submap", dm);
        assertTrue(c.equals(o1, o2));

        hm.put(Boolean.TRUE, '4');
        assertFalse(c.equals(o1, o2));

        dm.put("true", 4L);
        assertTrue(c.equals(o1, o2));
    }
}
