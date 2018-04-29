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

import org.apache.felix.scr.impl.logger.ScrLogger;
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
<<<<<<< HEAD
=======
        @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public void run()
        {
        }


<<<<<<< HEAD
=======
        @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public String toString()
        {
            return "Component Actor Terminator";
        }
    };

    // the queue of Runnable instances  to be run
    private final LinkedList<Runnable> tasks = new LinkedList<>();

    private final ScrLogger logger;


    ComponentActorThread( final ScrLogger log )
    {
<<<<<<< HEAD
        tasks = new LinkedList();
=======
        logger = log;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    // waits on Runnable instances coming into the queue. As instances come
    // in, this method calls the Runnable.run method, logs any exception
    // happening and keeps on waiting for the next Runnable. If the Runnable
    // taken from the queue is this thread instance itself, the thread
    // terminates.
    @Override
    public void run()
    {
<<<<<<< HEAD
        Activator.log( LogService.LOG_DEBUG, null, "Starting ComponentActorThread", null );
=======
        logger.log( LogService.LOG_DEBUG, "Starting ComponentActorThread", null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        for ( ;; )
        {
            final Runnable task;
            synchronized ( tasks )
            {
                while ( tasks.isEmpty() )
                {
                    boolean interrupted = Thread.interrupted();
                    try
                    {
                        tasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
<<<<<<< HEAD
                        Thread.currentThread().interrupt();
=======
                        interrupted = true;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        // don't care
                    }
                    finally
                    {
                        if (interrupted)
                        { // restore interrupt status
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                task = tasks.removeFirst();
            }

            try
            {
                // return if the task is this thread itself
                if ( task == TERMINATION_TASK )
                {
<<<<<<< HEAD
                    Activator.log( LogService.LOG_DEBUG, null, "Shutting down ComponentActorThread", null );
=======
                    logger.log( LogService.LOG_DEBUG, "Shutting down ComponentActorThread", null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    return;
                }

                // otherwise execute the task, log any issues
<<<<<<< HEAD
                Activator.log( LogService.LOG_DEBUG, null, "Running task: " + task, null );
=======
                logger.log( LogService.LOG_DEBUG, "Running task: " + task, null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                task.run();
            }
            catch ( Throwable t )
            {
<<<<<<< HEAD
                Activator.log( LogService.LOG_ERROR, null, "Unexpected problem executing task " + task, t );
=======
                logger.log( LogService.LOG_ERROR, "Unexpected problem executing task " + task, t );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
=======
                boolean interrupted = Thread.interrupted();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                try
                {
                    tasks.wait();
                }
                catch ( InterruptedException e )
                {
<<<<<<< HEAD
                    Thread.currentThread().interrupt();
                    Activator.log( LogService.LOG_ERROR, null, "Interrupted exception waiting for queue to empty", e );
=======
                    interrupted = true;
                    logger.log(LogService.LOG_ERROR,
                        "Interrupted exception waiting for queue to empty", e);
                }
                finally
                {
                    if (interrupted)
                    { // restore interrupt status
                        Thread.currentThread().interrupt();
                    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
            Activator.log( LogService.LOG_DEBUG, null, "Adding task [{0}] as #{1} in the queue" 
                    , new Object[] {task, tasks.size()}, null );
=======
            logger.log( LogService.LOG_DEBUG, "Adding task [{0}] as #{1} in the queue", null,
                    task, tasks.size(), null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

            // notify the waiting thread
            tasks.notifyAll();
        }
    }
}
