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

import org.apache.felix.useradmin.impl.role.ObservableProperties;

import junit.framework.TestCase;

/**
 * Test case for {@link ObservableProperties}.
 */
public class ObservablePropertiesTest extends TestCase {
    
    private ObservableProperties m_dict;

    /**
     * Tests that calling get with a non-string value yields an exception.
     */
    public void testGetNonStringKeyFail() {
        try {
            m_dict.get(Integer.valueOf(1));
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling get with a string value does not yield an exception.
     */
    public void testGetStringKeyOk() {
        assertNull(m_dict.get("key"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#put(java.lang.Object, java.lang.Object)}.
     */
    public void testPutByteArrayValueOk() {
        assertNull(m_dict.put("key", "value".getBytes()));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#put(java.lang.Object, java.lang.Object)}.
     */
    public void testPutNonStringKeyFail() {
        try {
            m_dict.put(Integer.valueOf(1), "value");

            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#put(java.lang.Object, java.lang.Object)}.
     */
    public void testPutNonStringValueFail() {
        try {
            m_dict.put("key", Integer.valueOf(1));

            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#put(java.lang.Object, java.lang.Object)}.
     */
    public void testPutStringKeyValueOk() {
        assertNull(m_dict.put("key", "value"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#remove(java.lang.Object)}.
     */
    public void testRemoveNonStringKeyFail() {
        try {
            m_dict.remove(Integer.valueOf(1));

            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.ObservableProperties#remove(java.lang.Object)}.
     */
    public void testRemoveStringKeyOk() {
        assertNull(m_dict.remove("foo"));
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_dict = new ObservableProperties(null, null);
    }
}
