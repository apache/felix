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

import java.util.Collection;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
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
            public void event(ServiceEvent event, Collection contexts)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHooks(EventHook.class).size());
        assertTrue(sr.getHooks(EventHook.class).iterator().next() instanceof ServiceReference);
        assertSame(reg.getReference(), sr.getHooks(EventHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {EventHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHooks(EventHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(EventHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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
            public void find(BundleContext context, String name, String filter,
                boolean allServices, Collection references)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {FindHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(FindHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {FindHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(FindHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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
            public void added(Collection listeners)
            {
            }

            public void removed(Collection listener)
            {
            }
        };

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {ListenerHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(ListenerHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(FindHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {ListenerHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(ListenerHook.class).iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(FindHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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
            public void added(Collection listeners)
            {
            }

            public void removed(Collection listener)
            {
            }

            public void find(BundleContext context, String name, String filter,
                    boolean allServices, Collection references)
            {
            }

            public void event(ServiceEvent event, Collection contexts)
            {
            }

            public void run()
            {
            }

        }
        CombinedService hook = new CombinedService();

        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {
            Runnable.class.getName(),
            ListenerHook.class.getName(),
            FindHook.class.getName(),
            EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getHooks(ListenerHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(ListenerHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getHooks(EventHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(EventHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getHooks(FindHook.class).size());
        assertSame(reg.getReference(), sr.getHooks(FindHook.class).iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getHooks(ListenerHook.class).size());
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
        assertEquals("Precondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Precondition failed", 0, sr.getHooks(ListenerHook.class).size());
        ServiceRegistration reg = sr.registerService(c, new String [] {String.class.getName()}, svcObj, new Hashtable());
        assertEquals("Postcondition failed", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Postcondition failed", 0, sr.getHooks(ListenerHook.class).size());

        sr.unregisterService(b, reg);
        assertEquals("Unregistration should have no effect", 0, sr.getHooks(EventHook.class).size());
        assertEquals("Unregistration should have no effect", 0, sr.getHooks(FindHook.class).size());
        assertEquals("Unregistration should have no effect", 0, sr.getHooks(ListenerHook.class).size());
    }
}
