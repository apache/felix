/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.filestore;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a timer that can be reset.
 */
final class ResettableTimer {
    private final ScheduledExecutorService m_executor;
    private final Runnable m_task;
    private final long m_timeout;
    private final TimeUnit m_timeUnit;
    private final AtomicReference m_futureRef;
    
    /**
     * Creates a new {@link ResettableTimer} calling a given task when a given 
     * timeout exceeds.
     * 
     * @param task the task to execute upon timout, cannot be <code>null</code>;
     * @param timeout the timeout value, > 0;
     * @param unit the time unit of the timeout value, cannot be <code>null</code>.
     */
    public ResettableTimer(Runnable task, long timeout, TimeUnit unit) {
        this(new ScheduledThreadPoolExecutor(1), task, timeout, unit);
    }
    
    /**
     * Creates a new {@link ResettableTimer} calling a given task when a given 
     * timeout exceeds.
     * 
     * @param executor the executor to use to execute the task, cannot be <code>null</code>;
     * @param task the task to execute upon timout, cannot be <code>null</code>;
     * @param timeout the timeout value, > 0;
     * @param unit the time unit of the timeout value, cannot be <code>null</code>.
     */
    public ResettableTimer(ScheduledExecutorService executor, Runnable task, long timeout, TimeUnit unit) {
        if (executor == null) { 
            throw new IllegalArgumentException("Executor cannot be null!");
        }
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null!");
        }
        if (timeout <= 0) { 
            throw new IllegalArgumentException("Timeout cannot be negative!");
        }
        if (unit == null) { 
            throw new IllegalArgumentException("TimeUnit cannot be null!"); 
        }
        
        m_executor = executor;
        m_task = task;
        m_timeout = timeout;
        m_timeUnit = unit;
        
        m_futureRef = new AtomicReference();
    }

    /**
     * Returns the state of this timer.
     * 
     * @return <code>true</code> if this timer is shut down, <code>false</code> otherwise.
     */
    public boolean isShutDown() {
        return m_executor.isShutdown();
    }
    
    /**
     * Schedules the task for execution with the contained timeout. If a task 
     * is already pending or running, it will be cancelled (not interrupted). 
     * The new task will be scheduled to run in now + timeout.
     */
    public ScheduledFuture schedule() {
        ScheduledFuture currentTask = cancelCurrentTask();
        ScheduledFuture newTask = m_executor.schedule(m_task, m_timeout, m_timeUnit);
        m_futureRef.compareAndSet(currentTask, newTask);
        return newTask;
    }
    
    /**
     * Shuts down this timer, allowing any pending tasks to execute. After this 
     * method is called, {@link #schedule()} may no longer be called.
     */
    public void shutDown() {
        m_executor.shutdown();
        try {
            m_executor.awaitTermination(2 * m_timeout, m_timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return the current task, or <code>null</code> if no task is available.
     */
    private ScheduledFuture cancelCurrentTask() {
        ScheduledFuture currentTask = (ScheduledFuture) m_futureRef.get();
        if (currentTask != null) {
            // Doesn't matter for completed tasks...
            currentTask.cancel(false /* mayInterruptIfRunning */);
        }
        return currentTask;
    }
}
