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

package org.apache.felix.ipojo.extender.internal.processor;

import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;

import junit.framework.TestCase;

/**
 * Checks the behavior of the reverse bundle processor.
 */
public class ReverseBundleProcessorTestCase extends TestCase {

    @Mock
    private Bundle m_bundle1;

    @Mock
    private Bundle m_bundle2;

    @Mock
    private Bundle m_bundle3;

    @Mock
    private BundleProcessor m_delegate;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testReverseOrderWhenStopped() throws Exception {
        ReverseBundleProcessor reverse = new ReverseBundleProcessor(m_delegate);
        reverse.activate(m_bundle1);
        reverse.activate(m_bundle2);
        reverse.activate(m_bundle3);

        reverse.stop();

        InOrder order = Mockito.inOrder(m_delegate);
        order.verify(m_delegate).deactivate(m_bundle3);
        order.verify(m_delegate).deactivate(m_bundle2);
        order.verify(m_delegate).deactivate(m_bundle1);

    }

    public void testReverseOrderWhenStoppedAndRemovedElements() throws Exception {
        ReverseBundleProcessor reverse = new ReverseBundleProcessor(m_delegate);
        reverse.activate(m_bundle1);
        reverse.activate(m_bundle2);
        reverse.activate(m_bundle3);

        reverse.deactivate(m_bundle2);

        reverse.stop();

        InOrder order = Mockito.inOrder(m_delegate);
        order.verify(m_delegate).deactivate(m_bundle2);
        order.verify(m_delegate).deactivate(m_bundle3);
        order.verify(m_delegate).deactivate(m_bundle1);

    }
}
