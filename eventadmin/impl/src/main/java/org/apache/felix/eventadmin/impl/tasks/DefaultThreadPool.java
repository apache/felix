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
package org.apache.felix.eventadmin.impl.tasks;

<<<<<<< HEAD
import org.apache.felix.eventadmin.impl.util.LogWrapper;

import EDU.oswego.cs.dl.util.concurrent.*;
=======
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.eventadmin.impl.util.LogWrapper;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * A thread pool that allows to execute tasks using pooled threads in order
 * to ease the thread creation overhead.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DefaultThreadPool
<<<<<<< HEAD
    extends PooledExecutor
{

=======
{

    private ExecutorService executor;

    private final ThreadFactory threadFactory;

    private int oldSize = -1;

    private final AtomicLong threadCounter = new AtomicLong(1);

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Create a new pool.
     */
    public DefaultThreadPool(final int poolSize, final boolean syncThreads)
    {
<<<<<<< HEAD
   	    super(new LinkedQueue());
   	    if ( syncThreads )
   	    {
            this.setThreadFactory(new ThreadFactory()
            {

=======
        if ( syncThreads )
        {
            threadFactory = new ThreadFactory()
            {

                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public Thread newThread( final Runnable command )
                {
                    final Thread thread = new SyncThread( command );
                    thread.setPriority( Thread.NORM_PRIORITY );
                    thread.setDaemon( true );

<<<<<<< HEAD
                    return thread;
                }
            });
   	    }
   	    else
   	    {
            this.setThreadFactory(new ThreadFactory()
            {

=======
                    thread.setName("EventAdminThread #" + threadCounter.getAndIncrement());
                    return thread;
                }
            };
        }
        else
        {
            threadFactory = new ThreadFactory()
            {

                @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public Thread newThread( final Runnable command )
                {
                    final Thread thread = new Thread( command );
                    thread.setPriority( Thread.NORM_PRIORITY );
                    thread.setDaemon( true );

<<<<<<< HEAD
                    return thread;
                }
            });
   	    }
   	    configure(poolSize);
        setKeepAliveTime(60000);
        runWhenBlocked();
=======
                    thread.setName("EventAdminAsyncThread #" + threadCounter.getAndIncrement());
                    return thread;
                }
            };
        }
   	    configure(poolSize);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Configure a new pool size.
     */
<<<<<<< HEAD
    public void configure(final int poolSize)
    {
        setMinimumPoolSize(poolSize);
        setMaximumPoolSize(poolSize + 10);
=======
    public synchronized void configure(final int poolSize)
    {
        if ( oldSize != poolSize)
        {
            oldSize = poolSize;
            final ExecutorService oldService = this.executor;
            this.executor = Executors.newFixedThreadPool(poolSize, threadFactory);
            if ( oldService != null )
            {
                oldService.shutdown();
            }
        }
    }

    /**
     * Returns current pool size.
     */
    public int getPoolSize()
    {
    	return oldSize;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Close the pool i.e, stop pooling threads. Note that subsequently, task will
     * still be executed but no pooling is taking place anymore.
     */
    public void close()
    {
<<<<<<< HEAD
        shutdownNow();

=======
        this.executor.shutdownNow();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Execute the task in a free thread or create a new one.
     * @param task The task to execute
<<<<<<< HEAD
     */
    public void executeTask(final Runnable task)
    {
        try
        {
            super.execute(task);
=======
     * @return {@code true} if the task execution could be scheduled, {@code false} otherwise.
     */
    public boolean executeTask(final Runnable task)
    {
        try
        {
            this.executor.submit(task);
        }
        catch ( final RejectedExecutionException ree )
        {
            LogWrapper.getLogger().log(
                    LogWrapper.LOG_WARNING,
                    "Exception: " + ree, ree);
            return false;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        catch (final Throwable t)
        {
            LogWrapper.getLogger().log(
                    LogWrapper.LOG_WARNING,
                    "Exception: " + t, t);
<<<<<<< HEAD
            // ignore this
        }
=======
        }
        return true;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
