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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;

import org.apache.felix.ipojo.extender.internal.queue.callable.StringCallable;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import junit.framework.TestCase;

/**
 * Checks the synchronous queue behavior
 */
public class SynchronousQueueServiceTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Mock
    private ServiceRegistration<?> m_registration;

    @Mock
    private Callback<String> m_callback;

    @Captor
    private ArgumentCaptor<JobInfo> infos;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testRegistration() throws Exception {
        SynchronousQueueService queueService = new SynchronousQueueService(m_bundleContext);

        Mockito.<ServiceRegistration<?>>when(m_bundleContext.registerService(eq(QueueService.class.getName()), eq(queueService), any(Dictionary.class))).thenReturn(m_registration);

        queueService.start();

        verify(m_bundleContext).registerService(eq(QueueService.class.getName()), eq(queueService), any(Dictionary.class));

        queueService.stop();

        verify(m_registration).unregister();
    }

    public void testSubmitsAreSequential() throws Exception {

        SynchronousQueueService queueService = new SynchronousQueueService(m_bundleContext);

        queueService.submit(new StringCallable("one"), m_callback, null);
        queueService.submit(new StringCallable("two"), m_callback, null);

        InOrder order = inOrder(m_callback);
        order.verify(m_callback).success(infos.capture(), eq("one"));
        order.verify(m_callback).success(infos.capture(), eq("two"));

        JobInfo one = infos.getAllValues().get(0);
        JobInfo two = infos.getAllValues().get(1);

        assertTrue(two.getEnlistmentTime() >= one.getEndTime());
    }
}
