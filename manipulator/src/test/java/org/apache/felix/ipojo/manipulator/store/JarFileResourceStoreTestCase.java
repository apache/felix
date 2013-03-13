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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.util.Streams;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class JarFileResourceStoreTestCase extends TestCase {
    public static final String RESOURCE_PATH = "org/apache/felix/ipojo/test/scenarios/component/Annotation.class";
    private JarFileResourceStore store;
    private File out;

    public void setUp() throws Exception {
        File classes = new File("target", "test-classes");
        out = new File(classes, "test-store.jar");
        JarFile jar = new JarFile(new File(classes, "tests.manipulation.java5.jar"));
        store = new JarFileResourceStore(jar, out);
    }

    @Override
    public void tearDown() throws Exception {
        out.delete();
    }

    public void testRead() throws Exception {
        byte[] bytes = store.read(RESOURCE_PATH);
        Assert.assertNotNull(bytes);
    }

    public void testAccept() throws Exception {
        ResourceVisitor visitor = mock(ResourceVisitor.class);
        store.accept(visitor);

        verify(visitor, atLeastOnce()).visit(anyString());

    }

    public void testWrite() throws Exception {

        ManifestBuilder builder = mock(ManifestBuilder.class);
        Manifest manifest = new Manifest();
        when(builder.build(any(Manifest.class))).thenReturn(manifest);
        store.setManifestBuilder(builder);

        store.write(RESOURCE_PATH, "Hello World".getBytes());

        store.close();

        JarFile outJar = new JarFile(out);
        Assert.assertNotNull(outJar.getEntry(RESOURCE_PATH));
        byte[] bytes = Streams.readBytes(outJar.getInputStream(outJar.getEntry(RESOURCE_PATH)));

        Assert.assertEquals("Hello World", new String(bytes));

    }
}
