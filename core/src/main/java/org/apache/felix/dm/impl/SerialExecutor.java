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

import org.osgi.service.log.LogService;

/**
 * Allows you to enqueue tasks from multiple threads and then execute
 * them on one thread sequentially. It assumes more than one thread will
 * try to execute the tasks and it will make an effort to pick the first
 * task that comes along whilst making sure subsequent tasks return
 * without waiting.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class SerialExecutor {
	private static final Runnable DUMMY_RUNNABLE = new Runnable() { public void run() {}; };
    private final LinkedList m_workQueue = new LinkedList();
    private Runnable m_active;
    private volatile Thread m_runningThread;
    private final Logger m_logger;
    
    /**
     * Makes a new SerialExecutor
     * @param logger the logger used to log possible errors thrown by submitted tasks.
     */
    public SerialExecutor(Logger logger) {
        m_logger = logger;
    }
    
    /**
     * Enqueue a new task for later execution. This method is
     * thread-safe, so multiple threads can contribute tasks.
     * 
     * @param runnable the runnable containing the actual task
     */
    public synchronized void enqueue(final Runnable runnable) {
    	m_workQueue.addLast(new Runnable() {
			public void run() {
				try {
					runnable.run();
				}
				catch (Throwable t) {
		            m_logger.log(LogService.LOG_ERROR, "got unexpected exception while executing dependencymanager task:"
		                + toString(), t);
				}
				finally {
					scheduleNext();
				}
			}
			public String toString() { return runnable.toString(); }
		});
    }
    
    /**
     * Execute any pending tasks. This method is thread safe,
     * so multiple threads can try to execute the pending
     * tasks, but only the first will be used to actually do
     * so. Other threads will return immediately.
     */
    public void execute() {
    	Runnable active;
    	synchronized (this) {
    		active = m_active;
    		// for now just put some non-null value in there so we can never
    		// get a race condition when two threads enter this section after
    		// one another (causing sheduleNext() to be invoked twice below)
    		m_active = DUMMY_RUNNABLE;
    	}
    	if (active == null) {
    	    scheduleNext();
    	}
    }
    
    /**
     * Execute a task. This method is thread safe,
     * so multiple threads can try to execute a task
     * but only the first will be executed, other threads will return immediately, and the
     * first thread will execute the tasks scheduled by the other threads.
     */
    public void execute(Runnable task) {
        enqueue(task);
        execute();
    }
    
    /**
     * Immediately execute a task if the current thread is being executed from that executor, else 
     * enqueue the task and try to execute it (same behavior as if the task would have been enqueued / executed).
     */
    public void executeNow(Runnable task) {
        if (Thread.currentThread() == (Thread) m_runningThread) {
        	int queueSize = m_workQueue.size();
        	if (queueSize > 0) {
        		m_logger.log(Logger.LOG_WARNING, task + " is overtaking " + queueSize + " items in the queue..." + m_workQueue);
        	}
            task.run();
        } else {
           enqueue(task);
           execute();
        }
    }

    private void scheduleNext() {
    	Runnable active;
    	synchronized (this) {
			if (!m_workQueue.isEmpty()) {
			    m_runningThread = Thread.currentThread();
				m_active = (Runnable) m_workQueue.removeFirst();
			} else {
	            m_runningThread = null;
			    m_active = null;
			}
    		active = m_active;
    	}
    	if (active != null) {
            active.run();
        }
    }
    
}
