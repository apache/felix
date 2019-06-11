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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;

public class RequiredConfigurationPluginTrackerTest {

    @Test
    public void test() throws Exception {
        final BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.getService(Mockito.any(ServiceReference.class)))
                .thenReturn(Mockito.mock(ConfigurationPlugin.class));
        Mockito.when(bundleContext.registerService(Mockito.eq(ConfigurationAdmin.class),
                Mockito.any(ServiceFactory.class), (Dictionary) Mockito.any()))
                .thenReturn(Mockito.mock(ServiceRegistration.class));
        final String[] pluginNames = new String[] { "p1", "p3" };

        final AtomicInteger deactivateCount = new AtomicInteger();
        final List<ExtPersistenceManager> activateList = new ArrayList<ExtPersistenceManager>();
        final List<String> propList = new ArrayList<>();

        final ExtPersistenceManager epm = Mockito.mock(ExtPersistenceManager.class);
        Mockito.when(epm.getDelegatee()).thenReturn(epm);

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

            @Override
            public void updateRegisteredConfigurationPlugins(final String propValue) {
                propList.add(propValue);
                super.updateRegisteredConfigurationPlugins(propValue);
            }

        };
        starter.setPersistenceManager(epm);

        final RequiredConfigurationPluginTracker tracker = new RequiredConfigurationPluginTracker(bundleContext,
                starter, pluginNames);

        final ServiceReference<ConfigurationPlugin> r1 = Mockito.mock(ServiceReference.class);
        Mockito.when(r1.getProperty(RequiredConfigurationPluginTracker.PROPERTY_NAME)).thenReturn("p1");
        Mockito.when(r1.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        tracker.addingService(r1);

        final ServiceReference<ConfigurationPlugin> r2 = Mockito.mock(ServiceReference.class);
        Mockito.when(r2.getProperty(RequiredConfigurationPluginTracker.PROPERTY_NAME)).thenReturn("p2");
        Mockito.when(r2.getProperty(Constants.SERVICE_ID)).thenReturn(2L);
        tracker.addingService(r2);

        assertTrue(activateList.isEmpty());
        assertEquals(0, deactivateCount.get());
        assertEquals(2, propList.size());
        assertEquals("p1,p2", propList.get(1));

        final ServiceReference<ConfigurationPlugin> r3 = Mockito.mock(ServiceReference.class);
        Mockito.when(r3.getProperty(RequiredConfigurationPluginTracker.PROPERTY_NAME)).thenReturn("p3");
        Mockito.when(r3.getProperty(Constants.SERVICE_ID)).thenReturn(3L);
        tracker.addingService(r3);

        assertEquals(1, activateList.size());
        assertEquals(0, deactivateCount.get());
        assertEquals(3, propList.size());
        assertEquals("p1,p2,p3", propList.get(2));

        final ServiceReference<ConfigurationPlugin> r4 = Mockito.mock(ServiceReference.class);
        Mockito.when(r4.getProperty(RequiredConfigurationPluginTracker.PROPERTY_NAME)).thenReturn("p4");
        Mockito.when(r4.getProperty(Constants.SERVICE_ID)).thenReturn(4L);
        tracker.addingService(r4);

        assertEquals(1, activateList.size());
        assertEquals(0, deactivateCount.get());
        assertEquals(4, propList.size());
        assertEquals("p1,p2,p3,p4", propList.get(3));

        tracker.removedService(r4, Mockito.mock(ConfigurationPlugin.class));
        assertEquals(1, activateList.size());
        assertEquals(0, deactivateCount.get());
        assertEquals(5, propList.size());
        assertEquals("p1,p2,p3", propList.get(4));

        tracker.removedService(r1, Mockito.mock(ConfigurationPlugin.class));
        assertEquals(1, activateList.size());
        assertEquals(1, deactivateCount.get());
        assertEquals(6, propList.size());
        assertEquals("p2,p3", propList.get(5));
    }
}
