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
package org.apache.felix.scr.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import junit.framework.TestCase;

public class SCRCommandlTest extends TestCase
{
 
    
    public void testPropertyInfo()
    {
        ScrCommand scr = new ScrCommand(null, null, null);
         check(scr, String.format("x Properties:%n    key = [1, 2]%n"), new int[] {1, 2});
         check(scr, String.format("x Properties:%n    key = [1, 2]%n"), new String[] {"1", "2"});
         check(scr, String.format("x Properties:%n    key = [true, false]%n"), new Boolean[] {true, false});
         check(scr, String.format("x Properties:%n    key = foo%n"), "foo");
         check(scr, String.format("x Properties:%n    key = true%n"), true);
    }

    private PrintWriter check(ScrCommand scr, String expected, Object o)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        scr.propertyInfo(Collections.<String, Object>singletonMap("key", o), pw, "", "x");
        assertEquals(expected, sw.toString());
        return pw;
    }
    
}
