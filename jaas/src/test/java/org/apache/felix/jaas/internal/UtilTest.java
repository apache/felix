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

package org.apache.felix.jaas.internal;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UtilTest
{

    @Test
    public void testTrimToNull() throws Exception
    {
        assertNull(Util.trimToNull(""));
        assertNull(Util.trimToNull(null));
        assertNull(Util.trimToNull("   "));
        assertEquals("foo", Util.trimToNull("foo   "));
        assertEquals("foo", Util.trimToNull("foo"));
        assertEquals("foo",Util.trimToNull("  foo  "));
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        assertTrue(Util.isEmpty(null));
        assertTrue(Util.isEmpty(""));
        assertFalse(Util.isEmpty("foo"));
    }
}
