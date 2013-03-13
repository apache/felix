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

package org.apache.felix.ipojo.manipulator.store.mapper;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.store.ResourceMapper;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileSystemResourceMapperTestCase extends TestCase {

    public void testUnixInternalize() throws Exception {

        ResourceMapper delegate = mock(ResourceMapper.class);
        FileSystemResourceMapper mapper = new FileSystemResourceMapper(delegate);

        String path = "this/is/a/unix/like/path.extension";
        when(delegate.internalize(eq(path))).thenReturn(path);

        String result = mapper.internalize(path);

        Assert.assertEquals(path, result);

    }

    public void testUnixExternalize() throws Exception {

        ResourceMapper delegate = mock(ResourceMapper.class);
        FileSystemResourceMapper mapper = new FileSystemResourceMapper(delegate);

        String path = "this/is/a/unix/like/path.extension";
        when(delegate.externalize(eq(path))).thenReturn(path);

        String result = mapper.externalize(path);

        // unix path is already normalized
        Assert.assertEquals(path, result);
    }

    public void ignoreWindowsExternalize() throws Exception {
        // As Java doesn't like '\' alone in Strings I have to replace them on the fly :'(
        // Grrr doesn't work as expected
        ResourceMapper delegate = mock(ResourceMapper.class);
        FileSystemResourceMapper mapper = new FileSystemResourceMapper(delegate);

        String path = "c:\\this\\is\\a\\windows\\like\\path.extension";
        when(delegate.externalize(eq(path))).thenReturn(path);

        String expected = "c:/this/is/a/windows/like/path.extension";
        String result = mapper.externalize(path);

        // unix path is already normalized
        Assert.assertEquals(expected, result);
    }
}
