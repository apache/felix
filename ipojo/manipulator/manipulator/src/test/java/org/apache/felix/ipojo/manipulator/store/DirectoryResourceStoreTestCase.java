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

package org.apache.felix.ipojo.manipulator.store;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.manipulator.util.Strings;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A {@code DirectoryBytecodeStoreTestCase} is ...
 *
 */
public class DirectoryResourceStoreTestCase extends TestCase {

    private DirectoryResourceStore store;
    private File classes;
    private File out;

    public void setUp() throws Exception {
        classes = new File("target", "classes");
        out = new File(classes, "test.txt");
        store = new DirectoryResourceStore(classes);
    }

    @Override
    public void tearDown() throws Exception {
        out.delete();
    }

    public void testAccessibleResources() throws Exception {
        byte[] data = store.read(Streams.class.getName().replace('.', '/').concat(".class"));
        assertNotNull("Data cannot be null", data);
    }

    public void testInaccessibleResources() throws Exception {
        try {
            store.read("something/that/do/not/exists.txt");
            fail();
        } catch (IOException ioe) {
            // Expected
        }
    }

    public void testResourceWriting() throws Exception {
        store.write("test.txt", "Hello World".getBytes());
        Assert.assertTrue(out.isFile());
    }

    public void testResourceVisitor() throws Exception {

        // final String expectedPath = Strings.asResourcePath(Pojoization.class.getName());
        ResourceVisitor visitor = mock(ResourceVisitor.class);
        store.accept(visitor);

        verify(visitor, atLeastOnce()).visit(anyString());

        // TODO try to check that Pojoization class resource was called
    }
}
