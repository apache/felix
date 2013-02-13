package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

public class TestServiceProdiving extends Common {

    @Test
    public void testProvidesSimple() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesSimple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }

    @Test
    public void testProvidesDouble() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesDouble");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }

    @Test
    public void testProvidesTriple() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesTriple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
    }

    @Test
    public void testProvidesQuatro() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesQuatro");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
        assertTrue("Provides Foo ", list.contains(FooService.class.getName()));
    }

    @Test
    public void testProperties() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesProperties");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        Element[] props = prov.getElements("property");
        assertEquals("Number of properties", props.length, 5);
        //Foo
        Element foo = getPropertyByName(props, "foo");
        assertEquals("Check foo field", "m_foo", foo.getAttribute("field"));
        assertEquals("Check foo name", "foo", foo.getAttribute("name"));
        //Bar
        Element bar = getPropertyByName(props, "bar");
        assertEquals("Check bar field", "bar", bar.getAttribute("field"));
        assertEquals("Check bar value", "4", bar.getAttribute("value"));
        assertEquals("Check mandatory value", "true", bar.getAttribute("mandatory"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));

        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz field", "m_baz", baz.getAttribute("field"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }

    @Test
    public void testStaticProperties() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ProvidesStaticProperties");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        Element[] props = prov.getElements("property");
        assertEquals("Number of properties", props.length, 9);
        //Prop1
        Element foo = getPropertyByName(props, "prop1");
        assertNull(foo.getAttribute("field"));
        assertEquals("prop1", foo.getAttribute("value"));

        //Prop2
        Element prop2 = getPropertyByName(props, "prop2");
        assertNull(prop2.getAttribute("field"));
        assertNull(prop2.getAttribute("value"));

        // Props
        Element prop = getPropertyByName(props, "props");
        assertNull(prop.getAttribute("field"));
        assertEquals("{prop1, prop2}", prop.getAttribute("value"));

        // Mandatory
        Element mandatory = getPropertyByName(props, "mandatory1");
        assertNull(mandatory.getAttribute("field"));
        assertNull(mandatory.getAttribute("value"));
        assertEquals("true", mandatory.getAttribute("mandatory"));

        //Bar
        Element bar = getPropertyByName(props, "bar");
        assertEquals("Check bar field", "bar", bar.getAttribute("field"));
        assertEquals("Check bar value", "4", bar.getAttribute("value"));
        assertEquals("Check mandatory value", "true", bar.getAttribute("mandatory"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));

        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz field", "m_baz", baz.getAttribute("field"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }

    @Test
    public void testServiceController() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.PSServiceController");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        System.out.println(provs[0].toString());
        assertNotNull(provs[0].getElements("controller"));
        assertEquals(1, provs[0].getElements("controller").length);
        assertEquals("false", provs[0].getElements("controller")[0].getAttribute("value"));
    }

    @Test
    public void testServiceControllerWithSpecification() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.PSServiceControllerSpec");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        System.out.println(provs[0].toString());
        assertNotNull(provs[0].getElements("controller"));
        assertEquals(2, provs[0].getElements("controller").length);
        assertEquals("false", provs[0].getElements("controller")[0].getAttribute("value"));
        assertEquals(FooService.class.getName(), provs[0].getElements("controller")[0].getAttribute("specification"));
    }

    private Element getPropertyByName(Element[] props, String name) {
        for (int i = 0; i < props.length; i++) {
            String na = props[i].getAttribute("name");
            String field = props[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return props[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return props[i];
            }
        }
        fail("Property  " + name + " not found");
        return null;
    }


}
