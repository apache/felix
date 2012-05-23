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

import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.eventadmin.impl.handler.EventHandlerProxy;
import org.osgi.service.event.Event;

import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * This class does the actual work of the synchronous event delivery.
 *
 * This is the heart of the event delivery. If an event is delivered
 * without timeout handling, the event is directly delivered using
 * the calling thread.
 * If timeout handling is enabled, a new thread is taken from the
 * thread pool and this thread is used to deliver the event.
 * The calling thread is blocked until either the deliver is finished
 * or the timeout occurs.
 * <p><tt>
 * Note that in case of a timeout while a task is disabled the thread
 * is released and we spin-off a new thread that resumes the disabled
 * task hence, this is the only place were we break the semantics of
 * the synchronous delivery. While the only one to notice this is the
 * timed-out handler - it is the fault of this handler too (i.e., it
 * blocked the dispatch for to long) but since it will not receive
 * events anymore it will not notice this semantic difference except
 * that it might not see events it already sent before.
 * </tt></pre>
 *
 * If during an event delivery a new event should be delivered from
 * within the event handler, the timeout handler is stopped for the
 * delivery time of the inner event!
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SyncDeliverTasks
{

    /** The thread pool used to spin-off new threads. */
    private final DefaultThreadPool pool;

    private long timeout;

    /**
     * Construct a new sync deliver tasks.
     * @param pool The thread pool used to spin-off new threads.
     */
    public SyncDeliverTasks(final DefaultThreadPool pool, final long timeout)
    {
        this.pool = pool;
        this.update(timeout);
    }

    /**
     * Update the timeout configuration
     */
    public void update(final long timeout)
    {
        this.timeout = timeout;
    }

    /**
     * This method defines if a timeout handling should be used for the
     * task.
     * @param tasks The event handler dispatch tasks to execute
     */
    private boolean useTimeout(final EventHandlerProxy proxy)
    {
        // we only check the proxy if a timeout is configured
        if ( this.timeout > 0)
        {
            return proxy.useTimeout();
        }
        return false;
    }

    /**
     * This blocks an unrelated thread used to send a synchronous event until the
     * event is send (or a timeout occurs).
     *
     * @param tasks The event handler dispatch tasks to execute
     *
     */
    public void execute(final Collection tasks, final Event event, final boolean filterAsyncUnordered)
    {
        final Thread sleepingThread = Thread.currentThread();
        final SyncThread syncThread = sleepingThread instanceof SyncThread ? (SyncThread)sleepingThread : null;

        final Iterator i = tasks.iterator();
        while ( i.hasNext() )
        {
            final EventHandlerProxy task = (EventHandlerProxy)i.next();
            if ( !filterAsyncUnordered || task.isAsyncOrderedDelivery() )
            {
                if ( !useTimeout(task) )
                {
                    // no timeout, we can directly execute
                    task.sendEvent(event);
                }
                else if ( syncThread != null )
                {
                    // if this is a cascaded event, we directly use this thread
                    // otherwise we could end up in a starvation
                    final long startTime = System.currentTimeMillis();
                    task.sendEvent(event);
                    if ( System.currentTimeMillis() - startTime > this.timeout )
                    {
                        task.blackListHandler();
                    }
                }
                else
                {
                    final Rendezvous startBarrier = new Rendezvous();
                    final Rendezvous timerBarrier = new Rendezvous();
                    this.pool.executeTask(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                // notify the outer thread to start the timer
                                startBarrier.waitForRendezvous();
                                // execute the task
                                task.sendEvent(event);
                                // stop the timer
                                timerBarrier.waitForRendezvous();
                            }
                            catch (final IllegalStateException ise)
                            {
                                // this can happen on shutdown, so we ignore it
                            }
                        }
                    });
                    // we wait for the inner thread to start
                    startBarrier.waitForRendezvous();

                    // timeout handling
                    // we sleep for the sleep time
                    // if someone wakes us up it's the finished inner task
                    try
                    {
                        timerBarrier.waitAttemptForRendezvous(this.timeout);
                    }
                    catch (final TimeoutException ie)
                    {
                        // if we timed out, we have to blacklist the handler
                        task.blackListHandler();
                    }

                }
            }
        }
    }
}
