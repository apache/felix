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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ThreadFactory;

import junit.framework.TestCase;

/**
 * Checks the behavior of the {@link PrefixedThreadFactory}.
 */
public class PrefixedThreadFactoryTestCase extends TestCase {
    public void testNewThread() throws Exception {
        PrefixedThreadFactory factory = new PrefixedThreadFactory("test ");
        Thread t = factory.newThread(mock(Runnable.class));
        assertTrue(t.getName().startsWith("test "));
    }
    public void testNewThreadDelegation() throws Exception {
        ThreadFactory delegate = mock(ThreadFactory.class);
        when(delegate.newThread(any(Runnable.class))).thenReturn(new Thread("thread"));
        PrefixedThreadFactory factory = new PrefixedThreadFactory(delegate, "test ");
        Thread t = factory.newThread(mock(Runnable.class));
        verify(delegate).newThread(any(Runnable.class));
        assertEquals(t.getName(), "test thread");
    }
}
