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

import java.util.Iterator;
import java.util.List;

import org.apache.felix.eventadmin.impl.dispatch.DefaultThreadPool;

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
public class SyncDeliverTasks implements DeliverTask
{
    /** The thread pool used to spin-off new threads. */
    final DefaultThreadPool m_pool;

    /** The timeout for event handlers, 0 = disabled. */
    long m_timeout;

    /**
     * The matcher interface for checking if timeout handling
     * is disabled for the handler.
     * Matching is based on the class name of the event handler.
     */
    private static interface Matcher
    {
        boolean match(String className);
    }

    /** Match a package. */
    private static final class PackageMatcher implements Matcher
    {
        private final String m_packageName;

        public PackageMatcher(final String name)
        {
            m_packageName = name;
        }
        public boolean match(String className)
        {
            final int pos = className.lastIndexOf('.');
            return pos > -1 && className.substring(0, pos).equals(m_packageName);
        }
    }

    /** Match a package or sub package. */
    private static final class SubPackageMatcher implements Matcher
    {
        private final String m_packageName;

        public SubPackageMatcher(final String name)
        {
            m_packageName = name + '.';
        }
        public boolean match(String className)
        {
            final int pos = className.lastIndexOf('.');
            return pos > -1 && className.substring(0, pos + 1).startsWith(m_packageName);
        }
    }

    /** Match a class name. */
    private static final class ClassMatcher implements Matcher
    {
        private final String m_className;

        public ClassMatcher(final String name)
        {
            m_className = name;
        }
        public boolean match(String className)
        {
            return m_className.equals(className);
        }
    }

    /** The matchers for ignore timeout handling. */
    private Matcher[] m_ignoreTimeoutMatcher;

    /**
     * Construct a new sync deliver tasks.
     * @param pool The thread pool used to spin-off new threads.
     * @param timeout The timeout for an event handler, 0 = disabled
     */
    public SyncDeliverTasks(final DefaultThreadPool pool, final long timeout, final String[] ignoreTimeout)
    {
        m_pool = pool;
        update(timeout, ignoreTimeout);
    }

    public void update(final long timeout, final String[] ignoreTimeout) {
        m_timeout = timeout;
        if ( ignoreTimeout == null || ignoreTimeout.length == 0 )
        {
            m_ignoreTimeoutMatcher = null;
        }
        else
        {
            Matcher[] ignoreTimeoutMatcher = new Matcher[ignoreTimeout.length];
            for(int i=0;i<ignoreTimeout.length;i++)
            {
                String value = ignoreTimeout[i];
                if ( value != null )
                {
                    value = value.trim();
                }
                if ( value != null && value.length() > 0 )
                {
                    if ( value.endsWith(".") )
                    {
                        ignoreTimeoutMatcher[i] = new PackageMatcher(value.substring(0, value.length() - 1));
                    }
                    else if ( value.endsWith("*") )
                    {
                        ignoreTimeoutMatcher[i] = new SubPackageMatcher(value.substring(0, value.length() - 1));
                    }
                    else
                    {
                        ignoreTimeoutMatcher[i] = new ClassMatcher(value);
                    }
                }
            }
            m_ignoreTimeoutMatcher = ignoreTimeoutMatcher;
        }
    }

    /**
     * This method defines if a timeout handling should be used for the
     * task.
     * @param tasks The event handler dispatch tasks to execute
     */
    private boolean useTimeout(final HandlerTask task)
    {
        // we only check the classname if a timeout is configured
        if ( m_timeout > 0)
        {
            final Matcher[] ignoreTimeoutMatcher = m_ignoreTimeoutMatcher;
            if ( ignoreTimeoutMatcher != null )
            {
                final String className = task.getHandlerClassName();
                for(int i=0;i<ignoreTimeoutMatcher.length;i++)
                {
                    if ( ignoreTimeoutMatcher[i] != null)
                    {
                        if ( ignoreTimeoutMatcher[i].match(className) )
                        {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * This blocks an unrelated thread used to send a synchronous event until the
     * event is send (or a timeout occurs).
     *
     * @param tasks The event handler dispatch tasks to execute
     *
     * @see org.apache.felix.eventadmin.impl.tasks.DeliverTask#execute(List)
     */
    public void execute(final List tasks)
    {
        final Thread sleepingThread = Thread.currentThread();
        final SyncThread syncThread = sleepingThread instanceof SyncThread ? (SyncThread)sleepingThread : null;

        final Iterator i = tasks.iterator();
        while ( i.hasNext() )
        {
            final HandlerTask task = (HandlerTask)i.next();

            if ( !useTimeout(task) )
            {
                // no timeout, we can directly execute
                task.execute();
            }
            else if ( syncThread != null )
            {
                // if this is a cascaded event, we directly use this thread
                // otherwise we could end up in a starvation
                final long startTime = System.currentTimeMillis();
                task.execute();
                if ( System.currentTimeMillis() - startTime > m_timeout )
                {
                    task.blackListHandler();
                }
            }
            else
            {
                final Rendezvous startBarrier = new Rendezvous();
                final Rendezvous timerBarrier = new Rendezvous();
                m_pool.executeTask(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            // notify the outer thread to start the timer
                            startBarrier.waitForRendezvous();
                            // execute the task
                            task.execute();
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
                    timerBarrier.waitAttemptForRendezvous(m_timeout);
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
