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
package org.apache.felix.eventadmin.impl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatchersTest {

    @Test public void testPackageMatchersNoConfig()
    {
        assertNull(Matchers.createPackageMatchers(null));
        assertNull(Matchers.createPackageMatchers(new String[0]));
    }

    @Test public void testPackageMatchersExact()
    {
        final Matchers.Matcher[] m = Matchers.createPackageMatchers(new String[] {"org.apache.felix.Foo"});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org.apache.felix.Foo"));
        assertFalse(m[0].match("org.apache.felix.Bar"));
        assertFalse(m[0].match("org.apache.felix.Foo$1"));
        assertFalse(m[0].match("org.apache.felix.Foo.Test"));
        assertFalse(m[0].match("org.apache.felix"));
    }

    @Test public void testPackageMatchersPackage()
    {
        final Matchers.Matcher[] m = Matchers.createPackageMatchers(new String[] {"org.apache.felix."});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org.apache.felix.Foo"));
        assertTrue(m[0].match("org.apache.felix.Bar"));
        assertTrue(m[0].match("org.apache.felix.Foo$1"));
        assertFalse(m[0].match("org.apache.felix.Foo.Test"));
        assertFalse(m[0].match("org.apache.felix"));
    }

    @Test public void testPackageMatchersSubPackage()
    {
        final Matchers.Matcher[] m = Matchers.createPackageMatchers(new String[] {"org.apache.felix*"});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org.apache.felix.Foo"));
        assertTrue(m[0].match("org.apache.felix.Bar"));
        assertTrue(m[0].match("org.apache.felix.Foo$1"));
        assertTrue(m[0].match("org.apache.felix.Foo.Test"));
        assertFalse(m[0].match("org.apache.felix"));
    }

    @Test public void testTopicMatchersNoConfig()
    {
        assertNull(Matchers.createEventTopicMatchers(null));
        assertNull(Matchers.createEventTopicMatchers(new String[0]));
    }

    @Test public void testTopicMatchersExact()
    {
        final Matchers.Matcher[] m = Matchers.createEventTopicMatchers(new String[] {"org/apache/felix/Foo"});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org/apache/felix/Foo"));
        assertFalse(m[0].match("org/apache/felix/Bar"));
        assertFalse(m[0].match("org/apache/felix/Foo$1"));
        assertFalse(m[0].match("org/apache/felix/Foo/Test"));
        assertFalse(m[0].match("org/apache/felix"));
    }

    @Test public void testTopicMatchersPackage()
    {
        final Matchers.Matcher[] m = Matchers.createEventTopicMatchers(new String[] {"org/apache/felix."});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org/apache/felix/Foo"));
        assertTrue(m[0].match("org/apache/felix/Bar"));
        assertTrue(m[0].match("org/apache/felix/Foo$1"));
        assertFalse(m[0].match("org/apache/felix/Foo/Test"));
        assertFalse(m[0].match("org/apache/felix"));
    }

    @Test public void testTopicMatchersSubPackage()
    {
        final Matchers.Matcher[] m = Matchers.createEventTopicMatchers(new String[] {"org/apache/felix*"});
        assertNotNull(m);
        assertEquals(1, m.length);
        assertNotNull(m[0]);

        assertTrue(m[0].match("org/apache/felix/Foo"));
        assertTrue(m[0].match("org/apache/felix/Bar"));
        assertTrue(m[0].match("org/apache/felix/Foo$1"));
        assertTrue(m[0].match("org/apache/felix/Foo/Test"));
        assertFalse(m[0].match("org/apache/felix"));
    }
}
