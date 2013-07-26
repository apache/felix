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

package org.apache.felix.ipojo.manipulator.metadata;

import junit.framework.TestCase;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;
import test.AnnotatedComponent;
import test.FakeAnnotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class AnnotationMetadataProviderTestCase extends TestCase {
    public void testGetMetadatas() throws Exception {
        MiniStore store = new MiniStore(AnnotatedComponent.class, FakeAnnotation.class);
        Reporter reporter = mock(Reporter.class);
        AnnotationMetadataProvider provider = new AnnotationMetadataProvider(store, reporter);

        List<Element> meta = provider.getMetadatas();
        assertEquals(1, meta.size());


    }

    private class MiniStore implements ResourceStore {

        private Map<String, byte[]> resources;

        public MiniStore(Class<?>... classes) throws IOException {
            this.resources = new HashMap<String, byte[]>();
            for (Class<?> type : classes) {
                resources.put(Strings.asResourcePath(type.getName()), from(type));
            }
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

        public void open() throws IOException {}

        public void writeMetadata(Element metadata) {}

        public void write(String resourcePath, byte[] resource) throws IOException {}

        public void close() throws IOException {}
    }

    private byte[] from(Class<?> type) throws IOException {
        ClassLoader loader = type.getClassLoader();
        InputStream is = loader.getResourceAsStream(Strings.asResourcePath(type.getName()));
        return Streams.readBytes(is);
    }

}
