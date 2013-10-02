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

package org.apache.felix.ipojo.extender.internal.queue;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.felix.ipojo.extender.internal.queue.callable.ExceptionCallable;
import org.apache.felix.ipojo.extender.internal.queue.callable.StringCallable;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * Checks the job info callable.
 */
public class JobInfoCallableTestCase extends TestCase {

    @Mock
    private QueueNotifier m_notifier;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testCall() throws Exception {
        Statistic stat = new Statistic();
        long mark = System.currentTimeMillis();
        JobInfoCallable<String> info = new JobInfoCallable<String>(m_notifier, stat, new StringCallable(), null, null);

        // Before execution
        assertTrue(info.getEnlistmentTime() >= mark);
        assertEquals(-1, info.getExecutionDuration());
        assertTrue(info.getWaitDuration() <= (System.currentTimeMillis() - mark));

        assertTrue(stat.getWaiters().contains(info));
        assertEquals(0, stat.getCurrentsCounter().get());
        assertEquals(0, stat.getFinishedCounter().get());

        info.call();

        assertTrue(info.getExecutionDuration() != -1);

        assertTrue(stat.getWaiters().isEmpty());
        assertEquals(0, stat.getCurrentsCounter().get());
        assertEquals(1, stat.getFinishedCounter().get());

        InOrder order = Mockito.inOrder(m_notifier);
        order.verify(m_notifier).fireEnlistedJobInfo(info);
        order.verify(m_notifier).fireStartedJobInfo(info);
        order.verify(m_notifier).fireExecutedJobInfo(info, "hello");
        verifyNoMoreInteractions(m_notifier);

    }

    public void testFailedCall() throws Exception {
        Statistic stat = new Statistic();
        Exception e = new Exception();
        JobInfoCallable<String> info = new JobInfoCallable<String>(m_notifier, stat, new ExceptionCallable(e), null, null);

        try {
            info.call();
        } catch (Exception e1) {
            InOrder order = Mockito.inOrder(m_notifier);
            order.verify(m_notifier).fireEnlistedJobInfo(info);
            order.verify(m_notifier).fireStartedJobInfo(info);
            order.verify(m_notifier).fireFailedJobInfo(info, e);
            verifyNoMoreInteractions(m_notifier);
            return;
        }

        fail("Should have throw an Exception");

    }

    public void testJobInfoType() throws Exception {
        JobInfoCallable<String> info = new JobInfoCallable<String>(m_notifier, new Statistic(), new StringCallable("ipojo.testJobType", "hello"), null, null);
        assertEquals("ipojo.testJobType", info.getJobType());
    }
}
