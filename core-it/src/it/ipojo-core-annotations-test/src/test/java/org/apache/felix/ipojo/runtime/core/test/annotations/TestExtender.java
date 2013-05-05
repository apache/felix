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

package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestExtender extends Common {

    String type = "org.apache.felix.ipojo.runtime.core.test.components.extender.Extender";
    String namespace = "org.apache.felix.ipojo.extender";


    @Test
    public void testMetadata() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  type);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("extender", namespace);
        assertEquals("Check size", 1, ext.length);
        String extension = ext[0].getAttribute("extension");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");

        assertEquals("Check extension", "foo", extension);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
    }

}
