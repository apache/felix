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
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.manipulation.ClassManipulator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class ManipulatedMetadataFilterTestCase extends TestCase {

    private ManipulatedMetadataFilter filter;

    @Override
    public void setUp() throws Exception {
        filter = new ManipulatedMetadataFilter();
    }

    public void testFilterPrefixedMethod() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", ClassManipulator.PREFIX + "PropertyName"));

        Assert.assertTrue(filter.accept(main));
    }

    public void testFilterInstanceManagerValue() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", InstanceManager.class.getName()));

        Assert.assertTrue(filter.accept(main));
    }

    public void testFilterInstanceManagerSetter() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", "_setInstanceManager"));

        Assert.assertTrue(filter.accept(main));
    }

    public void testDoNotFilterOthers() throws Exception {
        Element main = new Element("test", null);
        main.addAttribute(new Attribute("name", "setPropertyName"));

        Assert.assertFalse(filter.accept(main));
    }
}
