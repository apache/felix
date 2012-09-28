/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl.role;

import java.util.Date;

import org.apache.felix.useradmin.impl.role.ObservableDictionary;
import org.apache.felix.useradmin.impl.role.ObservableDictionary.DictionaryChangeListener;

import junit.framework.TestCase;


/**
 * Test case for {@link ObservableDictionary}.
 */
public class ObservableDictionaryTest extends TestCase {
    
    /**
     * Implementation of {@link DictionaryChangeListener} that keeps track of 
     * which callback methods are being called at least once.
     */
    static class StateCheckingDictionaryChangeListener implements DictionaryChangeListener {
        
        volatile boolean m_addedCalled = false;
        volatile boolean m_removedCalled = false;
        volatile boolean m_changedCalled = false;

        public void entryAdded(Object key, Object value) {
            m_addedCalled = true;
        }

        public void entryChanged(Object key, Object oldValue, Object newValue) {
            m_changedCalled = true;
        }

        public void entryRemoved(Object key) {
            m_removedCalled = true;
        }
    }
    
    private ObservableDictionary m_dict;
    private StateCheckingDictionaryChangeListener m_listener;

    /**
     * Tests that adding an entry emits a change event.
     */
    public void testAddEntryEmitsEvent() {
        m_dict.setDictionaryChangeListener(m_listener);
        
        m_dict.put("key", "value");
        
        assertTrue(m_listener.m_addedCalled);
        assertFalse(m_listener.m_changedCalled);
        assertFalse(m_listener.m_removedCalled);
    }

    /**
     * Tests that changing an entry emits a change event.
     */
    public void testChangeEntryEmitsEvent() {
        m_dict.put("key", "value1");
        
        m_dict.setDictionaryChangeListener(m_listener);
        
        m_dict.put("key", "value2");
        
        assertTrue(m_listener.m_changedCalled);
        assertFalse(m_listener.m_addedCalled);
        assertFalse(m_listener.m_removedCalled);
    }

    /**
     * Tests that creating a new {@link ObservableDictionary} with a valid dictionary succeeds. 
     */
    public void testCreateWithNonNullDictionaryFail() {
        m_dict.put("foo", "bar");
        m_dict.put("bar", "foo");
        
        m_dict = new ObservableDictionary("foo", "bar", m_dict);
        
        assertEquals("bar", m_dict.get("foo"));
        assertEquals("foo", m_dict.get("bar"));
    }

    /**
     * Tests that creating a new {@link ObservableDictionary} with a <code>null</code> dictionary fails. 
     */
    public void testCreateWithNullDictionaryFail() {
        try {
            new ObservableDictionary("foo", "bar", null);
            fail("Expected IllegalArgumentException!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Tests that we can get any kind of object from an {@link ObservableDictionary}.
     */
    public void testGetAnyKindOfObject() {
        assertNull(m_dict.get("non-existing"));
        
        Integer key = Integer.valueOf(3);
        Date value = new Date();

        m_dict.put(key, value);
        assertEquals(value, m_dict.get(key));
    }

    /**
     * Tests that we cannot get a null-key form a {@link ObservableDictionary}.
     */
    public void testGetNullKeyFail() {
        try {
            m_dict.get(null);
            
            fail();
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Test that we can put any kind of object in a {@link ObservableDictionary}.
     */
    public void testPutAnyKindOfObject() {
        Integer key = Integer.valueOf(3);
        Date value = new Date();

        m_dict.put(key, value);
        assertEquals(value, m_dict.get(key));
        
        m_dict.put(key, "other-value");
        assertEquals("other-value", m_dict.get(key));
    }

    /**
     * Test that we cannot put a null-key into a {@link ObservableDictionary}.
     */
    public void testPutNullKeyFail() {
        try {
            m_dict.put(null, "value");
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test that we cannot put a null-value into a {@link ObservableDictionary}.
     */
    public void testPutNullValueFail() {
        try {
            m_dict.put("key", null);
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that removing a key emits a change event.
     */
    public void testRemoveEmitsEvent() {
        m_dict.setDictionaryChangeListener(m_listener);
        
        m_dict.remove("key");
        
        assertTrue(m_listener.m_removedCalled);
        assertFalse(m_listener.m_addedCalled);
        assertFalse(m_listener.m_changedCalled);
    }

    /**
     * Tests that we cannot remove a null-key from a {@link ObservableDictionary}.
     */
    public void testRemoveNullKeyFail() {
        try {
            m_dict.remove(null);
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that we can remove any kind of object from a {@link ObservableDictionary}.
     */
    public void testRemoveObject() {
        Integer key = Integer.valueOf(3);
        Date value = new Date();

        m_dict.put(key, value);
        assertEquals(value, m_dict.get(key));
        
        m_dict.remove(key);
        assertNull(m_dict.get(key));
    }

    /**
     * Tests that {@link ObservableDictionary#equals(Object)} and {@link ObservableDictionary#hashCode()} work correctly.
     */
    public void testEqualsAndHashcode() {
        ObservableDictionary d1 = new ObservableDictionary(null, null);
        ObservableDictionary d2 = new ObservableDictionary(null, null);
        
        assertTrue(d1.hashCode() == d2.hashCode());
        assertTrue(d1.equals(d2));
        assertTrue(d1.equals(d1));
        
        d2.put("foo", "bar");
        
        assertFalse(d1.hashCode() == d2.hashCode());
        assertFalse(d1.equals(d2));
        assertFalse(d1.equals(null));
        assertFalse(d1.equals("bar"));
        assertTrue(d1.equals(d1));
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_dict = new ObservableDictionary(null, null);
        m_listener = new StateCheckingDictionaryChangeListener();

    }
}
