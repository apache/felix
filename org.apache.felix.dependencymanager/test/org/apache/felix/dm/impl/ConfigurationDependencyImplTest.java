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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.ComponentContext;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyImplTest {
    public static interface MyMap {
        String getFoo();

        int getQux();

        String[] getQuu();
    }

    public static interface MyConfiguration {
        boolean isTrue();

        int getValue();

        long getLongValue();

        double getPi();

        String[] getArgArray();

        List<String> getArgList();

        MyMap getMap();

        String getMessage();
    }

    static class PlainService {
        final CountDownLatch m_latch = new CountDownLatch(1);

        public void updated(Dictionary config) throws ConfigurationException {
            if (config != null) {
                m_latch.countDown();
            }
            assertConfiguration(config);
        }

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

    static class AManagedService extends PlainService implements ManagedService {
        @Override
        public void updated(Dictionary config) throws ConfigurationException {
            super.updated(config);
        }
    }

    static class FancyService {
        final CountDownLatch m_latch = new CountDownLatch(1);

        public void updated(MyConfiguration config) throws ConfigurationException {
            if (config != null) {
                m_latch.countDown();
            }
            assertConfiguration(config);
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

    @Test
    public void testInvokeManagedServiceUpdatedMethodOk() throws Exception {
        AManagedService service = new AManagedService();

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.updated(createDictionary());

        assertTrue(service.m_latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testInvokePlainUpdatedMethodOk() throws Exception {
        PlainService service = new PlainService();

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.updated(createDictionary());

        assertTrue(service.m_latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testInvokeFancyUpdatedMethodOk() throws Exception {
        FancyService service = new FancyService();

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.setCallback(service, "updated", MyConfiguration.class);
        cdi.updated(createDictionary());

        assertTrue(service.m_latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testDoNotInvokeFancyUpdatedMethodWithWrongSignatureOk() throws Exception {
        FancyService service = new FancyService();

        ConfigurationDependencyImpl cdi = createConfigurationDependency(service);
        cdi.setCallback(service, "updated", Dictionary.class);
        cdi.updated(createDictionary());

        assertFalse(service.m_latch.await(1, TimeUnit.SECONDS));
    }

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

    private ConfigurationDependencyImpl createConfigurationDependency(Object service) {
        Logger mockLogger = mock(Logger.class);

        ComponentContext component = mock(ComponentContext.class);
        when(component.getExecutor()).thenReturn(Executors.newSingleThreadExecutor());
        when(component.getLogger()).thenReturn(mockLogger);

        ConfigurationDependencyImpl result = new ConfigurationDependencyImpl();
        result.setCallback(service, "updated").setPid("does.not.matter");
        result.setComponentContext(component);
        return result;
    }
}
