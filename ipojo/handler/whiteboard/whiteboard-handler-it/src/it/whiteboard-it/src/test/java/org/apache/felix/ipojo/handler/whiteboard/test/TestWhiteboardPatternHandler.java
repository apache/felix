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

package org.apache.felix.ipojo.handler.whiteboard.test;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.handler.whiteboard.services.FooService;
import org.apache.felix.ipojo.handler.whiteboard.services.Observable;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class TestWhiteboardPatternHandler extends Common {

    Factory provFactory;
    Factory factory, factory2, factory3, factory4;

    @Before
    public void setUp() {
        provFactory = ipojoHelper.getFactory("fooprovider");
        factory = ipojoHelper.getFactory("under-providers");
        factory2 = ipojoHelper.getFactory("under-properties");
        factory3 = ipojoHelper.getFactory("under-providers-lifecycle");
        factory4 = ipojoHelper.getFactory("under-providers-2");
    }

    @Test
    public void testServiceProviders() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory.createComponentInstance(new Properties());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) osgiHelper.getServiceObject(ref);

        Map map = obs.getObservations();
        assertEquals("Check empty list", ((List) map.get("list")).size(), 0);

        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);

        Properties p2 = new Properties();
        p2.put("foo", "foo");
        ComponentInstance prov2 = provFactory.createComponentInstance(p2);

        map = obs.getObservations();
        assertEquals("Check list #2", ((List) map.get("list")).size(), 2);

        prov1.stop();

        map = obs.getObservations();
        assertEquals("(1) Check list #1", ((List) map.get("list")).size(), 1);

        prov2.stop();

        map = obs.getObservations();
        assertEquals("(2) Check list #0", ((List) map.get("list")).size(), 0);

        prov2.start();

        map = obs.getObservations();
        assertEquals("(3) Check list #1", ((List) map.get("list")).size(), 1);

        prov1.start();

        map = obs.getObservations();
        assertEquals("(4) Check list #2", ((List) map.get("list")).size(), 2);

        prov1.dispose();

        map = obs.getObservations();
        assertEquals("(5) Check list #1", ((List) map.get("list")).size(), 1);

        prov2.dispose();

        map = obs.getObservations();
        assertEquals("(6) Check list #0", ((List) map.get("list")).size(), 0);

        bc.ungetService(ref);
        ci.dispose();
    }

    @Test
    public void testPropertiesProviders() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory2.createComponentInstance(new Properties());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) osgiHelper.getServiceObject(ref);

        Map map = obs.getObservations();
        assertEquals("Check empty list", ((List) map.get("list")).size(), 0);

        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);
        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov1.getInstanceName());
        FooService fs1 = (FooService) osgiHelper.getServiceObject(ref1);

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);

        Properties p2 = new Properties();
        p2.put("foo", "foo");
        ComponentInstance prov2 = provFactory.createComponentInstance(p2);
        ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov2.getInstanceName());
        FooService fs2 = (FooService) osgiHelper.getServiceObject(ref2);

        map = obs.getObservations();
        assertEquals("Check list #2", ((List) map.get("list")).size(), 2);

        fs1.foo();

        map = obs.getObservations();
        assertEquals("(1) Check list #1", ((List) map.get("list")).size(), 1);

        fs2.foo();

        map = obs.getObservations();
        assertEquals("(2) Check list #0", ((List) map.get("list")).size(), 0);

        fs2.foo();

        map = obs.getObservations();
        assertEquals("(3) Check list #1", ((List) map.get("list")).size(), 1);

        fs1.foo();

        map = obs.getObservations();
        assertEquals("(4) Check list #2", ((List) map.get("list")).size(), 2);

        prov1.dispose();

        map = obs.getObservations();
        assertEquals("(5) Check list #1", ((List) map.get("list")).size(), 1);

        prov2.dispose();

        map = obs.getObservations();
        assertEquals("(6) Check list #0", ((List) map.get("list")).size(), 0);

        bc.ungetService(ref1);
        bc.ungetService(ref2);
        bc.ungetService(ref);
        ci.dispose();
    }

    @Test
    public void testModifications() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory.createComponentInstance(new Properties());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) osgiHelper.getServiceObject(ref);

        Map map = obs.getObservations();
        assertEquals("Check empty list", ((List) map.get("list")).size(), 0);

        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #0", ((Integer) map.get("modifications")).intValue(), 0);

        ServiceReference ref2 = osgiHelper.getServiceReference(FooService.class.getName(), null);
        assertNotNull("Check FooService availability", ref2);

        FooService fs = (FooService) osgiHelper.getServiceObject(ref2);
        fs.foo();

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #1 (" + map.get("modifications") + ")", ((Integer) map.get("modifications")).intValue(), 1);

        fs.foo();

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #2", ((Integer) map.get("modifications")).intValue(), 2);

        prov1.dispose();
        map = obs.getObservations();
        assertEquals("Check list #0", ((List) map.get("list")).size(), 0);
        assertEquals("Check modification #2", ((Integer) map.get("modifications")).intValue(), 2);

        bc.ungetService(ref);
        bc.ungetService(ref2);
        ci.dispose();
    }

    @Test
    public void testLifecycleCompliance() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // First expose a service.
        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);

        ComponentInstance ci = factory3.createComponentInstance(new Properties());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) osgiHelper.getServiceObject(ref);

        Map map = obs.getObservations();
        // Check time
        Long validate = (Long) map.get("validate");
        Long arrival = (Long) map.get("arrival");

        // Validate must be call before.
        assertTrue(validate + " <?> " + arrival, validate < arrival);

        prov1.dispose();
        ci.dispose();

    }

    @Test
    public void testServiceProvidersWhiteWhiteboards() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory4.createComponentInstance(new Properties());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) osgiHelper.getServiceObject(ref);

        Map map = obs.getObservations();
        assertEquals("Check empty list", ((List) map.get("list")).size(), 0);

        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);

        map = obs.getObservations();
        assertEquals("Check list #1", ((List) map.get("list")).size(), 1);

        Properties p2 = new Properties();
        p2.put("foo", "foo");
        ComponentInstance prov2 = provFactory.createComponentInstance(p2);

        map = obs.getObservations();
        assertEquals("Check list #2", ((List) map.get("list")).size(), 2);

        prov1.stop();

        map = obs.getObservations();
        assertEquals("(1) Check list #1", ((List) map.get("list")).size(), 1);

        prov2.stop();

        map = obs.getObservations();
        assertEquals("(2) Check list #0", ((List) map.get("list")).size(), 0);

        prov2.start();

        map = obs.getObservations();
        assertEquals("(3) Check list #1", ((List) map.get("list")).size(), 1);

        prov1.start();

        map = obs.getObservations();
        assertEquals("(4) Check list #2", ((List) map.get("list")).size(), 2);

        prov1.dispose();

        map = obs.getObservations();
        assertEquals("(5) Check list #1", ((List) map.get("list")).size(), 1);

        prov2.dispose();

        map = obs.getObservations();
        assertEquals("(6) Check list #0", ((List) map.get("list")).size(), 0);

        bc.ungetService(ref);
        ci.dispose();
    }
}
