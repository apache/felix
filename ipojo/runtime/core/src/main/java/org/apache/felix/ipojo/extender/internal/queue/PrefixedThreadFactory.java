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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A thread factory setting the name of the created thread.
 * This thread factory delegates the thread creation on another factory, it just set the thread name (actually just
 * set a prefix).
 */
public class PrefixedThreadFactory implements ThreadFactory {

    /**
     * The wrapped thread factory on which creation is delegated.
     */
    private final ThreadFactory m_threadFactory;

    /**
     * The prefix.
     */
    private final String m_prefix;

    /**
     * Creates the object using the default thread factory.
     *
     * @param prefix the prefix
     */
    public PrefixedThreadFactory(String prefix) {
        this(Executors.defaultThreadFactory(), prefix);
    }

    /**
     * Creates the object delegating to the given thread factory.
     *
     * @param threadFactory the thread factory
     * @param prefix        the prefix
     */
    public PrefixedThreadFactory(ThreadFactory threadFactory, String prefix) {
        m_threadFactory = threadFactory;
        m_prefix = prefix;
    }

    /**
     * Creates a new thread.
     * Prepend the prefix to the thread name
     *
     * @param r the runnable
     * @return the thread object
     */
    public Thread newThread(Runnable r) {
        Thread thread = m_threadFactory.newThread(r);
        thread.setName(m_prefix + thread.getName());
        return thread;
    }
}
