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

import static java.lang.String.format;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread factory setting the name of the created thread.
 * This thread factory delegates the thread creation on another factory and format the name.
 */
public class NamingThreadFactory implements ThreadFactory {

    /**
     * Unique identifier generator.
     */
    private static final AtomicInteger IDENTIFIERS = new AtomicInteger(1);

    /**
     * Per-factory counter
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * The wrapped thread factory on which creation is delegated.
     */
    private final ThreadFactory m_threadFactory;

    /**
     * Pool identifier.
     */
    private final int m_identifier;

    /**
     * For test only.
     */
    public static void reset() {
        IDENTIFIERS.set(1);
    }

    /**
     * Creates the object delegating to the given thread factory.
     *
     * @param threadFactory the thread factory
     */
    public NamingThreadFactory(ThreadFactory threadFactory) {
        this(threadFactory, IDENTIFIERS.getAndIncrement());
    }

    /**
     * Creates the object delegating to the given thread factory.
     *
     * @param threadFactory the thread factory
     * @param identifier the pool identifier
     */
    private NamingThreadFactory(final ThreadFactory threadFactory, final int identifier) {
        m_threadFactory = threadFactory;
        m_identifier = identifier;
    }

    /**
     * Creates a new thread.
     * Format the Thread name
     *
     * @param r the runnable
     * @return the thread object
     */
    public Thread newThread(Runnable r) {
        Thread thread = m_threadFactory.newThread(r);
        thread.setName(format("pool-%d-thread-%d", m_identifier, threadNumber.getAndIncrement()));
        return thread;
    }
}
