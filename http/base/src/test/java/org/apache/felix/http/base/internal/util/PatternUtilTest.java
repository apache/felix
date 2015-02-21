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

import static org.apache.felix.http.base.internal.util.PatternUtil.convertToRegEx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Test cases for {@link PatternUtil}.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PatternUtilTest
{

    @Test
    public void testConvertToRegExOk()
    {
        assertEquals("", convertToRegEx(""));
        assertEquals("/", convertToRegEx("/"));
        assertEquals("^(/foo/bar)(|/.*)$", convertToRegEx("/foo/bar/*"));
        assertEquals("^(.*)(\\.\\Qbop\\E)$", convertToRegEx("*.bop"));
        assertEquals("^(.*)(\\.\\Qtar.gz\\E)$", convertToRegEx("*.tar.gz"));
        assertEquals("^(.*)(\\.\\Q*\\E)$", convertToRegEx("*.*"));
    }

    @Test
    public void testConvertToRegExCreatesCorrectRegExOk()
    {
        Pattern p1 = Pattern.compile(convertToRegEx("/foo/bar/*"));
        Pattern p2 = Pattern.compile(convertToRegEx("/baz/*"));
        Pattern p3 = Pattern.compile(convertToRegEx("/catalog"));
        Pattern p4 = Pattern.compile(convertToRegEx("*.bop"));
        Pattern p5 = Pattern.compile(convertToRegEx("*.tar.gz"));
        Pattern p6 = Pattern.compile(convertToRegEx("*.*"));

        // Examples from the Servlet 3.0 spec, section 12.2.2...
        assertTrue(p1.matcher("/foo/bar/index.html").matches());
        assertTrue(p1.matcher("/foo/bar/index.bop").matches());
        assertTrue(p2.matcher("/baz").matches());
        assertTrue(p2.matcher("/baz/index.html").matches());
        assertTrue(p3.matcher("/catalog").matches());
        assertFalse(p3.matcher("/catalog/index.html").matches());
        assertTrue(p4.matcher("/catalog/index.bop").matches());
        assertTrue(p4.matcher("/index.bop").matches());
        assertTrue(p5.matcher("/index.tar.gz").matches());
        assertFalse(p5.matcher("/index.tar-gz").matches());
        assertFalse(p5.matcher("/index.gz").matches());
        assertFalse(p6.matcher("/index.gz").matches());
    }

    @Test
    public void testSymbolicName()
    {
        assertTrue(PatternUtil.isValidSymbolicName("default"));
        assertFalse(PatternUtil.isValidSymbolicName("$bad#"));
        assertTrue(PatternUtil.isValidSymbolicName("abcdefghijklmnopqrstuvwyz"));
        assertTrue(PatternUtil.isValidSymbolicName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertTrue(PatternUtil.isValidSymbolicName("0123456789-_"));
    }
}
