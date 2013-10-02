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

package org.apache.felix.ipojo.extender.internal.queue.debug;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 02/10/13
 * Time: 12:01
 */
public class ReplayQueueEventProxyTestCase extends TestCase {

    @Mock
    private JobInfo m_info1;

    @Mock
    private JobInfo m_info2;

    @Mock
    private QueueListener m_listener;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testEventsAreReplayedOnListenerAddition() throws Exception {
        ReplayQueueEventProxy proxy = new ReplayQueueEventProxy();

        proxy.enlisted(m_info1);
        proxy.started(m_info1);
        proxy.enlisted(m_info2);
        proxy.executed(m_info1, "hello");
        proxy.started(m_info2);
        Exception throwable = new Exception();
        proxy.failed(m_info2, throwable);

        proxy.addQueueListener(m_listener);

        verify(m_listener).enlisted(m_info1);
        verify(m_listener).started(m_info1);
        verify(m_listener).executed(m_info1, "hello");
        verify(m_listener).enlisted(m_info2);
        verify(m_listener).started(m_info2);
        verify(m_listener).failed(m_info2, throwable);

    }

    public void testNoEventsAreSendAfterListenerRemoval() throws Exception {
        ReplayQueueEventProxy proxy = new ReplayQueueEventProxy();

        proxy.enlisted(m_info1);
        proxy.started(m_info1);

        proxy.addQueueListener(m_listener);

        proxy.enlisted(m_info2);

        proxy.removeQueueListener(m_listener);

        proxy.executed(m_info1, "hello");
        proxy.started(m_info2);
        Exception throwable = new Exception();
        proxy.failed(m_info2, throwable);

        // Ensure no methods are called after removal
        verify(m_listener).enlisted(m_info1);
        verify(m_listener).started(m_info1);
        verify(m_listener).enlisted(m_info2);
        verifyNoMoreInteractions(m_listener);

    }

    public void testEventsAreForwarded() throws Exception {
        ReplayQueueEventProxy proxy = new ReplayQueueEventProxy();

        proxy.enlisted(m_info1);
        proxy.started(m_info1);

        proxy.addQueueListener(m_listener);

        proxy.enlisted(m_info2);
        proxy.executed(m_info1, "hello");
        proxy.started(m_info2);
        Exception throwable = new Exception();
        proxy.failed(m_info2, throwable);

        verify(m_listener).enlisted(m_info1);
        verify(m_listener).started(m_info1);
        verify(m_listener).executed(m_info1, "hello");
        verify(m_listener).enlisted(m_info2);
        verify(m_listener).started(m_info2);
        verify(m_listener).failed(m_info2, throwable);

    }
}
