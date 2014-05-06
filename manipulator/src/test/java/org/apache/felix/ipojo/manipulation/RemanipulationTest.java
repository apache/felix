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

package org.apache.felix.ipojo.manipulation;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.metadata.AnnotationMetadataProvider;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test Case for FELIX-3461.
 * Checks the consistency of multiple manipulation.
 */
public class RemanipulationTest extends TestCase {

    /**
     * Tests checking that the consecutive manipulation does still returns valid metadata (from annotations),
     * and valid manipulation metadata.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testDoubleManipulationWithAnnotations() throws IOException, ClassNotFoundException {
        Reporter reporter = mock(Reporter.class);
        // Step 1 - First collection and manipulation
        //1.1 Metadata collection
        byte[] origin = ManipulatorTest.getBytesFromFile(new File("target/test-classes/test/PlentyOfAnnotations.class"));
        MiniStore store = new MiniStore()
                .addClassToStore("test.PlentyOfAnnotations",
                        origin);
        AnnotationMetadataProvider provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> originalMetadata = provider.getMetadatas();
        // 1.2 Manipulation
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        manipulator.prepare(origin);
        byte[] clazz = manipulator.manipulate(origin);
        Element originalManipulationMetadata = manipulator.getManipulationMetadata();
        // 1.3 Check that the class is valid
        ManipulatedClassLoader classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        Class cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------

        // Step 2 - Second collection and manipulation
        // We use the output class as entry.
        // 2.1 Metadata collection
        store = new MiniStore().addClassToStore("test.PlentyOfAnnotations", clazz);
        provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> metadataAfterOneManipulation = provider.getMetadatas();
        // 2.2 Manipulation
        manipulator = new Manipulator(this.getClass().getClassLoader());
        manipulator.prepare(clazz);
        byte[] clazz2 = manipulator.manipulate(clazz);
        Element manipulationMetadataAfterSecondManipulation = manipulator.getManipulationMetadata();
        // 2.3 Check that the class is valid
        classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------

        // Step 3 - Third collection and manipulation
        // We use the output class of 2 as entry.
        // 3.1 Metadata collection
        store = new MiniStore().addClassToStore("test.PlentyOfAnnotations", clazz2);
        provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> metadataAfterTwoManipulation = provider.getMetadatas();
        // 3.2 Manipulation
        manipulator = new Manipulator(this.getClass().getClassLoader());
        manipulator.prepare(clazz2);
        byte[] clazz3 = manipulator.manipulate(clazz2);
        Element manipulationMetadataAfterThirdManipulation = manipulator.getManipulationMetadata();
        // 3.3 Check that the class is valid
        classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------
        // Verification

        // Unchanged metadata
        Assert.assertEquals(originalMetadata.toString(), metadataAfterOneManipulation.toString());
        Assert.assertEquals(originalMetadata.toString(), metadataAfterTwoManipulation.toString());

        // Unchanged manipulation metadata
        Assert.assertEquals(originalManipulationMetadata.toString(),
                manipulationMetadataAfterSecondManipulation.toString());
        Assert.assertEquals(originalManipulationMetadata.toString(),
                manipulationMetadataAfterThirdManipulation.toString());

    }

    private class MiniStore implements ResourceStore {

        private Map<String, byte[]> resources;

        public MiniStore addClassToStore(String qualifiedName, byte[] bytes) {
            if (this.resources == null) {
                this.resources = new HashMap<String, byte[]>();
            }
            this.resources.put(Strings.asResourcePath(qualifiedName), bytes);
            return this;
        }

        public byte[] read(String path) throws IOException {
            byte[] bytes = resources.get(path);
            if (bytes == null) {
                throw new IOException();
            }
            return bytes;
        }

        public void accept(ResourceVisitor visitor) {
            for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                visitor.visit(entry.getKey());
            }
        }

        public void open() throws IOException {
        }

        public void writeMetadata(Element metadata) {
        }

        public void write(String resourcePath, byte[] resource) throws IOException {
        }

        public void close() throws IOException {
        }
    }

}
