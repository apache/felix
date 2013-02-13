package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestFactory extends Common {


    @Test
    public void testArchDeprecated() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.FactoryDeprecated");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "true", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "org.apache.felix.ipojo.runtime.core.test.components.FactoryDeprecated", name);
    }

    @Test
    public void testArch() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.Factory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "true", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "factory", name);
    }

    @Test
    public void testNoArch() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.NoFactory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "false", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "nofactory", name);
    }

    @Test
    public void testFactoryMethod() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.FactoryMethod");
        String method = meta.getAttribute("factory-method");
        assertNotNull("Method exists ", method);
        assertEquals("Method value", "create", method);
    }

    @Test
    public void testFactoryMethodDeprecated() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.FactoryMethodDeprecated");
        String method = meta.getAttribute("factory-method");
        assertNotNull("Method exists ", method);
        assertEquals("Method value", "create", method);
    }

    @Test
    public void testVersion() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.ComponentTypeVersion");
        String version = meta.getAttribute("version");
        assertNotNull("Version exist", version);
        assertEquals("Version value", "1.0.0", version);
    }

    @Test
    public void testNoVersion() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.FactoryMethod");
        String version = meta.getAttribute("version");
        assertNull("No Version", version);
    }


}

