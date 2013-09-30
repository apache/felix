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

/**
 * A thread factory that groups produced threads inside a given {@link java.lang.ThreadGroup}.
 */
public class GroupThreadFactory implements ThreadFactory {

    /**
     * Group for produced Threads.
     */
    private final ThreadGroup m_threadGroup;

    public GroupThreadFactory() {
        this(defaultThreadGroup());
    }

    /**
     * Returns the default thread group just like {@link java.util.concurrent.Executors#defaultThreadFactory()}.
     */
    private static ThreadGroup defaultThreadGroup() {
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            return s.getThreadGroup();
        } else {
            return Thread.currentThread().getThreadGroup();
        }
    }

    /**
     * @param threadGroup group to be used for produced threads.
     */
    public GroupThreadFactory(final ThreadGroup threadGroup) {
        m_threadGroup = threadGroup;
    }

    /**
     * Creates a new thread.
     * Prepend the prefix to the thread name
     *
     * @param r the runnable
     * @return the thread object
     */
    public Thread newThread(Runnable r) {
        return new Thread(m_threadGroup, r);
    }
}
