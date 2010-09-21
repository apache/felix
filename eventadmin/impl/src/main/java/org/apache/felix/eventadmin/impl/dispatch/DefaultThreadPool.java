/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.eventadmin.impl.dispatch;

import org.apache.felix.eventadmin.impl.tasks.SyncThread;
import org.apache.felix.eventadmin.impl.util.LogWrapper;

import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * A thread pool that allows to execute tasks using pooled threads in order
 * to ease the thread creation overhead.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DefaultThreadPool
    extends PooledExecutor
{

    /**
     * Create a new pool.
     */
    public DefaultThreadPool(final int poolSize, final boolean syncThreads)
    {
   	    super(new LinkedQueue());
   	    if ( syncThreads )
   	    {
            this.setThreadFactory(new ThreadFactory()
            {

                public Thread newThread( final Runnable command )
                {
                    final Thread thread = new SyncThread( command );
                    thread.setPriority( Thread.NORM_PRIORITY );
                    thread.setDaemon( true );

                    return thread;
                }
            });
   	    }
   	    else
   	    {
            this.setThreadFactory(new ThreadFactory()
            {

                public Thread newThread( final Runnable command )
                {
                    final Thread thread = new Thread( command );
                    thread.setPriority( Thread.NORM_PRIORITY );
                    thread.setDaemon( false );

                    return thread;
                }
            });
   	    }
   	    configure(poolSize);
        setKeepAliveTime(60000);
        runWhenBlocked();
    }

    /**
     * Configure a new pool size.
     */
    public void configure(final int poolSize)
    {
        setMinimumPoolSize(poolSize);
        setMaximumPoolSize(poolSize + 10);
    }

    /**
     * Close the pool i.e, stop pooling threads. Note that subsequently, task will
     * still be executed but no pooling is taking place anymore.
     */
    public void close()
    {
        shutdownNow();

	    try
	    {
	        awaitTerminationAfterShutdown();
	    }
	    catch (final InterruptedException ie)
	    {
            // ignore this
	    }
    }

    /**
     * Execute the task in a free thread or create a new one.
     * @param task The task to execute
     */
    public void executeTask(Runnable task)
    {
        try
        {
            super.execute(task);
        }
        catch (Throwable t)
        {
            LogWrapper.getLogger().log(
                    LogWrapper.LOG_WARNING,
                    "Exception: " + t, t);
            // ignore this
        }
    }
}
