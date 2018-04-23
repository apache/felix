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
package org.apache.felix.http.base.internal.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test cases for {@link PatternUtil}.
 */
public class PatternUtilTest
{

    @Test
    public void testSymbolicName()
    {
        assertTrue(PatternUtil.isValidSymbolicName("default"));
        assertFalse(PatternUtil.isValidSymbolicName("$bad#"));
        assertTrue(PatternUtil.isValidSymbolicName("abcdefghijklmnopqrstuvwyz"));
        assertTrue(PatternUtil.isValidSymbolicName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertTrue(PatternUtil.isValidSymbolicName("0123456789-_"));
    }

    @Test public void testServletPattern()
    {
        assertFalse(PatternUtil.isValidPattern(null));
        assertTrue(PatternUtil.isValidPattern(""));
        assertTrue(PatternUtil.isValidPattern("*.html"));
        assertTrue(PatternUtil.isValidPattern("/"));
        assertTrue(PatternUtil.isValidPattern("/test"));
        assertTrue(PatternUtil.isValidPattern("/test/*"));
        assertTrue(PatternUtil.isValidPattern("/foo/bar"));
        assertTrue(PatternUtil.isValidPattern("/foo/bar/*"));
        assertFalse(PatternUtil.isValidPattern("/*.html"));
        assertFalse(PatternUtil.isValidPattern("/*/foo"));
        assertFalse(PatternUtil.isValidPattern("foo"));
        assertFalse(PatternUtil.isValidPattern("foo/bla"));
        assertFalse(PatternUtil.isValidPattern("/test/"));
    }
}
