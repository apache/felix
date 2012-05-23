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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.eventadmin.impl.handler.EventHandlerProxy;
import org.osgi.service.event.Event;

/**
 * This class does the actual work of the asynchronous event dispatch.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AsyncDeliverTasks
{
    /** The thread pool to use to spin-off new threads. */
    private final DefaultThreadPool m_pool;

    /** The deliver task for actually delivering the events. This
     * is the sync deliver tasks as this has all the code for timeout
     * handling etc.
     */
    private final SyncDeliverTasks m_deliver_task;

    /** A map of running threads currently delivering async events. */
    private final Map m_running_threads = new HashMap();

    /**
     * The constructor of the class that will use the asynchronous.
     *
     * @param pool The thread pool used to spin-off new asynchronous event
     *      dispatching threads in case of timeout or that the asynchronous event
     *      dispatching thread is used to send a synchronous event
     * @param deliverTask The deliver tasks for dispatching the event.
     */
    public AsyncDeliverTasks(final DefaultThreadPool pool, final SyncDeliverTasks deliverTask)
    {
        m_pool = pool;
        m_deliver_task = deliverTask;
    }

    /**
     * This does not block an unrelated thread used to send a synchronous event.
     *
     * @param tasks The event handler dispatch tasks to execute
     *
     */
    public void execute(final Collection tasks, final Event event)
    {
        final Iterator i = tasks.iterator();
        boolean hasOrdered = false;
        while ( i.hasNext() )
        {
            final EventHandlerProxy task = (EventHandlerProxy)i.next();
            if ( !task.isAsyncOrderedDelivery() )
            {
                // do somethimg
            }
            else
            {
                hasOrdered = true;
            }

        }
        if ( hasOrdered )
        {
            final Thread currentThread = Thread.currentThread();
            TaskExecuter executer = null;
            synchronized (m_running_threads )
            {
                final TaskExecuter runningExecutor = (TaskExecuter)m_running_threads.get(currentThread);
                if ( runningExecutor != null )
                {
                    runningExecutor.add(tasks, event);
                }
                else
                {
                    executer = new TaskExecuter( tasks, event, currentThread );
                    m_running_threads.put(currentThread, executer);
                }
            }
            if ( executer != null )
            {
                m_pool.executeTask(executer);
            }
        }
    }

    private final class TaskExecuter implements Runnable
    {
        private final List m_tasks = new LinkedList();

        private final Object m_key;

        public TaskExecuter(final Collection tasks, final Event event, final Object key)
        {
            m_key = key;
            m_tasks.add(new Object[] {tasks, event});
        }

        public void run()
        {
            boolean running;
            do
            {
                Object[] tasks = null;
                synchronized ( m_tasks )
                {
                    tasks = (Object[]) m_tasks.remove(0);
                }
                m_deliver_task.execute((Collection)tasks[0], (Event)tasks[1], true);
                synchronized ( m_running_threads )
                {
                    running = m_tasks.size() > 0;
                    if ( !running )
                    {
                        m_running_threads.remove(m_key);
                    }
                }
            } while ( running );
        }

        public void add(final Collection tasks, final Event event)
        {
            synchronized ( m_tasks )
            {
                m_tasks.add(new Object[] {tasks, event});
            }
        }
    }
}
