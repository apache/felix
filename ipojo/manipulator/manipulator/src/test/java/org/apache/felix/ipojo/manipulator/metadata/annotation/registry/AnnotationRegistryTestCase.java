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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import java.util.Collections;

import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay.RootAnnotationRecorder;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 31/05/13
 * Time: 00:18
 */
public class AnnotationRegistryTestCase extends TestCase {
    public void testEmptyRegistry() throws Exception {
        AnnotationRegistry registry = new AnnotationRegistry();
        assertTrue(registry.isUnknown(Type.BOOLEAN_TYPE));
        assertFalse(registry.isStereotype(Type.BOOLEAN_TYPE));
        assertFalse(registry.isUnbound(Type.BOOLEAN_TYPE));
    }

    public void testRegistry1() throws Exception {
        AnnotationRegistry registry = new AnnotationRegistry();
        registry.addStereotype(Type.BOOLEAN_TYPE, Collections.<RootAnnotationRecorder>emptyList());

        assertFalse(registry.isUnknown(Type.BOOLEAN_TYPE));
        assertTrue(registry.isStereotype(Type.BOOLEAN_TYPE));
        assertFalse(registry.isUnbound(Type.BOOLEAN_TYPE));
    }

    public void testRegistry2() throws Exception {
        AnnotationRegistry registry = new AnnotationRegistry();
        registry.addUnbound(Type.BOOLEAN_TYPE);

        assertFalse(registry.isUnknown(Type.BOOLEAN_TYPE));
        assertFalse(registry.isStereotype(Type.BOOLEAN_TYPE));
        assertTrue(registry.isUnbound(Type.BOOLEAN_TYPE));
    }
}
