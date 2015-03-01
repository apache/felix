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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.dm.Logger;
import org.osgi.service.log.LogService;

/**
 * Allows you to enqueue tasks from multiple threads and then execute
 * them on one thread sequentially. It assumes no more than one thread will
 * try to execute the tasks and it will make an effort to pick the first
 * task that comes along whilst making sure subsequent tasks return
 * without waiting. <p>
 * 
 * This class is <b>lock free</b> by design and ensures <b>"safe object publication"</b> between scheduling threads and
 * actual executing thread: if one thread T1 schedules a task, but another thread T2 actually 
 * executes it, then all the objects from the T1 thread will be "safely published" to the executing T2 thread.
 * Safe publication is ensured  because we are using a ConcurrentLinkedQueue.
 * (see [1], chapter 3.5.3 (Safe publication idioms). 
 * 
 * [1] Java Concurrency In Practice, Addison Wesley
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SerialExecutor implements Executor {
    /** 
     * All tasks scheduled are stored there and only one thread may run them.
     **/
    protected final ConcurrentLinkedQueue<Runnable> m_tasks = new ConcurrentLinkedQueue<Runnable>();

    /** 
     * Thread currently executing the task queue. 
     **/
    protected final AtomicReference<Thread> m_runningThread = new AtomicReference<>();

    /** 
     * Logger used when a task execution throws an exception 
     **/
    private final Logger m_logger;

    /**
     * Makes a new SerialExecutor
     * @param logger used when a task execution throws an exception. Can be null if no exception should be logger.
     */
    public SerialExecutor(Logger logger) {
        m_logger = logger;
    }

    /**
     * Enqueues a task for later execution. You must call {@link #execute()} in order
     * to trigger the task execution, which may or may not be executed by
     * your current thread.
     */
    public void schedule(Runnable task) {
        m_tasks.add(task); // No need to synchronize, m_tasks is a concurrent linked queue.
    }

    /**
     * Executes any pending tasks, enqueued using the {@link SerialExecutor#schedule(Runnable)} method. 
     * This method is thread safe, so multiple threads can try to execute the pending
     * tasks, but only the first will be used to actually do so. Other threads will return immediately.
     */
    public void execute() {
        Thread currentThread = Thread.currentThread();
        if (m_runningThread.compareAndSet(null, currentThread)) {
            runTasks(currentThread);
        }
    }

    /**
     * Schedules a task for execution, and then attempts to execute it. This method is thread safe, so 
     * multiple threads can try to execute a task but only the first will be executed, other threads will 
     * return immediately, and the first thread will execute the tasks scheduled by the other threads.<p>
     * <p>
     * This method is reentrant: if the current thread is currently being executed by this executor, then 
     * the task passed to this method will be executed immediately, from the current invoking thread
     * (inline execution).
     */
    public void execute(Runnable task) {
        Thread currentThread = Thread.currentThread();
        if (m_runningThread.get() == currentThread) {
            runTask(task);
        } else {
            schedule(task);  
            execute();
        }
    }
    
    /**
     * Run all pending tasks
     * @param currentRunninghread the current executing thread
     */
    private void runTasks(Thread currentRunninghread) {
        do {
            try {
                Runnable task;
                ConcurrentLinkedQueue<Runnable> tasks = m_tasks;

                while ((task = tasks.poll()) != null) {
                    runTask(task);
                }
            }
            finally {
                m_runningThread.set(null);
            }
        }
        // We must test again if some tasks have been scheduled after our "while" loop above, but before the
        // m_runningThread reference has been reset to null.
        while (!m_tasks.isEmpty() && m_runningThread.compareAndSet(null, currentRunninghread));
    }

    /**
     * Run a given task.
     * @param task the task to execute.
     */
    void runTask(Runnable command) {
        try {
            command.run();
        }
        catch (Throwable t) {
            if (m_logger != null) {
                m_logger.log(LogService.LOG_ERROR, "Error processing tasks", t);
            } else {
                t.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "[Executor: queue size: " + m_tasks.size() + "]";
    }
}