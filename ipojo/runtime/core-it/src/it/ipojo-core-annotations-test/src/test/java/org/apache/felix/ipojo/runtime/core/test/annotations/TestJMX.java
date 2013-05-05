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

public class TestJMX extends Common {


    @Test
    public void testDeprecated() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.jmx.JMXDeprecated");
        /*
         * org.apache.felix.ipojo.handlers.jmx:config domain="my-domain" usesmosgi="false"
        org.apache.felix.ipojo.handlers.jmx:property field="m_foo" name="prop" rights="w" notification="true"
        org.apache.felix.ipojo.handlers.jmx:method description="get the foo prop" method="getFoo"
        org.apache.felix.ipojo.handlers.jmx:method description="set the foo prop" method="setFoo"
         */

        Element[] ele = meta.getElements("config", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("ele not null", ele);
        assertEquals("Ele size", 1, ele.length);
        String domain = ele[0].getAttribute("domain");
        String mosgi = ele[0].getAttribute("usesmosgi");
        assertEquals("domain", "my-domain", domain);
        assertEquals("mosgi", "false", mosgi);

        Element[] props = ele[0].getElements("property", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("props not null", props);
        assertEquals("props size", 1, props.length);

        Element[] methods = ele[0].getElements("method", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("methods not null", methods);
        assertEquals("methods size", 2, methods.length);
    }

    @Test
    public void test() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.jmx.JMXSimple");
        /*
         * org.apache.felix.ipojo.handlers.jmx:config domain="my-domain" usesmosgi="false"
        org.apache.felix.ipojo.handlers.jmx:property field="m_foo" name="prop" rights="w" notification="true"
        org.apache.felix.ipojo.handlers.jmx:method description="get the foo prop" method="getFoo"
        org.apache.felix.ipojo.handlers.jmx:method description="set the foo prop" method="setFoo"
         */

        Element[] ele = meta.getElements("config", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("ele not null", ele);
        assertEquals("Ele size", 1, ele.length);
        String domain = ele[0].getAttribute("domain");
        String mosgi = ele[0].getAttribute("usesmosgi");
        assertEquals("domain", "my-domain", domain);
        assertEquals("mosgi", "false", mosgi);

        Element[] props = ele[0].getElements("JMXProperty", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("props not null", props);
        assertEquals("props size", 1, props.length);

        Element[] methods = ele[0].getElements("JMXMethod", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("methods not null", methods);
        assertEquals("methods size", 2, methods.length);
    }

}
