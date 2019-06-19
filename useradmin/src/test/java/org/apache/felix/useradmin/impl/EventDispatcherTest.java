/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;


import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.impl.EventDispatcher;
import org.apache.felix.useradmin.impl.UserAdminListenerList;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * Test case for {@link EventDispatcher}.
 */
public class EventDispatcherTest extends TestCase {
    
    /**
     * Provides a {@link EventAdmin} that counts the number of times 
     * {@link #postEvent(Event)} is called.
     */
    static class CountingEventAdmin implements EventAdmin {
        
        private final CountDownLatch m_latch;
        
        public CountingEventAdmin(CountDownLatch latch) {
            m_latch = latch;
        }
        
        public void postEvent(Event event) {
            if (m_latch != null) {
                m_latch.countDown();
            }
        }

        public void sendEvent(Event event) {
            throw new RuntimeException("Should not be called for asynchronous delivery!");
        }
    }
    
    /**
     * Provides a {@link UserAdminListener} that counts the number of times 
     * {@link #roleChanged(UserAdminEvent)} is called.
     */
    static class CountingUserAdminListener implements UserAdminListener {
        
        private final CountDownLatch m_latch;
        
        public CountingUserAdminListener(CountDownLatch latch) {
            m_latch = latch;
        }

        public void roleChanged(UserAdminEvent event) {
            if (m_latch != null) {
                m_latch.countDown();
            }
        }
    }

    /**
     * Implements a fake service reference.
     */
    static class FakeServiceReference implements ServiceReference {
        
        private final Properties m_props;

        public FakeServiceReference() {
            this(new String[0]);
        }

        public FakeServiceReference(String[] properties) {
            m_props = new Properties();
            
            for (int i = 0; i < properties.length; i += 2) {
                m_props.put(properties[i], properties[i + 1]);
            }
        }
        
        public int compareTo(Object reference) {
            return 0;
        }

        public Bundle getBundle() {
            return null;
        }

        public Object getProperty(String key) {
            return key;
        }

        public String[] getPropertyKeys() {
            return new String[0];
        }

        public Bundle[] getUsingBundles() {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            return false;
        }
    }

    private EventDispatcher m_dispatcher;

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#dispatch(org.osgi.service.useradmin.UserAdminEvent)}.
     */
    public void testDispatchEventCallsEventAdminOk() throws Exception {
        final CountDownLatch latch = new CountDownLatch(5);
        
        m_dispatcher = createEventDispatcher(new CountingEventAdmin(latch));
        m_dispatcher.start();

        for (int i = 0; i < 5; i++) {
            m_dispatcher.dispatch(createMockEvent(UserAdminEvent.ROLE_CHANGED));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#dispatch(org.osgi.service.useradmin.UserAdminEvent)}.
     */
    public void testDispatchEventCallsUserAdminListenersOk() throws Exception {
        final CountDownLatch latch = new CountDownLatch(5);
        
        m_dispatcher = createEventDispatcher(new CountingUserAdminListener(latch));
        m_dispatcher.start();

        for (int i = 0; i < 5; i++) {
            m_dispatcher.dispatch(createMockEvent(UserAdminEvent.ROLE_CHANGED));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#start()}.
     */
    public void testStartTwiceDoesNotMatter() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        
        m_dispatcher = createEventDispatcher(new CountingUserAdminListener(latch));
        m_dispatcher.start();
        
        assertTrue(m_dispatcher.isRunning());

        m_dispatcher.start();
            
        assertTrue(m_dispatcher.isRunning());

        assertEquals("Latch should not be decremented?!", 1L, latch.getCount());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#stop()}.
     */
    public void testStopOk() {
        final CountDownLatch latch = new CountDownLatch(1);
        
        m_dispatcher = createEventDispatcher(new CountingUserAdminListener(latch));
        m_dispatcher.start();

        m_dispatcher.stop();

        m_dispatcher.dispatch(createMockEvent(UserAdminEvent.ROLE_CHANGED));
        
        assertFalse(m_dispatcher.isRunning());

        assertEquals("Latch should not be decremented?!", 1L, latch.getCount());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#stop()}.
     */
    public void testStopUnstartedDoesNotMatter() {
        final CountDownLatch latch = new CountDownLatch(1);
        
        m_dispatcher = createEventDispatcher(new CountingUserAdminListener(latch));
        
        assertFalse(m_dispatcher.isRunning());

        m_dispatcher.stop();
        
        assertFalse(m_dispatcher.isRunning());

        assertEquals("Latch should not be decremented?!", 1L, latch.getCount());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.EventDispatcher#stop()}.
     */
    public void testStopTwiceDoesNotMatter() {
        final CountDownLatch latch = new CountDownLatch(1);
        
        m_dispatcher = createEventDispatcher(new CountingUserAdminListener(latch));
        m_dispatcher.start();
        
        assertTrue(m_dispatcher.isRunning());

        m_dispatcher.stop();
        
        assertFalse(m_dispatcher.isRunning());
        
        m_dispatcher.stop();

        assertEquals("Latch should not be decremented?!", 1L, latch.getCount());
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (m_dispatcher != null) {
            try {
                m_dispatcher.stop();
                m_dispatcher = null;
            } catch (IllegalStateException e) {
                // Not a problem; already stopped...
            }
        }
    }

    /**
     * @param eventAdmin
     * @param listenerList
     * @return
     */
    private EventDispatcher createEventDispatcher(EventAdmin eventAdmin) {
        return new EventDispatcher(eventAdmin, createListenerList());        
    }

    /**
     * @param eventAdmin
     * @param listenerList
     * @return
     */
    private EventDispatcher createEventDispatcher(UserAdminListener listener) {
        return new EventDispatcher(new CountingEventAdmin(null), createListenerList(listener));        
    }
    
    /**
     * @param listener
     * @return
     */
    private UserAdminListenerList createListenerList() {
        return new UserAdminListenerList() {
            public UserAdminListener[] getListeners() {
                return new UserAdminListener[0];
            }
        };
    }

    /**
     * @param listener
     * @return
     */
    private UserAdminListenerList createListenerList(final UserAdminListener listener) {
        return new UserAdminListenerList() {
            public UserAdminListener[] getListeners() {
                return new UserAdminListener[] { listener };
            }
        };
    }
    
    /**
     * @param type
     * @return
     */
    private UserAdminEvent createMockEvent(int type) {
        Role user = RoleFactory.createUser("user-" + System.currentTimeMillis());
        ServiceReference ref = new FakeServiceReference();
        return new UserAdminEvent(ref, type, user);
    }
}
