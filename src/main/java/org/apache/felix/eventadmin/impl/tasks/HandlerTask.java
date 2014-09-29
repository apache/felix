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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.apache.felix.eventadmin.impl.handler.EventHandlerProxy;
import org.osgi.service.event.Event;

/**
 * A task that processes an event handler
 *
 */
public class HandlerTask implements Runnable
{
	private final EventHandlerProxy task;

	private final Event event;

	private final long timeout;

	private final BlacklistLatch handlerLatch;

	private volatile long threadId;

	private volatile long startTime;

	private volatile long endTime;

	/**
	 *
	 *
	 * @param task Proxy to the event handler
	 * @param event The event to send to the handler
	 * @param timeout Timeout for handler blacklisting
	 * @param handlerLatch The latch used to ensure events fire in proper order
	 */
	public HandlerTask(final EventHandlerProxy task, final Event event, final long timeout, final BlacklistLatch handlerLatch)
	{
		this.task = task;
		this.event = event;
		this.timeout = timeout;
		this.handlerLatch = handlerLatch;
		this.threadId = -1l;
		this.startTime = -1l;
		this.endTime = -1l;
	}

	/**
	 *
	 * Perform timing based on thread CPU time with clock time fall back.
	 *
	 * @return
	 */
	public long getTimeInMillis()
    {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isThreadCpuTimeEnabled() ?
            bean.getThreadCpuTime(threadId)/1000000 : System.currentTimeMillis();
    }

	/**
	 * Run Hander Event
	 */
    @Override
    public void run()
    {
        try
        {
        	threadId = Thread.currentThread().getId();
            startTime = getTimeInMillis();
            // execute the task
            task.sendEvent(event);
            endTime = getTimeInMillis();
            checkForBlacklist();
        }
        finally
        {
        	handlerLatch.countDown();
        }
    }

    public void runWithoutBlacklistTiming()
    {
    	task.sendEvent(event);
    	handlerLatch.countDown();
    }

    /**
     * This method defines if a timeout handling should be used for the
     * task.
     */
    public boolean useTimeout()
    {
        // we only check the proxy if a timeout is configured
        if ( this.timeout > 0)
        {
            return task.useTimeout();
        }
        return false;
    }

    /**
     * Check to see if we need to blacklist this handler
     *
     */
    public void checkForBlacklist()
    {
    	if(useTimeout() && getTaskTime() > this.timeout)
		{
			task.blackListHandler();
		}
    }

    /**
     *
     * Determine the amount of time spent running this task
     *
     * @return
     */
    public long getTaskTime()
    {
    	if(threadId < 0l || startTime < 0l)
    	{
    		return 0l;
    	}
    	else if(endTime < 0l)
    	{
    		return getTimeInMillis() - startTime;
    	}
    	return endTime - startTime;

    }

}
