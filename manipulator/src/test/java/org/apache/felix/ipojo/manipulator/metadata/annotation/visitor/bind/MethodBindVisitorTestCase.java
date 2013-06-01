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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 22/05/13
 * Time: 17:09
 */
public class MethodBindVisitorTestCase extends TestCase {

    public void testIdentifierProvided() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, type());
        MethodNode node = new MethodNode();
        node.name = "myMethod";

        MethodBindVisitor visitor = new MethodBindVisitor(workbench, Action.BIND, node, reporter);
        visitor.visit("id", "my-identifier");
        visitor.visitEnd();

        assertNotNull(workbench.getIds().get("my-identifier"));
    }

    public void testNoIdentifierButSpecificationAsAttributeProvided() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, type());
        MethodNode node = new MethodNode();
        node.name = "notify";
        node.desc = "()V";

        MethodBindVisitor visitor = new MethodBindVisitor(workbench, Action.BIND, node, reporter);
        visitor.visit("specification", "my.Service");
        visitor.visitEnd();

        assertNotNull(workbench.getIds().get("my.Service"));
    }

    public void testNoIdentifierAndNoSpecificationProvided() throws Exception {
        Reporter reporter = mock(Reporter.class);
        ComponentWorkbench workbench = new ComponentWorkbench(null, type());
        MethodNode node = new MethodNode();
        node.name = "notify";
        node.desc = "()V";

        MethodBindVisitor visitor = new MethodBindVisitor(workbench, Action.BIND, node, reporter);
        visitor.visitEnd();

        verify(reporter).error(anyString(), anyVararg());
    }

    private static ClassNode type() {
        ClassNode node = new ClassNode();
        node.name = "my/Component";
        return node;
    }

}
