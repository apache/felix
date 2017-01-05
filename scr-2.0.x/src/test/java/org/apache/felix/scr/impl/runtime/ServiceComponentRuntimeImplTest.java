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
package org.apache.felix.scr.impl.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class ServiceComponentRuntimeImplTest extends TestCase
{

    /* in case behavior is supposed to actually involve copying
    public void testCopy()
    {
        ServiceComponentRuntimeImpl scr = new ServiceComponentRuntimeImpl(null, null);
        assertEquals(1, scr.convert(1));
        assertEquals("foo", scr.convert("foo"));
        equalCopy(new int[] {1, 2, 3}, scr);
        equalCopy(new byte[] {1, 2, 3}, scr);
        equalCopy(new String[] {"1", "2", "3"}, scr);
        equalCopy(new Long[] {1l, 2l, 3l}, scr);
    }

    private void equalCopy(Object o1, ServiceComponentRuntimeImpl scr)
    {
        Object o2 = scr.convert(o1);
        assertEquals("expected same length", Array.getLength(o1), Array.getLength(o2));
        assertEquals("expceted same component type", o1.getClass().getComponentType(), o2.getClass().getComponentType());
        for (int i = 0; i < Array.getLength(o1); i++)
        {
            assertEquals("expected same value at " + i, Array.get(o1, i), Array.get(o2, i));
        }

    }
    */

    public void testNullBundleServiceReferenceDTO() throws Exception
    {
        ServiceReference<?> sr = Mockito.mock(ServiceReference.class);
        Mockito.when(sr.getProperty(Constants.SERVICE_ID)).thenReturn(327L);
        Mockito.when(sr.getPropertyKeys()).thenReturn(new String[] {});

        ServiceComponentRuntimeImpl scr = new ServiceComponentRuntimeImpl(null, null);
        Method m = scr.getClass().getDeclaredMethod("serviceReferenceToDTO", ServiceReference.class);
        m.setAccessible(true);
        ServiceReferenceDTO dto = (ServiceReferenceDTO) m.invoke(scr, sr);
        assertEquals(-1, dto.bundle);
    }

    public void testNullBundleServiceReferenceDTO2() throws Exception
    {
        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getBundleId()).thenReturn(42L);

        ServiceReference<?> sr = Mockito.mock(ServiceReference.class);
        Mockito.when(sr.getProperty(Constants.SERVICE_ID)).thenReturn(327L);
        Mockito.when(sr.getPropertyKeys()).thenReturn(new String[] {});
        Mockito.when(sr.getBundle()).thenReturn(b);

        ServiceComponentRuntimeImpl scr = new ServiceComponentRuntimeImpl(null, null);
        Method m = scr.getClass().getDeclaredMethod("serviceReferenceToDTO", ServiceReference.class);
        m.setAccessible(true);
        ServiceReferenceDTO dto = (ServiceReferenceDTO) m.invoke(scr, sr);
        assertEquals(42, dto.bundle);
    }

    public void testConvert()
    {
        ServiceComponentRuntimeImpl scr = new ServiceComponentRuntimeImpl(null, null);
        same("foo", scr);
        same(Boolean.TRUE, scr);
        same(1, scr);
        same(1l, scr);
        same(new ServiceReferenceDTO(), scr);
        same( new String[] {"foo", "bar"}, scr);
        same( new Boolean[] {true, false}, scr);
        same( new Long[] {1l, 2l}, scr);
        same( new DTO[] {new ServiceReferenceDTO(), new BundleDTO()}, scr);
        equalsToString(new int[] {1, 2}, scr);
        equalsToString(Arrays.asList(new int[] {1, 2}), scr);
        equalsToString(Arrays.asList(new String[] {"foo", "bar"}), scr);
    }

    private void equalsToString(Object o, ServiceComponentRuntimeImpl scr)
    {
        assertEquals(String.valueOf(o), scr.convert(o));
    }

    private void same(Object o, ServiceComponentRuntimeImpl scr)
    {
        assertSame(o, scr.convert(o));
    }

}
