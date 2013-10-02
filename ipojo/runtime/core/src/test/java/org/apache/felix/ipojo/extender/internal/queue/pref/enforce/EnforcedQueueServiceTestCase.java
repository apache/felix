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

package org.apache.felix.ipojo.extender.internal.queue.pref.enforce;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.apache.felix.ipojo.extender.internal.DefaultJob;
import org.apache.felix.ipojo.extender.internal.LifecycleQueueService;
import org.apache.felix.ipojo.extender.internal.queue.callable.StringCallable;
import org.apache.felix.ipojo.extender.internal.queue.pref.Preference;
import org.apache.felix.ipojo.extender.internal.queue.pref.PreferenceSelection;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.apache.felix.ipojo.util.Log;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

import junit.framework.TestCase;

/**
 * Checks the enforced queue service.
 */
public class EnforcedQueueServiceTestCase extends TestCase {

    @Mock
    private LifecycleQueueService delegate;

    @Mock
    private Bundle m_bundle;

    @Mock
    private Log m_log;

    @Mock
    private PreferenceSelection m_selection;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testNoEnforcement() throws Exception {
        when(m_selection.select(m_bundle)).thenReturn(Preference.DEFAULT);
        EnforcedQueueService queueService = new EnforcedQueueService(m_selection, delegate, Preference.ASYNC, m_log);
        queueService.submit(new StringCallable(m_bundle));
        verifyZeroInteractions(m_log);
    }

    public void testIncompatibleEnforcement() throws Exception {
        when(m_selection.select(m_bundle)).thenReturn(Preference.SYNC);
        EnforcedQueueService queueService = new EnforcedQueueService(m_selection, delegate, Preference.ASYNC, m_log);
        queueService.submit(new StringCallable(m_bundle));

        verify(m_log).log(eq(Log.WARNING), anyString());
    }

    public void testCompatibleEnforcement() throws Exception {
        when(m_selection.select(m_bundle)).thenReturn(Preference.ASYNC);
        EnforcedQueueService queueService = new EnforcedQueueService(m_selection, delegate, Preference.ASYNC, m_log);
        queueService.submit(new StringCallable(m_bundle));
        verifyZeroInteractions(m_log);
    }

}
