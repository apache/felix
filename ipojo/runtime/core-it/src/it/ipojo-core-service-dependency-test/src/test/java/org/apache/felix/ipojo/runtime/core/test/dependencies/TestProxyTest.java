package org.apache.felix.ipojo.runtime.core.test.dependencies;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestProxyTest extends Common {


    @Test
    public void testDelegation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name", "FooProvider1-Proxy");
        ComponentInstance fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);


        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceDelegator").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertTrue(cs.check());

        fooProvider1.dispose();
        instance1.dispose();
    }

    @Test
    public void testDelegationOnNullable() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties i1 = new Properties();
        i1.put("instance.name", "DelegatorNullable");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceDelegator").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertFalse(cs.check()); // Nullable.

        instance1.dispose();
    }


    @Test
    public void testGetAndDelegation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name", "FooProvider1-Proxy");
        ComponentInstance fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);


        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceGetAndDelegate").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.


        assertTrue(cs.check());

        fooProvider1.dispose();
        instance1.dispose();
    }

    @Test
    public void testGetAndDelegationOnNullable() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties i1 = new Properties();
        i1.put("instance.name", "DelegatorNullable");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceGetAndDelegate").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertFalse(cs.check()); // Nullable.


        instance1.dispose();
    }

    @Test
    public void testImmediate() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name", "FooProvider1-Proxy");
        ComponentInstance fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);


        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceNoDelegate").createComponentInstance(i1);

        ServiceReference ref = osgiHelper.getServiceReference(CheckService.class.getName(), "(service.pid=Helper)");
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertTrue(cs.check());

        fooProvider1.dispose();
        instance1.dispose();
    }

    @Test
    public void testImmediateNoService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator-with-no-service");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceNoDelegate").createComponentInstance(i1);

        ServiceReference ref = osgiHelper.getServiceReference(CheckService.class.getName(), "(service.pid=Helper)");
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        try {
            cs.getProps();
            fail("Exception expected");
        } catch (RuntimeException e) {
            //OK
        }

        instance1.dispose();
    }

    @Test
    public void testProxyDisabled() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Disable proxy
        System.setProperty(DependencyHandler.PROXY_SETTINGS_PROPERTY, DependencyHandler.PROXY_DISABLED);
        Properties prov = new Properties();
        prov.put("instance.name", "FooProvider1-Proxy");
        ComponentInstance fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);


        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceDelegator").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertFalse(helper.toString().contains("$$Proxy")); // Not a proxy.

        assertTrue(cs.check());

        fooProvider1.dispose();
        instance1.dispose();
        System.setProperty(DependencyHandler.PROXY_SETTINGS_PROPERTY, DependencyHandler.PROXY_ENABLED);

    }

    @Test
    public void testDynamicProxy() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Dynamic proxy
        System.setProperty(DependencyHandler.PROXY_TYPE_PROPERTY, DependencyHandler.DYNAMIC_PROXY);
        Properties prov = new Properties();
        prov.put("instance.name", "FooProvider1-Proxy");
        ComponentInstance fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);


        Properties i1 = new Properties();
        i1.put("instance.name", "Delegator");
        ComponentInstance instance1 = ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.test.components.proxy.CheckServiceDelegator").createComponentInstance(i1);


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertFalse(helper.toString().contains("$$Proxy")); // Dynamic proxy.
        assertTrue(helper.toString().contains("DynamicProxyFactory"));
        assertTrue(helper.hashCode() > 0);

        assertTrue(helper.equals(helper));
        assertFalse(helper.equals(i1)); // This is a quite stupid test...

        assertTrue(cs.check());

        fooProvider1.dispose();
        instance1.dispose();
        System.setProperty(DependencyHandler.PROXY_TYPE_PROPERTY, DependencyHandler.SMART_PROXY);

    }


}
