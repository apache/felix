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
package org.apache.felix.scr.impl;


import java.util.LinkedList;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.service.log.LogService;


/**
 * The <code>ComponentActorThread</code> is the thread used to act upon registered
 * components of the service component runtime.
 */
class ComponentActorThread implements Runnable
{

    // sentinel task to terminate this thread
    private static final Runnable TERMINATION_TASK = new Runnable()
    {
        public void run()
        {
        }


        public String toString()
        {
            return "Component Actor Terminator";
        }
    };

    // the queue of Runnable instances  to be run
    private LinkedList<Runnable> tasks;

    private SimpleLogger logger;


    ComponentActorThread( SimpleLogger log )
    {
        tasks = new LinkedList<Runnable>();
        logger = log;
    }


    // waits on Runnable instances coming into the queue. As instances come
    // in, this method calls the Runnable.run method, logs any exception
    // happening and keeps on waiting for the next Runnable. If the Runnable
    // taken from the queue is this thread instance itself, the thread
    // terminates.
    public void run()
    {
        logger.log( LogService.LOG_DEBUG, "Starting ComponentActorThread", null );

        for ( ;; )
        {
            final Runnable task;
            synchronized ( tasks )
            {
                while ( tasks.isEmpty() )
                {
                    try
                    {
                        tasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
                        Thread.currentThread().interrupt();
                        // don't care
                    }
                }

                task = tasks.removeFirst();
            }

            try
            {
                // return if the task is this thread itself
                if ( task == TERMINATION_TASK )
                {
                    logger.log( LogService.LOG_DEBUG, "Shutting down ComponentActorThread", null );
                    return;
                }

                // otherwise execute the task, log any issues
                logger.log( LogService.LOG_DEBUG, "Running task: " + task, null );
                task.run();
            }
            catch ( Throwable t )
            {
                logger.log( LogService.LOG_ERROR, "Unexpected problem executing task " + task, t );
            }
            finally
            {
                synchronized ( tasks )
                {
                    tasks.notifyAll();
                }
            }
        }
    }


    // cause this thread to terminate by adding this thread to the end
    // of the queue
    void terminate()
    {
        schedule( TERMINATION_TASK );
        synchronized ( tasks )
        {
            while ( !tasks.isEmpty() )
            {
                try
                {
                    tasks.wait();
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    logger.log( LogService.LOG_ERROR, "Interrupted exception waiting for queue to empty", e );
                }
            }
        }
    }


    // queue the given runnable to be run as soon as possible
    void schedule( Runnable task )
    {
        synchronized ( tasks )
        {
            // append to the task queue
            tasks.add( task );

            logger.log( LogService.LOG_DEBUG, "Adding task [{0}] as #{1} in the queue"
                    , new Object[] {task, tasks.size()}, null );

            // notify the waiting thread
            tasks.notifyAll();
        }
    }
}
