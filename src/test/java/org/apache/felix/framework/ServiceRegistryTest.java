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
package org.apache.felix.framework;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.apache.felix.framework.ServiceRegistrationImpl.ServiceReferenceImpl;
import org.apache.felix.framework.ServiceRegistry.ServiceHolder;
import org.apache.felix.framework.ServiceRegistry.UsageCount;
import org.easymock.MockControl;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;

public class ServiceRegistryTest extends TestCase
{
    public void testRegisterEventHookService()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        EventHook hook = new EventHook()
        {
            @Override
            public void event(ServiceEvent event, Collection contexts)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertTrue(sr.getHookRegistry().getHooks(EventHook.class).iterator().next() instanceof ServiceReference);
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(EventHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterEventHookServiceFactory()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {EventHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(EventHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterFindHookService()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        FindHook hook = new FindHook()
        {
            @Override
            public void find(BundleContext context, String name, String filter,
                boolean allServices, Collection references)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {FindHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(FindHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterFindHookServiceFactory()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {FindHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(FindHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterListenerHookService()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        ListenerHook hook = new ListenerHook()
        {
            @Override
            public void added(Collection listeners)
            {
            }

            @Override
            public void removed(Collection listener)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {ListenerHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(ListenerHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterListenerHookServiceFactory()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {ListenerHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(ListenerHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterCombinedService()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        class CombinedService implements ListenerHook, FindHook, EventHook, Runnable
        {
            @Override
            public void added(Collection listeners)
            {
            }

            @Override
            public void removed(Collection listener)
            {
            }

            @Override
            public void find(BundleContext context, String name, String filter,
                    boolean allServices, Collection references)
            {
            }

            @Override
            public void event(ServiceEvent event, Collection contexts)
            {
            }

            @Override
            public void run()
            {
            }

        }
        CombinedService hook = new CombinedService();

        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {
            Runnable.class.getName(),
            ListenerHook.class.getName(),
            FindHook.class.getName(),
            EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(ListenerHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(EventHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHookRegistry().getHooks(FindHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    public void testRegisterPlainService()
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();

        MockControl controlContext = MockControl.createNiceControl(BundleContext.class);
        BundleContext c = (BundleContext) controlContext.getMock();
        controlContext.expectAndReturn(c.getBundle(), b);
        controlContext.replay();

        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        String svcObj = "hello";
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c.getBundle(), new String [] {String.class.getName()}, svcObj, new Hashtable());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Unregistration should have no effect", 0, sr.getHookRegistry().getHooks(EventHook.class).size());
        assertEquals("Unregistration should have no effect", 0, sr.getHookRegistry().getHooks(FindHook.class).size());
        assertEquals("Unregistration should have no effect", 0, sr.getHookRegistry().getHooks(ListenerHook.class).size());
    }

    @SuppressWarnings("unchecked")
    public void testGetService()
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        String svc = "foo";

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(true);
        Mockito.when(reg.getService(b)).thenReturn(svc);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        assertSame(svc, sr.getService(b, ref, false));
    }

    @SuppressWarnings("unchecked")
    public void testGetServiceHolderAwait() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final String svc = "test";

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(true);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        UsageCount uc = sr.obtainUsageCount(b, ref, null, false);

        // Set an empty Service Holder so we can test that it waits.
        final ServiceHolder sh = new ServiceHolder();
        uc.m_svcHolderRef.set(sh);

        final StringBuffer sb = new StringBuffer();
        final AtomicBoolean threadException = new AtomicBoolean(false);
        Thread t = new Thread() {
            @Override
            public void run()
            {
                try { Thread.sleep(250); } catch (InterruptedException e) {}
                sh.m_service = svc;
                if (sb.length() > 0)
                {
                    // Should not have put anything in SB until countDown() was called...
                    threadException.set(true);
                }
                sh.m_latch.countDown();
            }
        };
        assertFalse(t.isInterrupted());
        t.start();

        Object actualSvc = sr.getService(b, ref, false);
        sb.append(actualSvc);

        t.join();
        assertFalse("This thread did not wait until the latch was count down",
                threadException.get());

        assertSame(svc, actualSvc);
    }

    @SuppressWarnings("unchecked")
    public void testGetServicePrototype() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        String svc = "xyz";

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(true);
        Mockito.when(reg.getService(b)).thenReturn(svc);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        assertSame(svc, sr.getService(b, ref, true));

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");
        UsageCount[] uca = inUseMap.get(b);
        assertEquals(1, uca.length);
        assertEquals(1, uca[0].m_serviceObjectsCount.get());

        sr.getService(b, ref, true);
        assertEquals(2, uca[0].m_serviceObjectsCount.get());
    }

    @SuppressWarnings("unchecked")
    public void testGetServiceThreadMarking() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        sr.getService(b, ref, false);

        InOrder inOrder = Mockito.inOrder(reg);
        inOrder.verify(reg, Mockito.times(1)).currentThreadMarked();
        inOrder.verify(reg, Mockito.times(1)).markCurrentThread();
        inOrder.verify(reg, Mockito.times(1)).unmarkCurrentThread();
    }

    @SuppressWarnings("unchecked")
    public void testGetServiceThreadMarking2() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        String svc = "bar";

        Bundle b = Mockito.mock(Bundle.class);

        ServiceRegistrationImpl reg = (ServiceRegistrationImpl) sr.registerService(
                b, new String [] {String.class.getName()}, svc, null);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        reg.markCurrentThread();
        try
        {
            sr.getService(b, ref, false);
            fail("Should have thrown an exception to signal reentrant behaviour");
        }
        catch (ServiceException se)
        {
            assertEquals(ServiceException.FACTORY_ERROR, se.getType());
        }
    }

    @SuppressWarnings("unchecked")
    public void testUngetService() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        UsageCount uc = new UsageCount(ref, false);
        uc.m_svcHolderRef.set(new ServiceHolder());

        inUseMap.put(b, new UsageCount[] {uc});

        assertFalse(sr.ungetService(b, ref, null));
        assertNull(uc.m_svcHolderRef.get());
    }

    @SuppressWarnings("unchecked")
    public void testUngetService2() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        UsageCount uc = new UsageCount(ref, false);
        ServiceHolder sh = new ServiceHolder();
        Object svc = new Object();
        sh.m_service = svc;
        uc.m_svcHolderRef.set(sh);
        uc.m_count.incrementAndGet();

        Mockito.verify(reg, Mockito.never()).
            ungetService(Mockito.isA(Bundle.class), Mockito.any());
        inUseMap.put(b, new UsageCount[] {uc});

        assertTrue(sr.ungetService(b, ref, null));
        assertNull(uc.m_svcHolderRef.get());

        Mockito.verify(reg, Mockito.times(1)).
            ungetService(Mockito.isA(Bundle.class), Mockito.eq(svc));
    }

    @SuppressWarnings("unchecked")
    public void testUngetService3() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(true);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        UsageCount uc = new UsageCount(ref, false);
        uc.m_svcHolderRef.set(new ServiceHolder());
        uc.m_count.set(2);

        inUseMap.put(b, new UsageCount[] {uc});

        assertTrue(sr.ungetService(b, ref, null));
        assertNotNull(uc.m_svcHolderRef.get());
        assertNotNull(inUseMap.get(b));

        Mockito.verify(reg, Mockito.never()).
            ungetService(Mockito.isA(Bundle.class), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    public void testUngetService4() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(false);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        UsageCount uc = new UsageCount(ref, false);
        uc.m_svcHolderRef.set(new ServiceHolder());
        uc.m_count.set(2);

        inUseMap.put(b, new UsageCount[] {uc});

        assertTrue(sr.ungetService(b, ref, null));
        assertNull(uc.m_svcHolderRef.get());

        Mockito.verify(reg, Mockito.never()).
            ungetService(Mockito.isA(Bundle.class), Mockito.any());
    }

    @SuppressWarnings("unchecked")
    public void testUngetService5() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.doThrow(new RuntimeException("Test!")).when(reg).
            ungetService(Mockito.isA(Bundle.class), Mockito.any());

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        String svc = "myService";
        UsageCount uc = new UsageCount(ref, false);
        ServiceHolder sh = new ServiceHolder();
        sh.m_service = svc;
        sh.m_latch.countDown();
        uc.m_svcHolderRef.set(sh);
        uc.m_count.set(1);

        inUseMap.put(b, new UsageCount[] {uc});

        try
        {
            assertTrue(sr.ungetService(b, ref, null));
            fail("Should have propagated the runtime exception");
        }
        catch (RuntimeException re)
        {
            assertEquals("Test!", re.getMessage());
        }
        assertNull(uc.m_svcHolderRef.get());

        Mockito.verify(reg, Mockito.times(1)).ungetService(b, svc);
    }

    public void testUngetServiceThreadMarking()
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        assertFalse("There is no usage count, so this method should return false",
                sr.ungetService(b, ref, null));

        InOrder inOrder = Mockito.inOrder(reg);
        inOrder.verify(reg, Mockito.times(1)).currentThreadMarked();
        inOrder.verify(reg, Mockito.times(1)).markCurrentThread();
        inOrder.verify(reg, Mockito.times(1)).unmarkCurrentThread();
    }

    public void testUngetServiceThreadMarking2()
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.currentThreadMarked()).thenReturn(true);

        ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        try
        {
            sr.ungetService(b, ref, null);
            fail("The thread should be observed as marked and hence throw an exception");
        }
        catch (IllegalStateException ise)
        {
            // good
        }
    }

    public void testObtainUsageCount() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        assertEquals("Precondition", 0, inUseMap.size());

        Bundle b = Mockito.mock(Bundle.class);
        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = sr.obtainUsageCount(b, ref, null, false);
        assertEquals(1, inUseMap.size());
        assertEquals(1, inUseMap.get(b).length);
        assertSame(uc, inUseMap.get(b)[0]);
        assertSame(ref, uc.m_ref);
        assertFalse(uc.m_prototype);

        UsageCount uc2 = sr.obtainUsageCount(b, ref, null, false);
        assertSame(uc, uc2);

        ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        UsageCount uc3 = sr.obtainUsageCount(b, ref2, null, false);
        assertNotSame(uc3, uc2);
        assertSame(ref2, uc3.m_ref);
    }

    public void testObtainUsageCountPrototype() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);
        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = sr.obtainUsageCount(b, ref, null, true);
        assertEquals(1, inUseMap.size());
        assertEquals(1, inUseMap.values().iterator().next().length);

        ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        UsageCount uc2 = sr.obtainUsageCount(b, ref2, null, true);
        assertEquals(1, inUseMap.size());
        assertEquals(2, inUseMap.values().iterator().next().length);
        List<UsageCount> ucl = Arrays.asList(inUseMap.get(b));
        assertTrue(ucl.contains(uc));
        assertTrue(ucl.contains(uc2));
    }

    public void testObtainUsageCountPrototypeUnknownLookup() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);
        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);

