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

import java.util.concurrent.ThreadFactory;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 30/09/13
 * Time: 15:46
 */
public class NamingThreadFactoryTestCase extends TestCase {

    @Override
    public void setUp() throws Exception {
        // Ensure pool numbering starts at 1 for each test
        NamingThreadFactory.reset();
    }

    public void testSequentialThreadNaming() throws Exception {
        NamingThreadFactory factory = new NamingThreadFactory(new DefaultThreadFactory());

        Thread t1 = factory.newThread(new EmptyRunnable());
        Thread t2 = factory.newThread(new EmptyRunnable());
        Thread t3 = factory.newThread(new EmptyRunnable());
        Thread t4 = factory.newThread(new EmptyRunnable());

        assertEquals("pool-1-thread-1", t1.getName());
        assertEquals("pool-1-thread-2", t2.getName());
        assertEquals("pool-1-thread-3", t3.getName());
        assertEquals("pool-1-thread-4", t4.getName());
    }

    public void testMultiThreadNamingFactories() throws Exception {
        NamingThreadFactory factory1 = new NamingThreadFactory(new DefaultThreadFactory());
        NamingThreadFactory factory2 = new NamingThreadFactory(new DefaultThreadFactory());

        // Interleaved invocations
        Thread t21 = factory2.newThread(new EmptyRunnable());
        Thread t11 = factory1.newThread(new EmptyRunnable());
        Thread t12 = factory1.newThread(new EmptyRunnable());
        Thread t13 = factory1.newThread(new EmptyRunnable());
        Thread t22 = factory2.newThread(new EmptyRunnable());
        Thread t23 = factory2.newThread(new EmptyRunnable());
        Thread t14 = factory1.newThread(new EmptyRunnable());
        Thread t24 = factory2.newThread(new EmptyRunnable());

        assertEquals("pool-1-thread-1", t11.getName());
        assertEquals("pool-1-thread-2", t12.getName());
        assertEquals("pool-1-thread-3", t13.getName());
        assertEquals("pool-1-thread-4", t14.getName());

        assertEquals("pool-2-thread-1", t21.getName());
        assertEquals("pool-2-thread-2", t22.getName());
        assertEquals("pool-2-thread-3", t23.getName());
        assertEquals("pool-2-thread-4", t24.getName());
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        public Thread newThread(final Runnable r) {
            return new Thread(r);
        }
    }

    private static class EmptyRunnable implements Runnable {
        public void run() {
            // do nothing
        }
    }
}
