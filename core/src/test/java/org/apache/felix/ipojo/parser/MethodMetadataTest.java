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
package org.apache.felix.ipojo.parser;


import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

import java.lang.reflect.Method;

public class MethodMetadataTest extends TestCase {

    public void testIdComputationForArrays() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("withOneArray", new String[0].getClass());
        String id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withOneArray$java_lang_String__", id);

        method = this.getClass().getMethod("withDoubleArray", new String[0][0].getClass());
        id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withDoubleArray$java_lang_String____", id);

        method = this.getClass().getMethod("withTripleArray", new String[0][0][0].getClass());
        id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withTripleArray$java_lang_String______", id);
    }

    public void testIdComputationForArraysUsingPrimitive() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("withPrimitiveArray", new int[0].getClass());
        String id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withPrimitiveArray$int__", id);

        method = this.getClass().getMethod("withPrimitiveArray", new int[0][0].getClass());
        id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withPrimitiveArray$int____", id);

        method = this.getClass().getMethod("withPrimitiveArray", new int[0][0][0].getClass());
        id = MethodMetadata.computeMethodId(method);
        Assert.assertEquals("withPrimitiveArray$int______", id);
    }

    public void testComputationForArraysUsingObjectsFromElement() {
        Element metadata1 = new Element("method", null);
        metadata1.addAttribute(new Attribute("name", "withOneArray"));
        metadata1.addAttribute(new Attribute("arguments", "{java.lang.String[]}"));

        Element metadata2 = new Element("method", null);
        metadata2.addAttribute(new Attribute("name", "withOneArray"));
        metadata2.addAttribute(new Attribute("arguments", "{java.lang.String[][]}"));

        Element metadata3 = new Element("method", null);
        metadata3.addAttribute(new Attribute("name", "withOneArray"));
        metadata3.addAttribute(new Attribute("arguments", "{java.lang.String[][][]}"));

        MethodMetadata methodMetadata1 = new MethodMetadata(metadata1);
        Assert.assertEquals("withOneArray$java_lang_String__", methodMetadata1.getMethodIdentifier());

        MethodMetadata methodMetadata2 = new MethodMetadata(metadata2);
        Assert.assertEquals("withOneArray$java_lang_String____", methodMetadata2.getMethodIdentifier());

        MethodMetadata methodMetadata3 = new MethodMetadata(metadata3);
        Assert.assertEquals("withOneArray$java_lang_String______", methodMetadata3.getMethodIdentifier());
    }

    public void testComputationForArraysUsingPrimitiveFromElement() {
        Element metadata1 = new Element("method", null);
        metadata1.addAttribute(new Attribute("name", "withOneArray"));
        metadata1.addAttribute(new Attribute("arguments", "{int[]}"));

        Element metadata2 = new Element("method", null);
        metadata2.addAttribute(new Attribute("name", "withOneArray"));
        metadata2.addAttribute(new Attribute("arguments", "{int[][]}"));

        Element metadata3 = new Element("method", null);
        metadata3.addAttribute(new Attribute("name", "withOneArray"));
        metadata3.addAttribute(new Attribute("arguments", "{int[][][]}"));

        MethodMetadata methodMetadata1 = new MethodMetadata(metadata1);
        Assert.assertEquals("withOneArray$int__", methodMetadata1.getMethodIdentifier());

        MethodMetadata methodMetadata2 = new MethodMetadata(metadata2);
        Assert.assertEquals("withOneArray$int____", methodMetadata2.getMethodIdentifier());

        MethodMetadata methodMetadata3 = new MethodMetadata(metadata3);
        Assert.assertEquals("withOneArray$int______", methodMetadata3.getMethodIdentifier());
    }


    // Method analyzed for testing

    public void withOneArray(String[] arr) { }
    public void withPrimitiveArray(int[] arr) { }
    public void withDoubleArray(String[][] arr) { }
    public void withPrimitiveArray(int[][] arr) { }
    public void withPrimitiveArray(int[][][] arr) { }
    public void withTripleArray(String[][][] arr) { }

    public MethodMetadataTest() {
        super();
    }

    // Constructor used for testing
    public MethodMetadataTest(String[] arr) { }
    public MethodMetadataTest(String[][] arr) { }
    public MethodMetadataTest(int[] arr) { }
    public MethodMetadataTest(int[][] arr) { }
    public MethodMetadataTest(int[][][] arr) { }


}
