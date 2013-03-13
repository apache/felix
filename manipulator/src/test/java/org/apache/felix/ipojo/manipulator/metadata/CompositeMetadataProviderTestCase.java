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
import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class CompositeMetadataProviderTestCase extends TestCase {

    @Mock
    private MetadataProvider delegate1;

    @Mock
    private MetadataProvider delegate2;

    @Mock
    private Reporter reporter;


    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testGetMetadatas() throws Exception {
        CompositeMetadataProvider provider = new CompositeMetadataProvider(reporter);
        provider.addMetadataProvider(delegate1);
        provider.addMetadataProvider(delegate2);

        Element returned = newComponentElement("type1");
        when(delegate1.getMetadatas()).thenReturn(Collections.singletonList(returned));

        Element returned2 = newComponentElement("type2");
        when(delegate2.getMetadatas()).thenReturn(Collections.singletonList(returned2));

        List<Element> meta = provider.getMetadatas();
        assertEquals(2, meta.size());
    }

    public void testGetMetadatasWithDuplicate() throws Exception {
        CompositeMetadataProvider provider = new CompositeMetadataProvider(reporter);
        provider.addMetadataProvider(delegate1);
        provider.addMetadataProvider(delegate2);

        Element returned = newComponentElement("type1");
        when(delegate1.getMetadatas()).thenReturn(Collections.singletonList(returned));

        Element returned2 = newComponentElement("type1");
        when(delegate2.getMetadatas()).thenReturn(Collections.singletonList(returned2));

        List<Element> meta = provider.getMetadatas();
        assertEquals(1, meta.size());

        verify(reporter).warn(anyString());
    }

    public void testGetMetadatasWithInstances() throws Exception {
        CompositeMetadataProvider provider = new CompositeMetadataProvider(reporter);
        provider.addMetadataProvider(delegate1);
        provider.addMetadataProvider(delegate2);

        Element returned = newInstanceElement("type1", "name1");
        when(delegate1.getMetadatas()).thenReturn(Collections.singletonList(returned));

        // Try with a duplicate instance name
        Element returned2 = newInstanceElement("type1", "name2");
        when(delegate2.getMetadatas()).thenReturn(Collections.singletonList(returned2));

        List<Element> meta = provider.getMetadatas();
        assertEquals(2, meta.size());
    }

    private Element newComponentElement(String type) {
        Element main = new Element("component", null);
        main.addAttribute(new Attribute("name", type));
        return main;
    }

    private Element newInstanceElement(String type, String name) {
        Element main = new Element("instance", null);
        main.addAttribute(new Attribute("component", type));
        main.addAttribute(new Attribute("name", name));
        return main;
    }
}
