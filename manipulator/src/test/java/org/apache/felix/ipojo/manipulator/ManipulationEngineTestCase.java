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

package org.apache.felix.ipojo.manipulator;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import test.ClusterDaemon;
import test.PojoWithInner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManipulationEngineTestCase extends TestCase {

    @Mock
    private Reporter reporter;

    @Mock
    private ResourceStore store;

    @Mock
    private ManipulationVisitor visitor;

    @Mock
    private ManipulationResultVisitor result;

    @InjectMocks
    private ManipulationEngine engine = new ManipulationEngine(this.getClass().getClassLoader());


    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testManipulationOfSimpleClass() throws Exception {

        when(store.read(anyString())).thenReturn(from(ClusterDaemon.class));
        when(visitor.visitManipulationResult(any(Element.class))).thenReturn(result);

        String path = Strings.asResourcePath(ClusterDaemon.class.getName());
        Element metadata = new Element("", "");
        ManipulationUnit info = new ManipulationUnit(path, metadata);
        engine.addManipulationUnit(info);

        engine.generate();

        verify(visitor).visitManipulationResult(eq(metadata));
        verify(result).visitClassStructure(any(Element.class));
        verify(result).visitManipulatedResource(eq(path), any(byte[].class));
        verify(result).visitEnd();

    }

    public void testManipulationOfInnerClass() throws Exception {

        when(visitor.visitManipulationResult(any(Element.class))).thenReturn(result);

        String innerPath = Strings.asResourcePath(PojoWithInner.MyInner.class.getName());
        when(store.read(innerPath)).thenReturn(from(PojoWithInner.MyInner.class));

        String path = Strings.asResourcePath(PojoWithInner.class.getName());
        when(store.read(path)).thenReturn(from(PojoWithInner.class));

        Element metadata = new Element("", "");
        ManipulationUnit info = new ManipulationUnit(path, metadata);
        engine.addManipulationUnit(info);

        engine.generate();

        verify(visitor).visitManipulationResult(eq(metadata));
        verify(result).visitClassStructure(any(Element.class));
        verify(result).visitManipulatedResource(eq(path), any(byte[].class));
        verify(result).visitManipulatedResource(eq(innerPath), any(byte[].class));
        verify(result).visitEnd();

    }

    private byte[] from(Class<?> type) throws IOException {
        ClassLoader loader = type.getClassLoader();
        InputStream is = loader.getResourceAsStream(Strings.asResourcePath(type.getName()));
        return Streams.readBytes(is);
    }
}
