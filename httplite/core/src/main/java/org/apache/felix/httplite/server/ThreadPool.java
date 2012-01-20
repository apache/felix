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
package org.apache.felix.httplite.server;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.httplite.osgi.Logger;

/**
 * This class implements a simple thread pool for servicing HTTP connections.
 * The thread pool does not create any threads initially, but waits for
 * connections to be added to create threads. As connections are added, threads
 * are only created if they are needed up until the thread limit. If threads
 * are inactive for a period of time, then the threads terminate; the default
 * is 60000 milliseconds.
**/
public class ThreadPool
{
    /**
     * Default thread timeout
     */
    public static final int DEFAULT_THREAD_TIMEOUT = 60000;
    private final int m_threadTimeout;

    private final ThreadGroup m_group = new ThreadGroup("ThreadPoolGroup");
    private int m_state;
    private ThreadGate m_shutdownGate;
    private int m_threadName = 0;
    private int m_threadLimit = 0;
    private int m_threadCount = 0;
    private int m_threadAvailable = 0;
    private final List m_connectionList = new ArrayList();
    private final Logger m_logger;

    /**
     * Constructs a thread pool with the specified thread limit and with
     * the default inactivity timeout.
     * @param threadLimit The maximum number of threads in the pool.
     * @param logger Logger instance.
    **/
    public ThreadPool(final int threadLimit, final Logger logger)
    {
        this(threadLimit, DEFAULT_THREAD_TIMEOUT, logger);
    }

    /**
     * Constructs a thread pool with the specified thread limit and inactivity
     * timeout.
     * @param threadLimit The maximum number of threads in the pool.
     * @param threadTimeout The inactivity timeout for threads in milliseconds.
     * @param logger Logger instance.
    **/
    public ThreadPool(int threadLimit, int threadTimeout, Logger logger)
    {
        m_threadLimit = threadLimit;
        m_threadTimeout = threadTimeout;
        m_logger = logger;
        m_state = Server.INACTIVE_STATE;
    }

    /**
     * This method returns the current state of the thread pool, which is one
     * of the following values:
     * <ul>
     *   <li><tt>ThreadPool.INACTIVE_STATE</tt> - the thread pool is currently
     *       not active.
     *   </li>
     *   <li><tt>ThreadPool.ACTIVE_STATE</tt> - the thread pool is active and
     *       servicing connections.
     *   </li>
     *   <li><tt>ThreadPool.STOPPING_STATE</tt> - the thread pool is in the
     *       process of shutting down.
     *   </li>
     * </li>
     * @return The current state of the thread pool.
    **/
    public synchronized int getState()
    {
        return m_state;
    }

    /**
     * Starts the thread pool if it is not already active, allowing it to
     * service connections.
     * @throws java.lang.IllegalStateException If the thread pool is in the
     *         <tt>ThreadPool.STOPPING_STATE</tt> state.
    **/
    public synchronized void start()
    {
        if (m_state != Server.STOPPING_STATE)
        {
            m_state = Server.ACTIVE_STATE;
        }
        else
        {
            throw new IllegalStateException("Thread pool is in process of stopping.");
        }
    }

    /**
     * This method stops the thread pool if it is currently active. This method
     * will block the calling thread until the thread pool is completely stopped.
     * This can potentially take a long time, since it allows all existing
     * connections to be processed before shutting down. Subsequent calls to
     * this method will also block the caller. If a blocked thread is interrupted,
     * the method will release the blocked thread by throwing an interrupted
     * exception. In such a case, the thread pool will still continue its
     * shutdown process.
     * @throws java.lang.InterruptedException If the calling thread is interrupted.
    **/
    public void stop() throws InterruptedException
    {
        ThreadGate gate = null;

        synchronized (this)
        {
            if (m_state != Server.INACTIVE_STATE)
            {
                // If there is no shutdown gate, create one and save its
                // reference both in the field and locally. All threads
                // that call stop() while the server is stopping will wait
                // on this gate.
                if ((m_shutdownGate == null) && (m_threadCount > 0))
                {
                    m_shutdownGate = new ThreadGate();
                }
                gate = m_shutdownGate;
                m_state = Server.STOPPING_STATE;
                // Interrupt all threads that have been created by the
                // thread pool.
                m_group.interrupt();
            }
        }

        // Wait on gate for thread pool shutdown to complete.
        if (gate != null)
        {
            gate.await();
        }
    }

