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
package org.apache.felix.scr.impl;

import junit.framework.TestCase;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ConfigurableComponentHolder;
import org.apache.felix.scr.impl.config.ConfigurationSupport;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationEvent;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Mockito.*;

public class ConfigurationSupportFactoryPidsAndLocationUpdatesTest extends TestCase
{

    public static final String FACTORY_PID_X = "io.fabric8.gateway.http.mapping";
    public static final String PID = "io.fabric8.gateway.http.mapping.68a728ef-7dab-4381-9e63-9c69e128f79a";


    /**
     * This test represents a race condition depending on events ordering that can happen at runtime.
     * A CM_LOCATION_CHANGED event can risk to update the component registry with the wrong pid, with the effect that
     * subsequent CM_UPDATE event will be discarded since not matching the pid.
     *
     * This test simulates this interaction:
     * - verifies that an UPDATE event correctly triggers an update operation
     * - forges a LOCATION_UPDATE event
     * - verifies that after that even, a subsequent UPDATE event still triggers the update operation.
     *
     *  https://issues.apache.org/jira/browse/FELIX-5576
     */
    public void testFactoryPids()
    {
        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);
        when(bundleContext.getBundle()).thenReturn(bundle);

        ComponentRegistry componentRegistry = new ComponentRegistry(bundleContext);

        ComponentMetadata metadata = mock(ComponentMetadata.class);
        BundleComponentActivator activator = mock(BundleComponentActivator.class);
        ComponentHolder holder = spy(new ConfigurableComponentHolder(activator, metadata));

        when(metadata.getConfigurationPid()).thenReturn(PID);
        when(metadata.isConfigurationIgnored()).thenReturn(false);

        when(activator.getBundleContext()).thenReturn(bundleContext);

        componentRegistry.registerComponentHolder(key(1, "bundle1"), holder);

        when(bundleContext.registerService(any(String[].class), any(), any(Dictionary.class))).thenReturn(null);
        //wrong visibility for ConfigurationSupport::getConfigurationInfo, I have to subclass to re-expose
        ConfigurationSupport support = spy(new ConfigurationSupport(bundleContext, componentRegistry){
            ConfigurationInfo configurationInfo;
            @Override
            public ConfigurationInfo getConfigurationInfo(final TargetedPID pid, ComponentHolder componentHolder,
                                                          final BundleContext bundleContext){
                if(this.configurationInfo == null){
                    ConfigurationInfo configurationInfo = mock(ConfigurationInfo.class);
                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    when(configurationInfo.getProps()).thenReturn(props);
                    this.configurationInfo = configurationInfo;
                }

                return configurationInfo;
            }

        });


        ConfigurationEvent event = null;

        event = mockAnEvent(ConfigurationEvent.CM_UPDATED);

        // this represents the race condition
        doReturn(null).when(holder).getConfigurationTargetedPID(any(TargetedPID.class));

        //emit event
        support.configurationEvent(event);

        verify(holder, times(1)).configurationUpdated(any(String.class), any(Dictionary.class), anyLong(), any(TargetedPID.class));

        event = mockAnEvent(ConfigurationEvent.CM_LOCATION_CHANGED);

        //emit event
        support.configurationEvent(event);

        // here we are removing the subbed call for the value seen during race conditions
        Mockito.reset(holder);

        event = mockAnEvent(ConfigurationEvent.CM_UPDATED);

        //emit event
        support.configurationEvent(event);


        try {
            verify(holder, times(1)).configurationUpdated(any(String.class), any(Dictionary.class), anyLong(), any(TargetedPID.class));
        } catch (MockitoAssertionError e) {
            MockitoAssertionError err = new MockitoAssertionError("Was expecting a call to holder.configurationUpdated().");
            err.initCause(e);
            throw err;
        }

    }

    protected ConfigurationEvent mockAnEvent(int eventType) {
        ConfigurationEvent event;
        event = mock(ConfigurationEvent.class);
        when(event.getPid()).thenReturn(FACTORY_PID_X);
        when(event.getFactoryPid()).thenReturn(PID);
        when(event.getType()).thenReturn(eventType);
        return event;
    }

    private static ComponentRegistryKey key( final long bundleId, final String name )
    {
        return new ComponentRegistryKey( new MockBundle()
        {
            //            @Override
            public long getBundleId()
            {
                return bundleId;
            }
        }, name );
    }

}