        UsageCount uc = new UsageCount(ref, true);
        ServiceHolder sh = new ServiceHolder();
        String svc = "foobar";
        sh.m_service = svc;
        uc.m_svcHolderRef.set(sh);
        inUseMap.put(b, new UsageCount[] {uc});

        assertNull(sr.obtainUsageCount(b, Mockito.mock(ServiceReference.class), null, null));

        UsageCount uc2 = sr.obtainUsageCount(b, ref, svc, null);
        assertSame(uc, uc2);
    }

    public void testObtainUsageCountPrototypeUnknownLookup2() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);
        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);

        UsageCount uc = new UsageCount(ref, false);
        inUseMap.put(b, new UsageCount[] {uc});

        assertNull(sr.obtainUsageCount(b, Mockito.mock(ServiceReference.class), null, null));

        UsageCount uc2 = sr.obtainUsageCount(b, ref, null, null);
        assertSame(uc, uc2);
    }

    @SuppressWarnings("unchecked")
    public void testObtainUsageCountRetry1() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final Bundle b = Mockito.mock(Bundle.class);

        final ConcurrentMap<Bundle, UsageCount[]> orgInUseMap =
            (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        ConcurrentMap<Bundle, UsageCount[]> inUseMap =
            Mockito.mock(ConcurrentMap.class, AdditionalAnswers.delegatesTo(orgInUseMap));
        Mockito.doAnswer(new Answer<UsageCount[]>()
            {
                @Override
                public UsageCount[] answer(InvocationOnMock invocation) throws Throwable
                {
                    // This mimicks another thread putting another UsageCount in concurrently
                    // The putIfAbsent() will fail and it has to retry
                    UsageCount uc = new UsageCount(Mockito.mock(ServiceReference.class), false);
                    UsageCount[] uca = new UsageCount[] {uc};
                    orgInUseMap.put(b, uca);
                    return uca;
                }
            }).when(inUseMap).putIfAbsent(Mockito.any(Bundle.class), Mockito.any(UsageCount[].class));
        setPrivateField(sr, "m_inUseMap", inUseMap);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);

        assertEquals(0, orgInUseMap.size());
        UsageCount uc = sr.obtainUsageCount(b, ref, null, false);
        assertEquals(1, orgInUseMap.size());
        assertEquals(2, orgInUseMap.get(b).length);
        assertSame(ref, uc.m_ref);
        assertFalse(uc.m_prototype);
        List<UsageCount> l = new ArrayList<UsageCount>(Arrays.asList(orgInUseMap.get(b)));
        l.remove(uc);
        assertEquals("There should be one UsageCount left", 1, l.size());
        assertNotSame(ref, l.get(0).m_ref);
    }

    @SuppressWarnings("unchecked")
    public void testObtainUsageCountRetry2() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final Bundle b = Mockito.mock(Bundle.class);

        final ConcurrentMap<Bundle, UsageCount[]> orgInUseMap =
            (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");
        orgInUseMap.put(b, new UsageCount[] {new UsageCount(Mockito.mock(ServiceReference.class), false)});

        ConcurrentMap<Bundle, UsageCount[]> inUseMap =
            Mockito.mock(ConcurrentMap.class, AdditionalAnswers.delegatesTo(orgInUseMap));
        Mockito.doAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable
                {
                    orgInUseMap.remove(b);
                    return false;
                }
            }).when(inUseMap).replace(Mockito.any(Bundle.class),
                    Mockito.any(UsageCount[].class), Mockito.any(UsageCount[].class));
        setPrivateField(sr, "m_inUseMap", inUseMap);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);

        assertEquals("Precondition", 1, inUseMap.size());
        assertEquals("Precondition", 1, inUseMap.values().iterator().next().length);
        assertNotSame("Precondition", ref, inUseMap.get(b)[0].m_ref);
        sr.obtainUsageCount(b, ref, null, false);
        assertEquals(1, inUseMap.size());
        assertEquals(1, inUseMap.values().iterator().next().length);
        assertSame("The old usage count should have been removed by the mock and this one should have been added",
                ref, inUseMap.get(b)[0].m_ref);
    }

    public void testFlushUsageCount() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = new UsageCount(ref, false);
        ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        UsageCount uc2 = new UsageCount(ref2, true);

        inUseMap.put(b, new UsageCount[] {uc, uc2});

        assertEquals("Precondition", 1, inUseMap.size());
        assertEquals("Precondition", 2, inUseMap.values().iterator().next().length);

        sr.flushUsageCount(b, ref, uc);
        assertEquals(1, inUseMap.size());
        assertEquals(1, inUseMap.values().iterator().next().length);
        assertSame(uc2, inUseMap.values().iterator().next()[0]);

        sr.flushUsageCount(b, ref2, uc2);
        assertEquals(0, inUseMap.size());
    }

    public void testFlushUsageCountNullRef() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);
        Bundle b2 = Mockito.mock(Bundle.class);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = new UsageCount(ref, false);
        ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        UsageCount uc2 = new UsageCount(ref2, true);
        ServiceReference<?> ref3 = Mockito.mock(ServiceReference.class);
        UsageCount uc3 = new UsageCount(ref3, true);

        inUseMap.put(b, new UsageCount[] {uc2, uc});
        inUseMap.put(b2, new UsageCount[] {uc3});

        assertEquals("Precondition", 2, inUseMap.size());

        sr.flushUsageCount(b, null, uc);
        assertEquals(2, inUseMap.size());

        sr.flushUsageCount(b, null, uc2);
        assertEquals(1, inUseMap.size());
    }

    public void testFlushUsageCountAlienObject() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = new UsageCount(ref, false);

        inUseMap.put(b, new UsageCount[] {uc});
        assertEquals("Precondition", 1, inUseMap.size());
        assertEquals("Precondition", 1, inUseMap.values().iterator().next().length);

        UsageCount uc2 = new UsageCount(Mockito.mock(ServiceReference.class), false);
        sr.flushUsageCount(b, ref, uc2);
        assertEquals("Should be no changes", 1, inUseMap.size());
        assertEquals("Should be no changes", 1, inUseMap.values().iterator().next().length);
    }

    public void testFlushUsageCountNull() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        @SuppressWarnings("unchecked")
        ConcurrentMap<Bundle, UsageCount[]> inUseMap = (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        Bundle b = Mockito.mock(Bundle.class);
        Bundle b2 = Mockito.mock(Bundle.class);

        ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        UsageCount uc = new UsageCount(ref, false);
        ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        UsageCount uc2 = new UsageCount(ref2, true);
        ServiceReference<?> ref3 = Mockito.mock(ServiceReference.class);
        UsageCount uc3 = new UsageCount(ref3, true);

        inUseMap.put(b, new UsageCount[] {uc2, uc});
        inUseMap.put(b2, new UsageCount[] {uc3});

        assertEquals("Precondition", 2, inUseMap.size());

        sr.flushUsageCount(b, ref, null);
        assertEquals(2, inUseMap.size());

        sr.flushUsageCount(b, ref2, null);
        assertEquals(1, inUseMap.size());

    }

    @SuppressWarnings("unchecked")
    public void testFlushUsageCountRetry() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final Bundle b = Mockito.mock(Bundle.class);
        final ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        final UsageCount uc = new UsageCount(ref, false);
        final ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        final UsageCount uc2 = new UsageCount(ref2, false);

        final ConcurrentMap<Bundle, UsageCount[]> orgInUseMap =
            (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
            Mockito.mock(ConcurrentMap.class, AdditionalAnswers.delegatesTo(orgInUseMap));
        Mockito.doAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable
                {
                    inUseMap.put(b, new UsageCount[] {uc});
                    return false;
                }
            }).when(inUseMap).replace(Mockito.isA(Bundle.class),
                    Mockito.isA(UsageCount[].class), Mockito.isA(UsageCount[].class));
        setPrivateField(sr, "m_inUseMap", inUseMap);

        inUseMap.put(b, new UsageCount[] {uc, uc2});

        sr.flushUsageCount(b, null, uc);

        assertNull("A 'concurrent' process has removed uc2 as well, "
                + "so the entry for 'b' should have been removed",
                inUseMap.get(b));
    }

    public void testFlushUsageCountRetry2() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final Bundle b = Mockito.mock(Bundle.class);
        final ServiceReference<?> ref = Mockito.mock(ServiceReference.class);
        final UsageCount uc = new UsageCount(ref, false);
        final ServiceReference<?> ref2 = Mockito.mock(ServiceReference.class);
        final UsageCount uc2 = new UsageCount(ref2, false);

        final ConcurrentMap<Bundle, UsageCount[]> orgInUseMap =
            (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        final ConcurrentMap<Bundle, UsageCount[]> inUseMap =
            Mockito.mock(ConcurrentMap.class, AdditionalAnswers.delegatesTo(orgInUseMap));
        Mockito.doAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable
                {
                    inUseMap.put(b, new UsageCount[] {uc, uc2});
                    return false;
                }
            }).when(inUseMap).remove(Mockito.isA(Bundle.class), Mockito.isA(UsageCount[].class));
        setPrivateField(sr, "m_inUseMap", inUseMap);

        inUseMap.put(b, new UsageCount[] {uc});

        sr.flushUsageCount(b, null, uc);

        assertEquals(1, inUseMap.get(b).length);
        assertSame(uc2, inUseMap.get(b)[0]);
    }

    public void testGetUngetServiceFactory() throws Exception
    {
        final ServiceRegistry sr = new ServiceRegistry(null, null);
        final Bundle regBundle = Mockito.mock(Bundle.class);
        final ServiceRegistration<?> reg = sr.registerService(regBundle, new String[] {Observer.class.getName()},
                new ServiceFactory<Observer>()
                {

                    final class ObserverImpl implements Observer
                    {
                        private final AtomicInteger counter = new AtomicInteger();
                        public volatile boolean active = true;

                        @Override
                        public void update(Observable o, Object arg)
                        {
                            counter.incrementAndGet();
                            if ( !active )
                            {
                                throw new IllegalArgumentException("Iteration:" + counter.get());
                            }
                        }

                    };

                    @Override
                    public Observer getService(Bundle bundle, ServiceRegistration<Observer> registration)
                    {
                        return new ObserverImpl();
                    }

                    @Override
                    public void ungetService(Bundle bundle, ServiceRegistration<Observer> registration, Observer service)
                    {
                        ((ObserverImpl)service).active = false;
                    }
                }, null);

        final Bundle clientBundle = Mockito.mock(Bundle.class);
        Mockito.when(clientBundle.getBundleId()).thenReturn(42L);

        // check simple get/unget
        final Object obj = sr.getService(clientBundle, reg.getReference(), false);
        assertNotNull(obj);
        assertTrue(obj instanceof Observer);
        ((Observer)obj).update(null, null);
        sr.ungetService(clientBundle, reg.getReference(), null);
        try {
            ((Observer)obj).update(null, null);
            fail();
        }
        catch ( final IllegalArgumentException iae)
        {
            // expected
        }

        // start three threads
        final int MAX_THREADS = 3;
        final int MAX_LOOPS = 50000;
        final CountDownLatch latch = new CountDownLatch(MAX_THREADS);
        final Thread[] threads = new Thread[MAX_THREADS];
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        for(int i=0; i<MAX_THREADS; i++)
        {
            threads[i] = new Thread(new Runnable()
            {

                @Override
                public void run()
                {
                    try
                    {
                        Thread.currentThread().sleep(50);
                    }
                    catch (InterruptedException e1)
                    {
                        // ignore
                    }
                    for(int i=0; i < MAX_LOOPS; i++)
                    {
                        try
                        {
                            final Object obj = sr.getService(clientBundle, reg.getReference(), false);
                            ((Observer)obj).update(null, null);
                            sr.ungetService(clientBundle, reg.getReference(), null);
                        }
                        catch ( final Exception e)
                        {
                            exceptions.add(e);
                        }
                    }
                    latch.countDown();
                }
            });
        }
        for(int i=0; i<MAX_THREADS; i++)
        {
            threads[i].start();
        }

        latch.await();

        List<String> counterValues = new ArrayList<String>();
        for (Exception ex : exceptions)
        {
            counterValues.add(ex.getMessage());
        }

        assertTrue("" + counterValues, exceptions.isEmpty());
    }

    public void testUsageCountCleanup() throws Exception
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);
        Bundle regBundle = Mockito.mock(Bundle.class);

        ServiceRegistration<?> reg = sr.registerService(
                regBundle, new String [] {String.class.getName()}, "hi", null);

        final Bundle clientBundle = Mockito.mock(Bundle.class);
        Mockito.when(clientBundle.getBundleId()).thenReturn(327L);

        assertEquals("hi", sr.getService(clientBundle, reg.getReference(), false));
        sr.ungetService(clientBundle, reg.getReference(), null);

        ConcurrentMap<Bundle, UsageCount[]> inUseMap =
                (ConcurrentMap<Bundle, UsageCount[]>) getPrivateField(sr, "m_inUseMap");

        sr.unregisterService(regBundle, reg);
        assertEquals(0, inUseMap.size());
    }

    @SuppressWarnings("unchecked")
    public void testGetServiceThrowsException() throws Exception
    {
        final ServiceRegistry sr = new ServiceRegistry(null, null);

        final Bundle b = Mockito.mock(Bundle.class);
        ServiceRegistrationImpl reg = Mockito.mock(ServiceRegistrationImpl.class);
        Mockito.when(reg.isValid()).thenReturn(true);
        Mockito.when(reg.getService(b)).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(500);
                throw new Exception("boo!");
            }
        });

        final ServiceReferenceImpl ref = Mockito.mock(ServiceReferenceImpl.class);
        Mockito.when(ref.getRegistration()).thenReturn(reg);

        final StringBuffer sb = new StringBuffer();
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    assertEquals("Should not yet have given the service to the other thread",
                            "", sb.toString());
                    sr.getService(b, ref, false);
                }
                catch (Exception e)
                {
                    // We expect an exception here.
                }
            }
        };
        t.start();

        // Wait until the other thread has called getService();
        Thread.sleep(250);

        // This thread has waited long enough for the other thread to call getService()
        // however the actual getService() call blocks long enough for this one to then
        // concurrently call getService() while the other thread is in getService() of the
        // factory. This thread will then end up in m_latch.await().
        // The factory implementation of the other thread then throws an exception. This test
        // ultimately checks that this thread here is not stuck waiting forwever.
        assertNull(sr.getService(b, ref, false));
        sb.append("Obtained service");
    }

    private Object getPrivateField(Object obj, String fieldName) throws NoSuchFieldException,
            IllegalAccessException
    {
        Field f = ServiceRegistry.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }

    private void setPrivateField(ServiceRegistry obj, String fieldName, Object val) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = ServiceRegistry.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
