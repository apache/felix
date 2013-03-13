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
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class TestTemporalDependencies extends Common {


    @Test
    public void testSimple() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalSimple");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        String to = provs[0].getAttribute("timeout");
        assertNull("No timeout", to);
        String oto = provs[0].getAttribute("onTimeout");
        assertNull("No onTimeout", oto);
    }

    @Test
    public void testTemporal() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.Temporal");
        Element[] provs = meta.getElements("temporal", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        String to = provs[0].getAttribute("timeout");
        assertNull("No timeout", to);
        String oto = provs[0].getAttribute("onTimeout");
        assertNull("No onTimeout", oto);
    }

    @Test
    public void testDI() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithDI");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is the DI", "org.apache.felix.ipojo.runtime.core.test.components.ProvidesSimple", oto);

    }

    @Test
    public void testEmptyArray() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithEmptyArray");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is empty-array", "empty-array", oto);

    }

    @Test
    public void testNull() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithNull");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is null", "null", oto);

    }

    @Test
    public void testNullable() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithNullable");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is nullable", "nullable", oto);

    }

    @Test
    public void testFilter() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithFilter");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String filter = provs[0].getAttribute("filter");
        assertEquals("Filter", "(vendor=clement)", filter);

    }

    @Test
    public void testTimeout() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalWithTimeout");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);

        String to = provs[0].getAttribute("timeout");
        assertEquals("Check timeout", "100", to);

    }

    @Test
    public void testSimpleCollection() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs1");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.runtime.core.test.services.FooService", spec);
    }

    @Test
    public void testCollectionWithTimeout() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs2");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.runtime.core.test.services.FooService", spec);
        String to = dep.getAttribute("timeout");
        assertEquals("Check timeout", "300", to);
    }

    @Test
    public void testCollectionWithPolicy() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs3");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.runtime.core.test.services.FooService", spec);
        String to = dep.getAttribute("ontimeout");
        assertEquals("Check policy", "empty", to);
    }

    @Test
    public void testCollectionWithProxy() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs4");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.runtime.core.test.services.FooService", spec);
        String proxy = dep.getAttribute("proxy");
        assertEquals("Check proxy", "true", proxy);
    }

    private Element getElementPerField(Element elem, String field) {
        Element[] provs = elem.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        for (int i = 0; i < provs.length; i++) {
            if (provs[i].getAttribute("field").equals(field)) {
                return provs[i];
            }
        }
        return null;
    }

}
