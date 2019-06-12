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
package org.apache.felix.cm.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.cm.impl.persistence.ExtPersistenceManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;


public class ConfigurationAdminStarterTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testWaitingForPlugins() throws Exception {
        final BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.registerService(Mockito.eq(ConfigurationAdmin.class),
                Mockito.any(ServiceFactory.class),
                (Dictionary) Mockito.any())).thenReturn(Mockito.mock(ServiceRegistration.class));

        final ExtPersistenceManager epm = Mockito.mock(ExtPersistenceManager.class);
        Mockito.when(epm.getDelegatee()).thenReturn(epm);

        final AtomicInteger deactivateCount = new AtomicInteger();
        final List<ExtPersistenceManager> activateList = new ArrayList<ExtPersistenceManager>();

        final ConfigurationAdminStarter starter = new ConfigurationAdminStarter(bundleContext) {

            @Override
            public void activate(ExtPersistenceManager pm) {
                activateList.add(pm);
                super.activate(pm);
            }

            @Override
            public void deactivate() {
                deactivateCount.incrementAndGet();
                super.deactivate();
            }

        };
        starter.setPersistenceManager(epm);

        assertTrue(activateList.isEmpty());
        assertEquals(0, deactivateCount.get());

        starter.updatePluginsSet(true);

        assertEquals(0, deactivateCount.get());
        assertEquals(1, activateList.size());
        assertEquals(epm, activateList.get(0));

        starter.updatePluginsSet(true);

        assertEquals(0, deactivateCount.get());
        assertEquals(1, activateList.size());
        assertEquals(epm, activateList.get(0));

        starter.updatePluginsSet(false);
        assertEquals(1, deactivateCount.get());
        assertEquals(1, activateList.size());
        assertEquals(epm, activateList.get(0));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testWaitingForPM() throws Exception {
        final BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.registerService(Mockito.eq(ConfigurationAdmin.class),
                Mockito.any(ServiceFactory.class),
                (Dictionary) Mockito.any())).thenReturn(Mockito.mock(ServiceRegistration.class));

        final ExtPersistenceManager epm = Mockito.mock(ExtPersistenceManager.class);
        Mockito.when(epm.getDelegatee()).thenReturn(epm);

        final AtomicInteger deactivateCount = new AtomicInteger();
        final List<ExtPersistenceManager> activateList = new ArrayList<ExtPersistenceManager>();

        final ConfigurationAdminStarter starter = new ConfigurationAdminStarter(bundleContext) {

            @Override
            public void activate(ExtPersistenceManager pm) {
                activateList.add(pm);
                super.activate(pm);
            }

            @Override
            public void deactivate() {
                deactivateCount.incrementAndGet();
                super.deactivate();
            }

        };
        starter.updatePluginsSet(true);

        assertTrue(activateList.isEmpty());
        assertEquals(0, deactivateCount.get());

        starter.setPersistenceManager(epm);

        assertEquals(0, deactivateCount.get());
        assertEquals(1, activateList.size());
        assertEquals(epm, activateList.get(0));

        starter.unsetPersistenceManager();
        assertEquals(1, deactivateCount.get());
        assertEquals(1, activateList.size());
        assertEquals(epm, activateList.get(0));
    }
}

