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

package org.apache.felix.ipojo.manipulator.visitor.check;

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.ManipulationResultVisitor;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;


public class CheckFieldConsistencyResultVisitorTestCase extends TestCase {

    @Mock
    private Reporter reporter;

    @Mock
    private ManipulationResultVisitor delegate;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testVisitClassStructureOK() throws Exception {
        Element component = newComponentElement();
        CheckFieldConsistencyResultVisitor visitor = new CheckFieldConsistencyResultVisitor(delegate);
        visitor.setReporter(reporter);
        visitor.setMetadata(component);

        Element manipulation = newManipulationElement(true);
        visitor.visitClassStructure(manipulation);

        verifyZeroInteractions(reporter);

    }

    public void testVisitClassStructureWithMissingFields() throws Exception {
        Element component = newComponentElement();
        CheckFieldConsistencyResultVisitor visitor = new CheckFieldConsistencyResultVisitor(delegate);
        visitor.setReporter(reporter);
        visitor.setMetadata(component);

        Element manipulation = newManipulationElement(false);
        visitor.visitClassStructure(manipulation);

        verify(reporter).error(anyString());

    }

    private Element newManipulationElement(boolean complete) {
        Element manipulation = new Element("manipulation", null);
        if (complete) {
            Element field = new Element("field", null);
            field.addAttribute(new Attribute("name", "property"));
            manipulation.addElement(field);
        }

        return manipulation;
    }

    private Element newComponentElement() {
        Element component = new Element("component", null);
        Element requires = new Element("requires", null);
        requires.addAttribute(new Attribute("field", "property"));
        component.addElement(requires);

        return component;
    }
}
