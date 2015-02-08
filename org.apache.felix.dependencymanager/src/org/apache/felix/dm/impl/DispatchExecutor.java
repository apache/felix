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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.Logger;
import org.osgi.service.log.LogService;

/**   
 * A DispatchExecutor is a queue that can execute FIFO tasks in a shared threadpool configured for the dispatcher.
 * Each task scheduled in a given DispatchExecutor will be executed serially in FIFO order; and multiple 
 * DispatchExecutor instances may each run concurrently with respect to each other.
 * <p>
 * 
 * This class also supports synchronous scheduling, like the @link {@link SerialExecutor} class; and in this case,
 * only one caller thread will execute the tasks scheduled in the DispatchQueue (and the internal 
 * threadpool won't be used).
 * 
 * <p> 
 * 
 * This class is <b>lock free</b> by design and ensures <b>"safe object publication"</b> between scheduling threads and
 * actual executing thread: if one thread T1 schedules a task, but another thread T2 actually 
 * executes it, then all the objects from the T1 thread will be "safely published" to the executing T2 thread.
 * Safe publication is ensured  because we are using a ConcurrentLinkedQueue, and volatile attributes.
 * (see [1], chapter 3.5.3 (Safe publication idioms). 
 * 
 * [1] Java Concurrency In Practice, Addison Wesley
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DispatchExecutor implements Executor, Runnable {
	/**
	 * The threadpool used for the execution of the tasks that are scheduled in this queue.
	 */
	private final Executor m_threadPool;

	/** 
	 * List of tasks scheduled in our queue.
	 */
	protected final ConcurrentLinkedQueue<Runnable> m_tasks = new ConcurrentLinkedQueue<>();

    /**
     * Marker used to remember the id of the thread currently executing this dispatch queue.
     */
    private volatile Thread m_executingThread;

    /** 
     * Flag telling if this dispatch queue is already scheduled for execution in the threadpool.
     */
    private final AtomicBoolean m_scheduled = new AtomicBoolean();

    /** 
	 * Logger used to log exceptions thrown by scheduled tasks. 
	 */
	private final Logger m_logger;
	
	/**
	 * Creates a new DispatchQueue, which can be executed within a fixed thread pool. Multiple queue
	 * can be executed concurrently, but all runnables scheduled in a given queue will be executed serially, 
	 * in FIFO order. 
	 * 
	 * @param threadPool the executor (typically a threadpool) used to execute this DispatchExecutor.
	 * @param logger the Logger used when errors are taking place
	 */
    public DispatchExecutor(Executor threadPool, Logger logger) {
		m_logger = logger;
		m_threadPool = threadPool;
	}
	
    /**
     * Enqueues a task for later execution. You must call {@link #execute()} in order
     * to trigger the actual execution of all scheduled tasks (in FIFO order).
     */
    public void schedule(Runnable task) {
        m_tasks.add(task);
    }

	/**
	 * Submits a task in this queue, and schedule the execution of this DispatchQueue in the threadpool. 	 
	 * The task is immediately executed (inline execution) if the queue is currently being executed by 
	 * the current thread.
	 */
	public void execute(Runnable task) {
	    execute(task, true);
	}
	
	/**
     * Schedules a task in this queue.
     * If the queue is currently being executed by the current thread, then the task is executed immediately.
     * @tasks the task to schedule
     * @threadpool true if the queue should be executed in the threadpool, false if the queue must be executed by
     * only one caller thread.
     */
    public void execute(Runnable task, boolean threadpool) {
        Thread currThread = Thread.currentThread();
        if (m_executingThread == currThread) {
            runTask(task);
        } else {
            schedule(task);
            execute(threadpool);
        }
    }
	
    /**
     * Schedules the execution of this DispatchQueue in the threadpool.
     */
	public void execute() {
	    execute(true);
	}
	
    /**
     * Schedules the execution of this DispatchQueue in the threadpool, or from a single caller thread.
     * 
     * @param threadpool true means the DispatchQueue is executed in the threadpool, false means the queue is executed from the
     * caller thread.
     */
    public void execute(boolean threadpool) {
        if (m_scheduled.compareAndSet(false, true)) { // schedules our run method in the tpool.
            try {
                if (threadpool) {
                    m_threadPool.execute(this);
                } else {
                    run(); // run all queue tasks from the caller thread
                }
            } catch (RejectedExecutionException e) {
                // The threadpool seems stopped (maybe the framework is being stopped). Anyway, just execute our tasks
                // from the current thread.
                run();
            }
        }
    }

	/**
	 * Run all tasks scheduled in this queue, in FIFO order. This method may be executed either in the threadpool, or from
	 * the caller thread.
	 */
	@Override
	public void run() {
        try {
            // We do a memory barrier in order to ensure consistent per-thread
            // memory visibility
            m_executingThread = Thread.currentThread();
            Runnable task;
            while ((task = m_tasks.poll()) != null) {
                runTask(task);
            }
        } finally {
            m_scheduled.set(false);
            m_executingThread = null;
            if (m_tasks.peek() != null) {
                execute();
            }
        }
	}

	/**
	 * Runs a given task
	 * @param task the task to execute
	 */
    private void runTask(Runnable task) {
		try {
		    task.run();
		} catch (Throwable t) {
			m_logger.log(LogService.LOG_ERROR, "Error processing tasks", t);
		}
	}
}
