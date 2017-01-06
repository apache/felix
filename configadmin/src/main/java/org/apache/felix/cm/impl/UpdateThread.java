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
package org.apache.felix.cm.impl;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;

import org.osgi.service.log.LogService;


/**
 * The <code>UpdateThread</code> is the thread used to update managed services
 * and managed service factories as well as to send configuration events.
 */
public class UpdateThread implements Runnable
{

    // the configuration manager on whose behalf this thread is started
    // (this is mainly used for logging)
    private final ConfigurationManager configurationManager;

    // the thread group into which the worker thread will be placed
    private final ThreadGroup workerThreadGroup;

    // the thread's base name
    private final String workerBaseName;

    // the queue of Runnable instances  to be run
    private final LinkedList updateTasks;

    // the actual thread
    private Thread worker;

    // the access control context
    private final AccessControlContext acc;

    public UpdateThread( final ConfigurationManager configurationManager, final ThreadGroup tg, final String name )
    {
        this.configurationManager = configurationManager;
        this.workerThreadGroup = tg;
        this.workerBaseName = name;
        this.acc = AccessController.getContext();

        this.updateTasks = new LinkedList();
    }


    // waits on Runnable instances coming into the queue. As instances come
    // in, this method calls the Runnable.run method, logs any exception
    // happening and keeps on waiting for the next Runnable. If the Runnable
    // taken from the queue is this thread instance itself, the thread
    // terminates.
    public void run()
    {
        for ( ;; )
        {
            Runnable task;
            synchronized ( updateTasks )
            {
                while ( updateTasks.isEmpty() )
                {
                    try
                    {
                        updateTasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
                        // don't care
                    }
                }

                task = ( Runnable ) updateTasks.removeFirst();
            }

            // return if the task is this thread itself
            if ( task == this )
            {
                return;
            }

            // otherwise execute the task, log any issues
            try
            {
                // set the thread name indicating the current task
                Thread.currentThread().setName( workerBaseName + " (" + task + ")" );

                configurationManager.log( LogService.LOG_DEBUG, "Running task {0}", new Object[]
                    { task } );

                run0(task);
            }
            catch ( Throwable t )
            {
                configurationManager.log( LogService.LOG_ERROR, "Unexpected problem executing task", t );
            }
            finally
            {
                // reset the thread name to "idle"
                Thread.currentThread().setName( workerBaseName );
            }
        }
    }

    void run0(final Runnable task) throws Throwable {
        if (System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        public Void run() throws Exception {
                            task.run();
                            return null;
                        }
                    },
                    acc
                );
            }
            catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        else {
            task.run();
        }
    }

    /**
     * Starts processing the queued tasks. This method does nothing if the
     * worker has already been started.
     */
    synchronized void start()
    {
        if ( this.worker == null )
        {
            Thread workerThread = new Thread( workerThreadGroup, this, workerBaseName );
            workerThread.setDaemon( true );
            workerThread.start();
            this.worker = workerThread;
        }
    }


    /**
     * Terminates the worker thread and waits for the thread to have processed
     * all outstanding events up to and including the termination job. All
     * jobs {@link #schedule(Runnable) scheduled} after termination has been
     * initiated will not be processed any more. This method does nothing if
     * the worker thread is not currently active.
     * <p>
     * If the worker thread does not terminate within 5 seconds it is killed
     * by calling the (deprecated) <code>Thread.stop()</code> method. It may
     * be that the worker thread may be blocked by a deadlock (it should not,
     * though). In this case hope is that <code>Thread.stop()</code> will be
     * able to released that deadlock at the expense of one or more tasks to
     * not be executed any longer.... In any case an ERROR message is logged
     * with the LogService in this situation.
     */
    synchronized void terminate()
    {
        if ( this.worker != null )
        {
            Thread workerThread = this.worker;
            this.worker = null;

            schedule( this );

            // wait for all updates to terminate (<= 10 seconds !)
            try
            {
                workerThread.join( 5000 );
            }
            catch ( InterruptedException ie )
            {
                // don't really care
            }

            if ( workerThread.isAlive() )
            {
                this.configurationManager.log( LogService.LOG_ERROR,
                    "Worker thread {0} did not terminate within 5 seconds; trying to kill", new Object[]
                        { workerBaseName } );
                workerThread.stop();
            }
        }
    }


    // queue the given runnable to be run as soon as possible
    void schedule( Runnable update )
    {
        synchronized ( updateTasks )
        {
            configurationManager.log( LogService.LOG_DEBUG, "Scheduling task {0}", new Object[]
                { update } );

            // append to the task queue
            updateTasks.add( update );

            // notify the waiting thread
            updateTasks.notifyAll();
        }
    }
}
