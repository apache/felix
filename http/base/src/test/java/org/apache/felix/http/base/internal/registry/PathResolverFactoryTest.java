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
package org.apache.felix.http.base.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PathResolverFactoryTest {

    private void assertResult(final PathResolver resolver,
            final String path,
            final String expectedServletPath,
            final String expectedPathInfo)
    {
        final PathResolution pr = resolver.resolve(path);
        assertNotNull(pr);
        assertEquals(path, pr.requestURI);
        assertEquals(expectedServletPath, pr.servletPath);
        if ( expectedPathInfo == null )
        {
            assertNull(pr.pathInfo);
        }
        else
        {
            assertEquals(expectedPathInfo, pr.pathInfo);
        }
    }

    @Test public void testRootMatching()
    {
        final PathResolver pr = PathResolverFactory.createPatternMatcher(null, "");
        assertNotNull(pr);

        assertResult(pr, "/", "", "/");
        assertResult(pr, "", "", "/");

        assertNull(pr.resolve("/foo"));
    }

    @Test public void testDefaultMatcher()
    {
        final PathResolver pr = PathResolverFactory.createPatternMatcher(null, "/");
        assertNotNull(pr);

        assertResult(pr, "/foo/bar", "/foo/bar", null);
        assertResult(pr, "/foo", "/foo", null);
    }

    @Test public void testPathMatcher()
    {
        final PathResolver pr = PathResolverFactory.createPatternMatcher(null, "/*");
        assertNotNull(pr);

        assertResult(pr, "/foo", "", "/foo");
        assertResult(pr, "/foo/bar", "", "/foo/bar");

        assertResult(pr, "/", "", "/");

        assertResult(pr, "", "", null);
    }
}
