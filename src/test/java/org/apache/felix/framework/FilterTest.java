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
package org.apache.felix.framework;

import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

public class FilterTest extends TestCase
{
    public void testMissingAttribute()
    {
        Dictionary dict = new Hashtable();
        dict.put("one", "one-value");
        dict.put("two", "two-value");
        dict.put("three", "three-value");
        Filter filter = null;
        try
        {
            filter = FrameworkUtil.createFilter("(missing=value)");
        }
        catch (Exception ex)
        {
            assertTrue("Filter should parse: " + ex, false);
        }
        assertFalse("Filter should not match: " + filter, filter.match(dict));
    }
}
