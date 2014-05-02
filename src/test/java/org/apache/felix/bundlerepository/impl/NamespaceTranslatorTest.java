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
package org.apache.felix.bundlerepository.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

public class NamespaceTranslatorTest extends TestCase
{
    public void testNamespaceTranslator()
    {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("osgi.wiring.bundle", "bundle");
        expected.put("osgi.wiring.package", "package");
        expected.put("osgi.wiring.host", "fragment");
        expected.put("osgi.service", "service");

        assertEquals(new HashSet<String>(expected.keySet()),
            new HashSet<String>(NamespaceTranslator.getTranslatedOSGiNamespaces()));
        assertEquals(new HashSet<String>(expected.values()),
            new HashSet<String>(NamespaceTranslator.getTranslatedFelixNamespaces()));

        for (Map.Entry<String, String> entry : expected.entrySet())
        {
            assertEquals(entry.getValue(),
                NamespaceTranslator.getFelixNamespace(entry.getKey()));
            assertEquals(entry.getKey(),
                NamespaceTranslator.getOSGiNamespace(entry.getValue()));
        }

        assertEquals("bheuaark", NamespaceTranslator.getFelixNamespace("bheuaark"));
        assertEquals("bheuaark", NamespaceTranslator.getOSGiNamespace("bheuaark"));
    }
}
