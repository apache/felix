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
package org.apache.felix.scr.impl.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class SingleComponentManagerTest
{
    @Test
    public void testGetService() throws Exception {
        ComponentMetadata cm = new ComponentMetadata(DSVersion.DS13);
        cm.setImplementationClassName("foo.bar.SomeClass");
        cm.validate(null);

        @SuppressWarnings("unchecked")
        ComponentContainer<Object> cc = Mockito.mock(ComponentContainer.class);
        Mockito.when(cc.getComponentMetadata()).thenReturn(cm);

        SingleComponentManager<Object> scm = new SingleComponentManager<Object>(cc, new ComponentMethods()) {
            @Override
            boolean getServiceInternal(ServiceRegistration<Object> serviceRegistration)
            {
                return true;
            }
        };

        BundleContext bc = Mockito.mock(BundleContext.class);
        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getBundleContext()).thenReturn(bc);

        ComponentContextImpl<Object> cci = new ComponentContextImpl<Object>(scm, b, null);
        Object implObj = new Object();
        cci.setImplementationObject(implObj);
        cci.setImplementationAccessible(true);

        Field f = SingleComponentManager.class.getDeclaredField("m_componentContext");
        f.setAccessible(true);
        f.set(scm, cci);

        scm.m_internalEnabled = true;
        assertSame(implObj, scm.getService(null, null));

        Field u = SingleComponentManager.class.getDeclaredField("m_useCount");
        u.setAccessible(true);
        AtomicInteger use = (AtomicInteger) u.get(scm);
        assertEquals(1, use.get());
    }

    @Test
    public void testGetServiceWithNullComponentContext() throws Exception
    {
        ComponentMetadata cm = new ComponentMetadata(DSVersion.DS13);
        cm.setImplementationClassName("foo.bar.SomeClass");
        cm.validate(null);

        @SuppressWarnings("unchecked")
        ComponentContainer<Object> cc = Mockito.mock(ComponentContainer.class);
        Mockito.when(cc.getComponentMetadata()).thenReturn(cm);

        SingleComponentManager<?> scm = new SingleComponentManager<Object>(cc, new ComponentMethods()) {
            @Override
            boolean getServiceInternal(ServiceRegistration<Object> serviceRegistration)
            {
                return true;
            }
        };
        scm.m_internalEnabled = true;
        assertNull("m_componentContext is null, this should not cause an NPE",
                scm.getService(null, null));

        Field u = SingleComponentManager.class.getDeclaredField("m_useCount");
        u.setAccessible(true);
        AtomicInteger use = (AtomicInteger) u.get(scm);
        assertEquals(0, use.get());
    }
}
