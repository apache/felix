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

import static org.apache.felix.http.base.internal.util.UriUtils.concat;
import static org.apache.felix.http.base.internal.util.UriUtils.decodePath;
import static org.apache.felix.http.base.internal.util.UriUtils.removeDotSegments;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test cases for {@link UriUtils}.
 */
public class UriUtilsTest
{
    @Test
    public void testConcatOk()
    {
        assertEquals(null, concat(null, null));
        assertEquals("", concat(null, ""));
        assertEquals("/", concat(null, "/"));
        assertEquals("foo", concat(null, "foo"));
        assertEquals("/foo", concat(null, "/foo"));

        assertEquals("", concat("", null));
        assertEquals("/", concat("/", null));
        assertEquals("foo", concat("foo", null));
        assertEquals("/foo", concat("/foo", null));

        assertEquals("", concat("", ""));
        assertEquals("foo", concat("", "foo"));
        assertEquals("/", concat("", "/"));
        assertEquals("/foo", concat("", "/foo"));

        assertEquals("foo", concat("foo", ""));
        assertEquals("/", concat("/", ""));
        assertEquals("/foo", concat("/foo", ""));

        assertEquals("foo", concat("foo", ""));
        assertEquals("foo/bar", concat("foo", "bar"));
        assertEquals("foo/", concat("foo", "/"));
        assertEquals("foo/bar", concat("foo", "/bar"));

        assertEquals("/foo", concat("/", "foo"));
        assertEquals("/", concat("/", "/"));
        assertEquals("/bar", concat("/", "/bar"));

        assertEquals("foo/", concat("foo/", null));
        assertEquals("foo/", concat("foo/", ""));
        assertEquals("foo/bar", concat("foo/", "bar"));
        assertEquals("foo/", concat("foo/", "/"));
        assertEquals("foo/bar", concat("foo/", "/bar"));

        assertEquals("?quu=1", concat("?quu=1", null));
        assertEquals("?quu=1", concat("?quu=1", ""));
        assertEquals("foo?quu=1", concat("?quu=1", "foo"));
        assertEquals("/?quu=1", concat("?quu=1", "/"));
        assertEquals("/foo?quu=1", concat("?quu=1", "/foo"));

        assertEquals("foo?quu=1", concat("foo?quu=1", null));
        assertEquals("foo?quu=1", concat("foo?quu=1", ""));
        assertEquals("foo/bar?quu=1", concat("foo?quu=1", "bar"));
        assertEquals("foo/?quu=1", concat("foo?quu=1", "/"));
        assertEquals("foo/bar?quu=1", concat("foo?quu=1", "/bar"));

        assertEquals("foo/?quu=1", concat("foo/?quu=1", null));
        assertEquals("foo/?quu=1", concat("foo/?quu=1", ""));
        assertEquals("foo/bar?quu=1", concat("foo/?quu=1", "bar"));
        assertEquals("foo/?quu=1", concat("foo/?quu=1", "/"));
        assertEquals("foo/bar?quu=1", concat("foo/?quu=1", "/bar"));
    }

    @Test
    public void testDecodePathOk()
    {
        assertEquals(null, decodePath(null));
        assertEquals("foo bar", decodePath("foo%20bar"));
        assertEquals("foo%23;,:=b a r", decodePath("foo%2523%3b%2c:%3db%20a%20r"));
        assertEquals("f\u00e4\u00e4%23;,:=b a r=", decodePath("f\u00e4\u00e4%2523%3b%2c:%3db%20a%20r%3D"));
        assertEquals("f\u0629\u0629%23;,:=b a r", decodePath("f%d8%a9%d8%a9%2523%3b%2c:%3db%20a%20r"));
    }

    @Test
    public void testRemoveDotSegmentsOk()
    {
        assertEquals(null, removeDotSegments(null));
        assertEquals("", removeDotSegments(""));
        assertEquals("", removeDotSegments("."));
        assertEquals("", removeDotSegments(".."));
        assertEquals("/", removeDotSegments("/"));
        assertEquals("/", removeDotSegments("/."));
        assertEquals("", removeDotSegments("/.."));
        assertEquals("foo", removeDotSegments("./foo"));
        assertEquals("/bar/", removeDotSegments("./foo/../bar/"));
        assertEquals("foo", removeDotSegments("../foo"));
        assertEquals("/", removeDotSegments("/foo/.."));
        assertEquals("/foo/", removeDotSegments("/foo/."));
        assertEquals("/foo/bar", removeDotSegments("/foo/./bar"));
        assertEquals("/bar", removeDotSegments("/foo/../bar"));
        assertEquals("/bar", removeDotSegments("/foo/./../bar"));
        assertEquals("/foo/bar", removeDotSegments("/foo/././bar"));
        assertEquals("/qux", removeDotSegments("/foo/bar/../../qux"));
        assertEquals("/foo/qux/quu", removeDotSegments("/foo/bar/../qux/././quu"));
        assertEquals("/bar//", removeDotSegments("/foo/./../bar//"));
        assertEquals("/", removeDotSegments("/foo/../bar/.."));
        assertEquals("/foo/quu", removeDotSegments("/foo/bar/qux/./../../quu"));
        assertEquals("mid/6", removeDotSegments("mid/content=5/../6"));
        assertEquals("//bar/qux/file.ext", removeDotSegments("foo/.././/bar/qux/file.ext"));
        // weird cases
        assertEquals("..foo", removeDotSegments("..foo"));
        assertEquals("foo..", removeDotSegments("foo.."));
        assertEquals("foo.", removeDotSegments("foo."));
        assertEquals("/.foo", removeDotSegments("/.foo"));
        assertEquals("/..foo", removeDotSegments("/..foo"));

        // FELIX-4440
        assertEquals("foo.bar", removeDotSegments("foo.bar"));
        assertEquals("/test.jsp", removeDotSegments("/test.jsp"));
        assertEquals("http://foo/bar./qux.quu", removeDotSegments("http://foo/bar./qux.quu"));
        assertEquals("http://foo/bar.qux/quu", removeDotSegments("http://foo/bar.qux/quu"));
    }
}
