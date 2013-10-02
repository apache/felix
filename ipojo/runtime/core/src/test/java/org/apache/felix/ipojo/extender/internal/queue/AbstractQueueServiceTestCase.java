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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.Job;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import static org.mockito.Mockito.verify;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 01/10/13
 * Time: 16:36
 */
public class AbstractQueueServiceTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Mock
    private JobInfo m_info;

    @Mock
    private QueueListener m_one;

    @Mock
    private QueueListener m_two;

    private AbstractQueueService m_queueService;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        m_queueService = new TestableAbstractQueueService(m_bundleContext, QueueService.class);
    }

    public void testFireEnlistedJobInfo() throws Exception {
        m_queueService.addQueueListener(m_one);
        m_queueService.addQueueListener(m_two);
        m_queueService.fireEnlistedJobInfo(m_info);
        verify(m_one).enlisted(m_info);
        verify(m_two).enlisted(m_info);
    }

    public void testFireStartedJobInfo() throws Exception {
        m_queueService.addQueueListener(m_one);
        m_queueService.addQueueListener(m_two);
        m_queueService.fireStartedJobInfo(m_info);
        verify(m_one).started(m_info);
        verify(m_two).started(m_info);
    }

    public void testFireExecutedJobInfo() throws Exception {
        m_queueService.addQueueListener(m_one);
        m_queueService.addQueueListener(m_two);
        m_queueService.fireExecutedJobInfo(m_info, "hello");
        verify(m_one).executed(m_info, "hello");
        verify(m_two).executed(m_info, "hello");
    }

    public void testFireFailedJobInfo() throws Exception {
        m_queueService.addQueueListener(m_one);
        m_queueService.addQueueListener(m_two);
        Exception throwable = new Exception();
        m_queueService.fireFailedJobInfo(m_info, throwable);
        verify(m_one).failed(m_info, throwable);
        verify(m_two).failed(m_info, throwable);
    }

    private class TestableAbstractQueueService extends AbstractQueueService {

        public TestableAbstractQueueService(final BundleContext bundleContext, final Class<?> type) {
            super(bundleContext, type);
        }

        public int getFinished() {
            return 0;
        }

        public int getWaiters() {
            return 0;
        }

        public int getCurrents() {
            return 0;
        }

        public List<JobInfo> getWaitersInfo() {
            return null;
        }

        public <T> Future<T> submit(final Job<T> callable, final Callback<T> callback, final String description) {
            return null;
        }

        public <T> Future<T> submit(final Job<T> callable, final String description) {
            return null;
        }

        public <T> Future<T> submit(final Job<T> callable) {
            return null;
        }
    }

}
