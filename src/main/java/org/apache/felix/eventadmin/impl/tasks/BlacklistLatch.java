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
package org.apache.felix.eventadmin.impl.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.felix.eventadmin.impl.util.LogWrapper;

/**
 *
 * A latch that checks handlers for blacklisting on an interval.
 *
 */
public class BlacklistLatch {

	private final Semaphore internalSemaphore;

	private final int count;

	private final long timeout;

	private final List<HandlerTask> handlerTasks;

	/**
	 * @param count Number of handlers that must call countdown
	 * @param timeout Timeout in Milliseconds to check for blacklisting handlers
	 */
	public BlacklistLatch(final int count, final long timeout)
	{
		this.handlerTasks = new ArrayList<HandlerTask>(count);
		this.count = count;
		this.timeout = timeout;
		internalSemaphore = new Semaphore(count);
		internalSemaphore.drainPermits();
	}

	/**
	 *
	 * Count down the number of handlers blocking event completion.
	 *
	 */
	public void countDown()
	{
		internalSemaphore.release();
	}

	/**
	 *
	 * Adds a handler task to the timeout based blackout checking.
	 *
	 * @param task
	 */
	public void addToBlacklistCheck(final HandlerTask task)
	{
		this.handlerTasks.add(task);
	}

	/**
	 *
	 * Causes current thread to wait until each handler has called countDown.
	 * Checks on timeout interval to determine if a handler needs blacklisting.
	 *
	 */
	public void awaitAndBlacklistCheck()
	{
		try
        {
        	while(!internalSemaphore.tryAcquire(this.count, this.timeout, TimeUnit.MILLISECONDS))
            {
            	final Iterator<HandlerTask> handlerTaskIt = handlerTasks.iterator();
            	while(handlerTaskIt.hasNext())
            	{
            		HandlerTask currentTask = handlerTaskIt.next();
            		currentTask.checkForBlacklist();
            	}
            }
        }
        catch (final InterruptedException e)
        {
        	LogWrapper.getLogger().log(
                    LogWrapper.LOG_WARNING,
                    "Event Task Processing Interrupted. Events may not be recieved in proper order.");
        }
	}


}
