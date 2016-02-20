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
package org.apache.felix.dm.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.ComponentContext;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import test.Ensure;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyImplTest {
    public static interface MyConfiguration {
        String[] getArgArray();

        List<String> getArgList();

        long getLongValue();

        MyMap getMap();

        String getMessage();

        double getPi();

        int getValue();

        boolean isTrue();
    }

    public static interface MyMap {
        String getFoo();

        String[] getQuu();

        int getQux();
    }

    static class AManagedService extends PlainService implements ManagedService {
        public AManagedService(Ensure ensure) {
            super(ensure);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void updated(Dictionary config) throws ConfigurationException {
            super.updated(config);
        }
    }

    static class FancyService {
        final Ensure m_ensure;

        public FancyService(Ensure ensure) {
            m_ensure = ensure;
        }

        public void updated(MyConfiguration config) throws ConfigurationException {
            m_ensure.step();

            if (config != null) {
                assertConfiguration(config);
            }
            else {
                assertNull(config);
            }
        }

        public void stop() {
            m_ensure.step();
        }

        private void assertConfiguration(MyConfiguration cfg) {
            assertEquals("isTrue", true, cfg.isTrue());
            assertEquals("getValue", 42, cfg.getValue());
            assertEquals("getLongValue", 1234567890L, cfg.getLongValue());
            assertEquals("getPi", 3.141, cfg.getPi(), 0.001);
            assertArrayEquals("getArgArray", new String[] { "a", "b", "c" }, cfg.getArgArray());
            assertEquals("getArgList", Arrays.asList("d", "e", "f"), cfg.getArgList());
            assertEquals("getMessage", "hello world!", cfg.getMessage());

            MyMap map = cfg.getMap();
            assertEquals("getMap.getFoo", "bar", map.getFoo());
            assertEquals("getMap.getQux", 123, map.getQux());
            assertArrayEquals("getMap.getQuu", new String[] { "x", "y", "z" }, map.getQuu());
        }
    }

    static class PlainService {
        final Ensure m_ensure;

        public PlainService(Ensure ensure) {
            m_ensure = ensure;
        }

        @SuppressWarnings("rawtypes")
        public void updated(Dictionary config) throws ConfigurationException {
            m_ensure.step();

            if (config != null) {
                assertConfiguration(config);
            }
            else {
                assertNull(config);
            }
        }

        public void stop() {
            m_ensure.step();
        }

        @SuppressWarnings("rawtypes")
        private void assertConfiguration(Dictionary cfg) {
            assertEquals("isTrue", "true", cfg.get("true"));
            assertEquals("getValue", "42", cfg.get("value"));
            assertEquals("getLongValue", "1234567890", cfg.get("longValue"));
            assertEquals("getPi", "3.141", cfg.get("pi"));
            assertEquals("getArgArray", "[a, b, c]", cfg.get("argArray"));
            assertEquals("getArgList", "[d, e, f]", cfg.get("argList"));
            assertEquals("getMap.foo", "bar", cfg.get("map.foo"));
            assertEquals("getMap.qux", "123", cfg.get("map.qux"));
            assertEquals("getMap.quu", "[x, y, z]", cfg.get("map.quu"));
            assertEquals("getMessage", "hello world!", cfg.get("message"));
        }
    }

    @Test
    public void testDoNotInvokeFancyUpdatedMethodWithWrongSignatureOk() throws Exception {
        Ensure ensure = createEnsure();
        FancyService service = new FancyService(ensure);

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.setCallback(service, "updated", Dictionary.class);
        ensure.step(1);

        cdi.updated(createDictionary());

        TimeUnit.SECONDS.sleep(1L);

        // Our step shouldn't be changed...
        ensure.waitForStep(1, 1000);
    }

    @Test
    public void testInvokeFancyUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        FancyService service = new FancyService(ensure);

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.setCallback(service, "updated", MyConfiguration.class);
        cdi.updated(createDictionary());

        ensure.waitForStep(1, 1000);

        cdi.updated(null);

        ensure.waitForStep(2, 1000);
    }

    @Test
    public void testInvokeManagedServiceUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        AManagedService service = new AManagedService(ensure);

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.updated(createDictionary());

        ensure.waitForStep(1, 1000);

        cdi.updated(null);

        ensure.waitForStep(2, 1000);
    }

    @Test
    public void testInvokePlainUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        PlainService service = new PlainService(ensure);

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.updated(createDictionary());

        ensure.waitForStep(1, 1000);

        cdi.updated(null);

        ensure.waitForStep(2, 1000);
    }

    private ConfigurationDependencyImpl createConfigurationDependency(Object service) {
        BundleContext bc = mock(BundleContext.class);
        Logger mockLogger = mock(Logger.class);

        ComponentContext component = mock(ComponentContext.class);
        when(component.getExecutor()).thenReturn(Executors.newSingleThreadExecutor());
        when(component.getLogger()).thenReturn(mockLogger);

        ConfigurationDependencyImpl result = new ConfigurationDependencyImpl(bc, mockLogger);
        result.setCallback(service, "updated").setPid("does.not.matter");
        result.setComponentContext(component);
        result.start();
        return result;
    }

    @SuppressWarnings("rawtypes")
    private Dictionary createDictionary() {
        Dictionary<String, Object> result = new Hashtable<>();
        result.put("true", "true");
        result.put("value", "42");
        result.put("longValue", "1234567890");
        result.put("pi", "3.141");
        result.put("argArray", "[a, b, c]");
        result.put("argList", "[d, e, f]");
        result.put("map.foo", "bar");
        result.put("map.qux", "123");
        result.put("map.quu", "[x, y, z]");
        result.put("message", "hello world!");
        return result;
    }

    private Ensure createEnsure() {
        return new Ensure(false);
    }
}
