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
import org.objectweb.asm.tree.ClassNode;

import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: guillaume
 * Date: 10/10/12
 * Time: 5:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentVisitorTestCase extends TestCase {

    public void testDefaultNameIsClassname() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, clazz());
        ComponentVisitor visitor = new ComponentVisitor(workbench, reporter);
        visitor.visitEnd();

        Element root = workbench.getRoot();
        assertNotNull(root);
        assertEquals("my.Component", root.getAttribute("name"));
    }

    public void testNameAttribute() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, clazz());
        ComponentVisitor visitor = new ComponentVisitor(workbench, reporter);
        visitor.visit("name", "changed");
        visitor.visitEnd();

        Element root = workbench.getRoot();
        assertNotNull(root);
        assertEquals("changed", root.getAttribute("name"));
    }

    public void testPublicFactoryDeprecationSupport() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, clazz());
        ComponentVisitor visitor = new ComponentVisitor(workbench, reporter);
        visitor.visit("public_factory", "false");
        visitor.visitEnd();

        Element root = workbench.getRoot();
        assertNotNull(root);
        assertEquals("false", root.getAttribute("public"));
    }

    public void testFactoryMethodDeprecationSupport() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, clazz());
        ComponentVisitor visitor = new ComponentVisitor(workbench, reporter);
        visitor.visit("factory_method", "create");
        visitor.visitEnd();

        Element root = workbench.getRoot();
        assertNotNull(root);
        assertEquals("create", root.getAttribute("factory-method"));
    }

    private ClassNode clazz() {
        ClassNode node = new ClassNode();
        node.name = "my/Component";
        return node;
    }
}
