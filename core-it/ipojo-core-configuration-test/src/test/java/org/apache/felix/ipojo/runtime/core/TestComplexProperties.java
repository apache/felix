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

package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestComplexProperties extends Common {


    private ServiceReference m_ref;
    private CheckService m_check;

    @Before
    public void setUp() {
        m_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "complex");
        assertNotNull("Complex service availability", m_ref);
        m_check = (CheckService) osgiHelper.getRawServiceObject(m_ref);
    }

    @After
    public void tearDown() {
        m_check = null;

    }

    @Test
    public void testArray() {
        String[] array = (String[]) m_check.getProps().get("array");
        assertEquals("Array size", 2, array.length);
        assertEquals("Array[0]", "a", array[0]);
        assertEquals("Array[1]", "b", array[1]);
    }

    @Test
    public void testList() {
        List list = (List) m_check.getProps().get("list");
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
    }

    @Test
    public void testMap() {
        Map map = (Map) m_check.getProps().get("map");
        assertEquals("Map size", 2, map.size());
        assertEquals("Map[a]", "a", map.get("a"));
        assertEquals("Map[b]", "b", map.get("b"));
    }

    @Test
    public void testDictionary() {
        Dictionary dict = (Dictionary) m_check.getProps().get("dict");
        assertEquals("Map size", 2, dict.size());
        assertEquals("Map[a]", "a", dict.get("a"));
        assertEquals("Map[b]", "b", dict.get("b"));
    }

    @Test
    public void testComplexArray() {
        Object[] array = (Object[]) m_check.getProps().get("complex-array");
        assertEquals("Array size", 2, array.length);
        assertTrue("Array[0] type", array[0] instanceof List);
        assertTrue("Array[1] type", array[1] instanceof List);
        List list = (List) array[0];
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
        list = (List) array[1];
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }

    @Test
    public void testComplexList() {
        List list = (List) m_check.getProps().get("complex-list");
        assertEquals("List size", 2, list.size());
        assertTrue("List[0] type", list.get(0) instanceof List);
        assertTrue("List[1] type", list.get(1) instanceof List);
        List list1 = (List) list.get(0);
        assertEquals("List size - 1", 2, list1.size());
        assertEquals("List[0] - 1", "a", list1.get(0));
        assertEquals("List[1] - 1", "b", list1.get(1));
        list1 = (List) list.get(1);
        assertEquals("List size - 2", 2, list1.size());
        assertEquals("List[0] - 2", "c", list1.get(0));
        assertEquals("List[1] - 2", "d", list1.get(1));
    }

    @Test
    public void testComplexMap() {
        Map map = (Map) m_check.getProps().get("complex-map");
        assertEquals("List size", 2, map.size());
        assertTrue("List[0] type", map.get("a") instanceof List);
        assertTrue("List[1] type", map.get("b") instanceof List);
        List list = (List) map.get("a");
        assertEquals("List size - 1", 2, list.size());
        assertEquals("List[0] - 1", "a", list.get(0));
        assertEquals("List[1] - 1", "b", list.get(1));
        list = (List) map.get("b");
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }

    @Test
    public void testServiceArray() {
        String[] array = (String[]) m_ref.getProperty("array");
        assertEquals("Array size", 2, array.length);
        assertEquals("Array[0]", "a", array[0]);
        assertEquals("Array[1]", "b", array[1]);
    }

    @Test
    public void testServiceList() {
        List list = (List) m_ref.getProperty("list");
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
    }

    @Test
    public void testServiceMap() {
        Map map = (Map) m_ref.getProperty("map");
        assertEquals("Map size", 2, map.size());
        assertEquals("Map[a]", "a", map.get("a"));
        assertEquals("Map[b]", "b", map.get("b"));
    }

    @Test
    public void testServiceDictionary() {
        Dictionary dict = (Dictionary) m_ref.getProperty("dict");
        assertEquals("Map size", 2, dict.size());
        assertEquals("Map[a]", "a", dict.get("a"));
        assertEquals("Map[b]", "b", dict.get("b"));
    }

    @Test
    public void testServiceComplexArray() {
        Object[] array = (Object[]) m_ref.getProperty("complex-array");
        assertEquals("Array size", 2, array.length);
        assertTrue("Array[0] type", array[0] instanceof List);
        assertTrue("Array[1] type", array[1] instanceof List);
        List list = (List) array[0];
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
        list = (List) array[1];
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }

    @Test
    public void testServiceComplexList() {
        List list = (List) m_ref.getProperty("complex-list");
        assertEquals("List size", 2, list.size());
        assertTrue("List[0] type", list.get(0) instanceof List);
        assertTrue("List[1] type", list.get(1) instanceof List);
        List list1 = (List) list.get(0);
        assertEquals("List size - 1", 2, list1.size());
        assertEquals("List[0] - 1", "a", list1.get(0));
        assertEquals("List[1] - 1", "b", list1.get(1));
        list1 = (List) list.get(1);
        assertEquals("List size - 2", 2, list1.size());
        assertEquals("List[0] - 2", "c", list1.get(0));
        assertEquals("List[1] - 2", "d", list1.get(1));
    }

    @Test
    public void testServiceComplexMap() {
        Map map = (Map) m_ref.getProperty("complex-map");
        assertEquals("List size", 2, map.size());
        assertTrue("List[0] type", map.get("a") instanceof List);
        assertTrue("List[1] type", map.get("b") instanceof List);
        List list = (List) map.get("a");
        assertEquals("List size - 1", 2, list.size());
        assertEquals("List[0] - 1", "a", list.get(0));
        assertEquals("List[1] - 1", "b", list.get(1));
        list = (List) map.get("b");
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }

    @Test
    public void testServiceEmptyArray() {
        String[] array = (String[]) m_ref.getProperty("empty-array");
        assertEquals("Array size", 0, array.length);
    }

    @Test
    public void testServiceEmptyList() {
        List list = (List) m_ref.getProperty("empty-list");
        assertEquals("List size", 0, list.size());
    }

    @Test
    public void testServiceEmptyMap() {
        Map map = (Map) m_ref.getProperty("empty-map");
        assertEquals("Map size", 0, map.size());
    }

}
