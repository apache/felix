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

package org.apache.felix.ipojo.manipulator.render;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class MetadataRendererTestCase extends TestCase {

    private MetadataRenderer renderer;

    @Override
    public void setUp() throws Exception {
        renderer = new MetadataRenderer();
    }

    public void testAddMetadataFilter() throws Exception {

        // Auto remove all elements with a namespace
        renderer.addMetadataFilter(new MetadataFilter() {
            public boolean accept(Element element) {
                return element.getNameSpace() != null;
            }
        });

        Element main = new Element("test", null);
        Element child = new Element("child", "uri");
        main.addElement(child);
        String rendered = renderer.render(main);
        Assert.assertEquals("test { }", rendered);

    }

    public void testRenderElementWithNoNamespace() throws Exception {
        Element main = new Element("test", null);
        String rendered = renderer.render(main);
        Assert.assertEquals("test { }", rendered);
    }

    public void testRenderElementWithEmptyNamespace() throws Exception {
        Element main = new Element("test", "");
        String rendered = renderer.render(main);
        Assert.assertEquals("test { }", rendered);
    }

    public void testRenderElementWithDefaultNamespace() throws Exception {
        // TODO Do we need to strip off default namespace ?
        Element main = new Element("test", "org.apache.felix.ipojo");
        String rendered = renderer.render(main);
        Assert.assertEquals("org.apache.felix.ipojo:test { }", rendered);
    }

    public void testRenderElementWithNamespace() throws Exception {
        Element main = new Element("test", "http://felix.apache.org/ipojo/testing");
        String rendered = renderer.render(main);
        Assert.assertEquals("http://felix.apache.org/ipojo/testing:test { }", rendered);
    }

    public void testRenderElementWithNoNamespaceAttribute() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", "attribute"));
        String rendered = renderer.render(main);
        Assert.assertEquals("test { $name=\"attribute\" }", rendered);
    }

    public void testRenderElementWithNamespaceAttribute() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", "ns-uri", "attribute"));
        String rendered = renderer.render(main);
        Assert.assertEquals("test { $ns-uri:name=\"attribute\" }", rendered);
    }

    public void testRenderElementWithChildren() throws Exception {
        Element main = new Element("test", null);
        Element child = new Element("child", null);
        main.addElement(child);
        String rendered = renderer.render(main);
        Assert.assertEquals("test { child { }}", rendered);
    }

}
