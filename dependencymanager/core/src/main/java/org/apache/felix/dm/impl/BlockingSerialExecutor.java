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
package org.apache.felix.dm.impl;

import java.util.LinkedList;

/**
 * This class allows to serialize the execution of tasks on one single/unique thread. 
 * Other threads are blocked until they are elected for execution. 
 * 
 * <p>note I: when one leader thread executes a task, it does not hold any locks
 * while executing the task, and does not execute tasks scheduled by other threads.
 * 
 * <p>note II: this executor is reentrant: when one task executed by a leader thread
 * reschedule another task, then the task is run immediately.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BlockingSerialExecutor {
    private final LinkedList m_tasksQueue = new LinkedList();
    private Thread m_executingThread = null;

    /**
     * Executes a task exclusively without holding any locks (other concurrent tasks are blocked until the current task is executed).
     * @param task a task to be executed serially, without holding any locks.
     */
    public void execute(Runnable task) {
        boolean releaseLock = false;
        synchronized (this) {
            if (m_executingThread != Thread.currentThread()) {
                m_tasksQueue.addLast(task);
                while (m_tasksQueue.size() > 0 && m_tasksQueue.get(0) != task) {
                    try {
                        // TODO it might make sense to use a maxwait time and throw an exception on timeouts.
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                m_executingThread = Thread.currentThread();
                releaseLock = true;
            }
        }
        try {
            task.run();
        } finally {
            if (releaseLock) {
                synchronized (this) {
                    m_tasksQueue.remove(task);
                    notifyAll();
                    m_executingThread = null;
                }
            }
        }
    }
}
