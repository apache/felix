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
package org.apache.felix.configurator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigUtilTest {

    private String getFilterString(final String pid) {
        return "(" + Constants.SERVICE_PID + "=" + pid + ")";
    }

    @Test public void testGetNoCreate() throws Exception {
        final String pid = "a.b";
        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(null);
        assertNull(ConfigUtil.getOrCreateConfiguration(ca, pid, false));
        verify(ca).listConfigurations(getFilterString(pid));
        verifyNoMoreInteractions(ca);
    }

    @Test public void testGetCreate() throws Exception {
        final String pid = "a.b";
        final Configuration cfg = mock(Configuration.class);
        when(cfg.getPid()).thenReturn(pid);

        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(null);
        when(ca.getConfiguration(pid, "?")).thenReturn(cfg);
        assertEquals(cfg, ConfigUtil.getOrCreateConfiguration(ca, pid, true));
        verify(ca).listConfigurations(getFilterString(pid));
        verify(ca).getConfiguration(pid, "?");
        verifyNoMoreInteractions(ca);
    }

    @Test public void testGetAvailable() throws Exception {
        final String pid = "a.b";
        final Configuration cfg = mock(Configuration.class);
        when(cfg.getPid()).thenReturn(pid);

        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(new Configuration[] {cfg});
        assertEquals(cfg, ConfigUtil.getOrCreateConfiguration(ca, pid, true));
        verify(ca).listConfigurations(getFilterString(pid));
        verifyNoMoreInteractions(ca);
    }

    @Test public void testGetFactoryNoCreate() throws Exception {
        final String pid = "a.b~name";
        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(null);
        assertNull(ConfigUtil.getOrCreateConfiguration(ca, pid, false));
        verify(ca).listConfigurations(getFilterString(pid));
        verifyNoMoreInteractions(ca);
    }

    @Test public void testGetFactoryCreate() throws Exception {
        final String pid = "a.b~name";
        final Configuration cfg = mock(Configuration.class);
        when(cfg.getPid()).thenReturn(pid);

        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(null);
        when(ca.getFactoryConfiguration("a.b", "name", "?")).thenReturn(cfg);
        assertEquals(cfg, ConfigUtil.getOrCreateConfiguration(ca, pid, true));
        verify(ca).listConfigurations(getFilterString(pid));
        verify(ca).getFactoryConfiguration("a.b", "name", "?");
        verifyNoMoreInteractions(ca);
    }

    @Test public void testGetFactoryAvailable() throws Exception {
        final String pid = "a.b~name";
        final Configuration cfg = mock(Configuration.class);
        when(cfg.getPid()).thenReturn(pid);

        final ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.listConfigurations(getFilterString(pid))).thenReturn(new Configuration[] {cfg});
        assertEquals(cfg, ConfigUtil.getOrCreateConfiguration(ca, pid, true));
        verify(ca).listConfigurations(getFilterString(pid));
        verifyNoMoreInteractions(ca);
    }
}
