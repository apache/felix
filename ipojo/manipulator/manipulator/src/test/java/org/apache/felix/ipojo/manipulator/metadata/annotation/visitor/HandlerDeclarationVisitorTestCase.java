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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor;

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbenchTestCase.node;
import static org.mockito.Mockito.mock;

/**
 * User: guillaume
 * Date: 14/02/13
 * Time: 10:22
 */
public class HandlerDeclarationVisitorTestCase extends TestCase {

    private Reporter reporter;
    private ComponentWorkbench workbench;
    private HandlerDeclarationVisitor visitor;

    @Override
    public void setUp() throws Exception {
        reporter = mock(Reporter.class);
        workbench = new ComponentWorkbench(null, node());
        workbench.setRoot(new Element("container", null));
        visitor = new HandlerDeclarationVisitor(workbench, builder(), reporter);
    }

    public void testElementConversionWithNoNamespace() throws Exception {
        visitor.visit(null, "<testing/>");
        visitor.visitEnd();

        Element produced = workbench.build();
        Element testing = produced.getElements("testing")[0];
        assertEquals(0, testing.getElements().length);
        assertEquals(0, testing.getAttributes().length);
    }

    public void testElementConversionWithNamespace() throws Exception {
        visitor.visit(null, "<ns:testing xmlns:ns='org.apache.felix.ipojo.testing'/>");
        visitor.visitEnd();

        Element produced = workbench.build();
        Element testing = produced.getElements("testing", "org.apache.felix.ipojo.testing")[0];
        assertEquals(0, testing.getElements().length);
        assertEquals(0, testing.getAttributes().length);
        assertNotNull(produced.getElements("org.apache.felix.ipojo.testing:testing"));
        assertNull(produced.getElements("testing"));
    }

    public void testAttributeConversionWithNamespace() throws Exception {
        visitor.visit(null, "<ns:testing xmlns:ns='org.apache.felix.ipojo.testing' ns:name='Guillaume'/>");
        visitor.visitEnd();

        Element produced = workbench.build();
        Element testing = produced.getElements("testing", "org.apache.felix.ipojo.testing")[0];
        assertEquals(0, testing.getElements().length);
        assertEquals(1, testing.getAttributes().length);
        assertEquals("Guillaume", testing.getAttribute("name", "org.apache.felix.ipojo.testing"));
        assertEquals("Guillaume", testing.getAttribute("org.apache.felix.ipojo.testing:name"));
    }

    public void testAttributeConversionWithNoNamespace() throws Exception {
        visitor.visit(null, "<ns:testing xmlns:ns='org.apache.felix.ipojo.testing' name='Guillaume'/>");
        visitor.visitEnd();

        Element produced = workbench.build();
        Element testing = produced.getElements("testing", "org.apache.felix.ipojo.testing")[0];
        assertEquals(0, testing.getElements().length);
        assertEquals(1, testing.getAttributes().length);
        assertEquals("Guillaume", testing.getAttribute("name"));
    }

    private DocumentBuilder builder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }
}
