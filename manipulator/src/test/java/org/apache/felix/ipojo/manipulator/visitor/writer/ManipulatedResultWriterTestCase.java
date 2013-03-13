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

package org.apache.felix.ipojo.manipulator.visitor.writer;

import junit.framework.TestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.mockito.Mockito.*;


public class ManipulatedResultWriterTestCase extends TestCase {

    @Spy
    private Element element = new Element("component", null);

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testVisitClassStructure() throws Exception {
        ManipulatedResultWriter writer = new ManipulatedResultWriter(element);

        Element manipulation = new Element("manipulation", null);
        writer.visitClassStructure(manipulation);
        verify(element).addElement(same(manipulation));
    }

    public void testVisitManipulatedResource() throws Exception {
        ManipulatedResultWriter writer = new ManipulatedResultWriter(element);

        writer.visitManipulatedResource("test.class", "Hello".getBytes());

        assertNotNull(writer.getResources().get("test.class"));
        assertEquals("Hello", new String(writer.getResources().get("test.class")));
    }
}