    /**
     * This method adds an HTTP connection to the thread pool for servicing.
     * @param connection
     * @throws java.lang.IllegalStateException If the thread pool is not in the
     *         <tt>ThreadPool.ACTIVE_STATE</tt> state.
    **/
    public synchronized void addConnection(final Connection connection)
    {
        if (m_state == Server.ACTIVE_STATE)
        {
            // Add the new connection to the connection list.
            m_connectionList.add(connection);
            notify();

            // If there are not enough available threads to handle all outstanding
            // connections and we still haven't reached our thread limit, then
            // add another thread.
            if ((m_threadAvailable < m_connectionList.size())
                && (m_threadCount < m_threadLimit))
            {
                // Increase our thread count, but not number of available threads,
                // since the new thread will be used to service the new connection
                // and thus is not available.
                m_threadCount++;
                // Use simple integer for thread name for logging purposes.
                if (m_threadName == Integer.MAX_VALUE)
                {
                    m_threadName = 1;
                }
                else
                {
                    m_threadName++;
                }
                // Create and start thread into our thread group.
                new Thread(m_group, new Runnable()
                {
                    public void run()
                    {
                        processConnections();
                    }
                }, Integer.toString(m_threadName)).start();
                m_logger.log(Logger.LOG_DEBUG, "Created new thread for pool; count = "
                    + m_threadCount + ", max = " + m_threadLimit + ".");
            }
        }
        else
        {
            throw new IllegalStateException("The thread pool is not active.");
        }
    }

    /**
     * This method is the main loop for all threads servicing connections.
    **/
    private void processConnections()
    {
        Connection connection;
        while (true)
        {
            synchronized (this)
            {
                // Any new threads entering this region are now available to
                // process a connection, so increment the available count.
                m_threadAvailable++;

                try
                {
                    // Keep track of when we start to wait so that we
                    // know if our timeout expires.
                    long start = System.currentTimeMillis();
                    long current = start;
                    // Wait until there is a connection to service or until
                    // the timeout expires; if the timeout is zero, then there
                    // is no timeout.
                    while (m_state == Server.ACTIVE_STATE
                        && (m_connectionList.size() == 0)
                        && ((m_threadTimeout == 0) || ((current - start) < m_threadTimeout)))
                    {
                        // Try to wait for another connection, but our timeout
                        // expires then commit suicide.
                        wait(m_threadTimeout - (current - start));
                        current = System.currentTimeMillis();
                    }
                }
                catch (InterruptedException ex)
                {
                    // This generally happens when we are shutting down.
                    Thread.currentThread().interrupt();
                }

                // Set connection to null if we are going to commit suicide;
                // otherwise get the first available connection for servicing.
                if (m_connectionList.size() == 0)
                {
                    connection = null;
                }
                else
                {
                    connection = (Connection) m_connectionList.remove(0);
                }

                // Decrement number of available threads, since we will either
                // start to service a connection at this point or we will commit
                // suicide.
                m_threadAvailable--;

                // If we do not have a connection, then we are committing
                // suicide due to inactivity or because we were interrupted
                // and are stopping the thread pool.
                if (connection == null)
                {
                    // One less thread in use.
                    m_threadCount--;
                    if (Thread.interrupted())
                    {
                        m_logger.log(Logger.LOG_DEBUG,
                            "Pool thread dying due to interrupt.");
                    }
                    else
                    {
                        m_logger.log(Logger.LOG_DEBUG,
                            "Pool thread dying due to inactivity.");
                    }
                    // If we are stopping and the last thread is dead, then
                    // open the shutdown gate to release all threads waiting
                    // for us to stop.
                    if ((m_state == Server.STOPPING_STATE) && (m_threadCount == 0))
                    {
                        m_shutdownGate.open();
                        m_shutdownGate = null;
                        m_state = Server.INACTIVE_STATE;
                        m_logger.log(Logger.LOG_DEBUG,
                            "Server shutdown complete.");
                    }
                    // Return to kill the thread by exiting our run method.
                    return;
                }
            }

            // Otherwise, we have a connection so process it.
            // Note, we might have outstanding connections to
            // process even if we are stopping, so we cleaning
            // service those remaining connections before stopping.
            try
            {
                connection.process();
                m_logger.log(Logger.LOG_DEBUG, "Connection closed normally.");
            }
            catch (SocketTimeoutException ex)
            {
                m_logger.log(Logger.LOG_INFO, "Connection closed due to inactivity.");
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Connection close due to unknown reason.",
                    ex);
            }
        }
    }
}