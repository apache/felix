package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestJMX extends Common {


    @Test
    public void testDeprecated() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.jmx.JMXDeprecated");
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
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.jmx.JMXSimple");
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
