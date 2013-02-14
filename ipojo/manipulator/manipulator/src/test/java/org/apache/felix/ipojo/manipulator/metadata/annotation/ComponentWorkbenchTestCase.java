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

package org.apache.felix.ipojo.manipulator.metadata.annotation;

import junit.framework.TestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.tree.ClassNode;

/**
 * Created with IntelliJ IDEA.
 * User: guillaume
 * Date: 10/12/12
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentWorkbenchTestCase extends TestCase {
    public void testBuildWithNoTopLevelElements() throws Exception {

        ComponentWorkbench workbench = new ComponentWorkbench(null, node());
        Element built = workbench.build();
        assertNull(built);

    }

    public void testSimpleBuild() throws Exception {

        Element root = new Element("root", null);

        ComponentWorkbench workbench = new ComponentWorkbench(null, node());
        workbench.setRoot(root);
        Element built = workbench.build();

        assertEquals("root", built.getName());
        assertNull(built.getNameSpace());
        assertEquals(0, built.getAttributes().length);
        assertEquals(0, built.getElements().length);

    }

    public void testElementsAreHierarchicallyPlaced() throws Exception {

        Element root = new Element("root", null);
        Element child = new Element("child", null);

        ComponentWorkbench workbench = new ComponentWorkbench(null, node());
        workbench.setRoot(root);
        workbench.getElements().put(child, null);

        Element built = workbench.build();

        assertEquals("root", built.getName());
        assertNull(built.getNameSpace());
        assertEquals(0, built.getAttributes().length);
        assertEquals(1, built.getElements().length);

        Element builtChild = built.getElements("child")[0];

        assertEquals("child", builtChild.getName());
        assertNull(builtChild.getNameSpace());
        assertEquals(0, builtChild.getAttributes().length);
        assertEquals(0, builtChild.getElements().length);


    }


    public static ClassNode node() {
        ClassNode node = new ClassNode();
        node.name = "my/Component";
        return node;
    }

}
