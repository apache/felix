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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;
import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.getMethodIdentifier;

import org.objectweb.asm.tree.MethodNode;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 24/05/13
 * Time: 09:44
 */
public class NamesTestCase extends TestCase {
    public void testComputeEffectiveMethodNameForNotManipulatedMethod() throws Exception {
        assertEquals("foo", computeEffectiveMethodName("foo"));
    }

    public void testComputeEffectiveMethodNameForManipulatedMethod() throws Exception {
        assertEquals("foo", computeEffectiveMethodName("__M_foo"));
    }

    public void testComputeEffectiveMethodNameForNullInput() throws Exception {
        assertNull(computeEffectiveMethodName(null));
    }

    public void testBindPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "bindService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testUnbindPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "unbindService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testSetPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "setService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testUnsetPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "unsetService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testAddPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "addService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testRemovePatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "removeService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testModifiedPatternRecognition() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "modifiedService";
        assertEquals("Service", getMethodIdentifier(node));
    }

    public void testNoPatternRecognized() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "notify";
        node.desc = "()V";
        assertNull(getMethodIdentifier(node));
    }

    public void testSpecificationRecognized() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Lmy/Service;)V";
        assertEquals("my.Service", getMethodIdentifier(node));
    }

    public void testSpecificationRecognizedWithMap() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Lmy/Service;Ljava/util/Map;)V";
        assertEquals("my.Service", getMethodIdentifier(node));
    }

    public void testSpecificationRecognizedWithDictionary() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Lmy/Service;Ljava/util/Dictionary;)V";
        assertEquals("my.Service", getMethodIdentifier(node));
    }

    public void testSpecificationRecognizedWithServiceReference() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Lmy/Service;Lorg/osgi/framework/ServiceReference;)V";
        assertEquals("my.Service", getMethodIdentifier(node));
    }

    public void testSpecificationRecognizedOnlyMap() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Ljava/util/Map;)V";
        assertNull(getMethodIdentifier(node));
    }

    public void testSpecificationRecognizedOnlyDictionary() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Ljava/util/Dictionary;)V";
        assertNull(getMethodIdentifier(node));

    }

    public void testSpecificationRecognizedOnlyServiceReference() throws Exception {
        MethodNode node = new MethodNode();
        node.name = "handle";
        node.desc = "(Lorg/osgi/framework/ServiceReference;)V";
        assertNull(getMethodIdentifier(node));
    }
}
