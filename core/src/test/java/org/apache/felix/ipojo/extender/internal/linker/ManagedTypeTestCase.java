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

package org.apache.felix.ipojo.extender.internal.linker;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 10/06/13
 * Time: 13:32
 */
public class ManagedTypeTestCase extends TestCase {

    @Mock
    private BundleContext bundleContext;
    @Mock
    private QueueService queueService;
    @Mock
    private TypeDeclaration declaration;
    @Mock
    private Filter filter;


    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(bundleContext.createFilter(anyString())).thenReturn(filter);
    }

    public void testStartingManagedType() throws Exception {

        String filterStr = String.format(
                "(&(objectclass=%s)(%s=%s))",
                ExtensionDeclaration.class.getName(),
                ExtensionDeclaration.EXTENSION_NAME_PROPERTY,
                "test"
        );

        when(declaration.getExtension()).thenReturn("test");
        when(declaration.getComponentMetadata()).thenReturn(element("test", "f.q.n.Type"));
        when(declaration.getComponentName()).thenReturn("Type");
        when(filter.toString()).thenReturn(filterStr);

        ManagedType managedType = new ManagedType(bundleContext, queueService, declaration);
        managedType.start();

        verify(bundleContext).getAllServiceReferences(null, filterStr);

        // Check that the filter contains a logical OR with component's name and classname
        verify(bundleContext).createFilter(
                contains(
                        String.format("(|(%s=%s)(%s=%s))",
                               InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                               "Type",
                               InstanceDeclaration.COMPONENT_NAME_PROPERTY,
                               "f.q.n.Type")
                )
        );
    }

    private Element element(String type, String classname) {
        Element root = new Element(type, null);
        root.addAttribute(new Attribute("classname", classname));
        return root;
    }

}
