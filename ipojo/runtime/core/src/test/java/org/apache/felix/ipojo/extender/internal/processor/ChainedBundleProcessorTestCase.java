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

import static org.mockito.Mockito.inOrder;

import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;

import junit.framework.TestCase;

/**
 * Checks the behavior of the chained bundle processor.
 */
public class ChainedBundleProcessorTestCase extends TestCase {

    @Mock
    private BundleProcessor m_delegate1;

    @Mock
    private BundleProcessor m_delegate2;

    @Mock
    private Bundle m_bundle;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testActivationThenDeactivateAreCalledWithReversedProcessorsList() throws Exception {
        ChainedBundleProcessor chain = ChainedBundleProcessor.create(m_delegate1, m_delegate2);

        chain.activate(m_bundle);
        chain.deactivate(m_bundle);

        InOrder order = inOrder(m_delegate1, m_delegate2);
        order.verify(m_delegate1).activate(m_bundle);
        order.verify(m_delegate2).activate(m_bundle);
        order.verify(m_delegate2).deactivate(m_bundle);
        order.verify(m_delegate1).deactivate(m_bundle);
    }

    public void testStartStopIsCalledWithReversedProcessorsList() throws Exception {
        ChainedBundleProcessor chain = ChainedBundleProcessor.create(m_delegate1, m_delegate2);

        chain.start();
        chain.stop();

        InOrder order = inOrder(m_delegate1, m_delegate2);
        order.verify(m_delegate1).start();
        order.verify(m_delegate2).start();
        order.verify(m_delegate2).stop();
        order.verify(m_delegate1).stop();
    }
}
