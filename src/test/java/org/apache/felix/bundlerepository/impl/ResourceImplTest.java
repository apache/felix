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
package org.apache.felix.bundlerepository.impl;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;

public class ResourceImplTest extends TestCase
{
    public void testGetSizeFileResource() {
        ResourceImpl res = new ResourceImpl();
        res.put(Property.URI, "repo_files/test_file_3.jar");

        final URL dir = getClass().getResource("/repo_files");
        Repository repo = new RepositoryImpl() {
            { setURI(dir.toExternalForm()); }
        };
        res.setRepository(repo);

        assertEquals("Should have obtained the file size", 3, (long) res.getSize());
    }

    public void testGetSizeNonExistentFileResource() {
        ResourceImpl res = new ResourceImpl();
        res.put(Property.URI, "repo_files/test_file_3_garbage.jar");

        final URL dir = getClass().getResource("/repo_files");
        Repository repo = new RepositoryImpl() {
            { setURI(dir.toExternalForm()); }
        };
        res.setRepository(repo);

        assertEquals("File size should be reported as 0", 0, (long) res.getSize());
    }

    public void testGetSizeNonFileResource() {
        final URL testFile4 = getClass().getResource("/repo_files/test_file_4.jar");

        ResourceImpl res = new ResourceImpl();
        res.put(Property.URI, "jar:" + testFile4.toExternalForm() + "!/blah.txt");

        final URL dir = getClass().getResource("/repo_files");
        Repository repo = new RepositoryImpl() {
            { setURI(dir.toExternalForm()); }
        };
        res.setRepository(repo);

        assertEquals("Should have obtained the file size", 5, (long) res.getSize());
    }

    public void testGetSizeNonExistentResource() {
        final URL testFile4 = getClass().getResource("/repo_files/test_file_4.jar");

        ResourceImpl res = new ResourceImpl();
        res.put(Property.URI, "jar:" + testFile4.toExternalForm() + "!/blah_xyz.txt");

        final URL dir = getClass().getResource("/repo_files");
        Repository repo = new RepositoryImpl() {
            { setURI(dir.toExternalForm()); }
        };
        res.setRepository(repo);

        assertEquals("File size should be reported as 0", 0, (long) res.getSize());
    }
}
