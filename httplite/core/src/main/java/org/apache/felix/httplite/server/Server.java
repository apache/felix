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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.httplite.osgi.Logger;
import org.apache.felix.httplite.osgi.ServiceRegistrationResolver;

/**
 * This class implements a simple multi-threaded web server. It
 * only supports GET/HEAD requests. The web server has various configurable
 * properties that can be passed into the constructor; see the constructor
 * for more information about configuration properties.
**/
public class Server
{
    /**
     * Connection timeout property
     */
    public static final String CONFIG_PROPERTY_CONNECTION_TIMEOUT_PROP = "org.apache.felix.http.connection.timeout";
    /**
     * Connection request limit property
     */
    public static final String CONFIG_PROPERTY_CONNECTION_REQUESTLIMIT_PROP = "org.apache.felix.http.connection.requestlimit";
    /**
     * Thread pool timeout property
     */
    public static final String CONFIG_PROPERTY_THREADPOOL_TIMEOUT_PROP = "org.apache.felix.http.threadpool.timeout";
    /**
     * Thread pool limit property
     */
    public static final String CONFIG_PROPERTY_THREADPOOL_LIMIT_PROP = "org.apache.felix.http.threadpool.limit";
    /**
     * Flag to enable the user of HTTPS. The default is false.
     */
    public static final String CONFIG_PROPERTY_HTTPS_ENABLE = "org.apache.felix.https.enable";
    /**
     * Flag to enable the use of HTTP. The default is true.
     */
    public static final String CONFIG_PROPERTY_HTTP_ENABLE = "org.apache.felix.http.enable"; 
    /**
     * The port used for servlets and resources available via HTTP. The default is 8080. A negative port number has the same effect as setting org.apache.felix.http.enable to false.
     */
    public static final String CONFIG_PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    /**
     * The address of the host interface to bind http to. The default is to bind to all interfaces.
     */
    public static final String CONFIG_PROPERTY_HTTP_HOST = "org.apache.felix.http.host"; 

    /**
     * Default HTTP port to listen on.
     */
    private static final int DEFAULT_PORT = 8080;
    /**
     * Default number of concurrent requests.
     */
    private static final int DEFAULT_THREADPOOL_LIMIT = 10;

    /**
     * Server is inactive (off).
     */
    public static final int INACTIVE_STATE = 0;
    /**
     * Server is active (running)
     */
    public static final int ACTIVE_STATE = 1;
    /**
     * Server is shutting down.
     */
    public static final int STOPPING_STATE = 2;

    private String m_hostname;
    private final int m_port;
    private InetAddress m_bindAddr;

    private int m_state;
    private ThreadGate m_shutdownGate;

    private Thread m_serverThread;
    private ServerSocket m_serverSocket;
    private final ThreadPool m_threadPool;

    private final int m_connectionTimeout;
    private final int m_connectionRequestLimit;
    private ServiceRegistrationResolver m_resolver;
    private final Logger m_logger;
    
	/**
	 * Set to true when bundle is in a stopping state.  Socket errors will be ignored.
	 */
	private boolean m_stopping = false;

    /**
     * Construct a web server with the specified configuration. The configuration
     * map may contain the following properties:
     * <ul>
     *   <li><tt>org.osgi.service.http.port</tt> - the port on which it listens for connections;
     *       the default is 8080.
     *   </li>   
     *   <li><tt>org.apache.felix.http.host</tt> - the address of the host interface which is bound;
     *       the default is all addresses.
     *   </li>
     *   <li><tt>org.apache.felix.http.threadpool.limit</tt> - the maximum number of threads in the
     *       thread pool; the default value is 10.
     *   </li>
     *   <li><tt>org.apache.felix.http.threadpool.timeout</tt> - the inactivity timeout for threads in
     *       the thread pool after which time the threads will terminate; the
     *       default value is 60000 milliseconds.
     *   </li>
     *   <li><tt>.org.apache.felix.http.connection.requestlimit</tt> - the maximum number of requests that
     *       will be accepted over a persistent connection before closing the
     *       connection; the default value is 50.
     *   </li>
     *   <li><tt>org.apache.felix.http.connection.timeout</tt> - the inactivity timeout for persistent
     *       connections after which the connection is closed; the default value
     *       is 10000 milliseconds.
     *   </li>
     * </ul>
     * The configuration properties cannot be changed after construction. The
     * web server is not active until it is started.
     * @param configMap The map of configuration properties; can be <tt>null</tt>.
     * @param logger 
    **/
    public Server(Map configMap, final Logger logger)
    {
        this.m_logger = logger;
        m_state = INACTIVE_STATE;

        configMap = (configMap == null) ? new HashMap() : configMap;

        // Read in the configured properties or their default values.
        m_port = getConfiguredPort(configMap);
        m_bindAddr = getConfiguredBindAddr(configMap);
        int threadLimit = (configMap.get(Server.CONFIG_PROPERTY_THREADPOOL_LIMIT_PROP) == null) ? DEFAULT_THREADPOOL_LIMIT
            : Integer.parseInt((String) configMap.get(Server.CONFIG_PROPERTY_THREADPOOL_LIMIT_PROP));
        int threadTimeout = (configMap.get(Server.CONFIG_PROPERTY_THREADPOOL_TIMEOUT_PROP) == null) ? ThreadPool.DEFAULT_THREAD_TIMEOUT
            : Integer.parseInt((String) configMap.get(Server.CONFIG_PROPERTY_THREADPOOL_TIMEOUT_PROP));
        m_threadPool = new ThreadPool(threadLimit, threadTimeout, m_logger);
        m_connectionTimeout = (configMap.get(Server.CONFIG_PROPERTY_CONNECTION_TIMEOUT_PROP) == null) ? Connection.DEFAULT_CONNECTION_TIMEOUT
            : Integer.parseInt((String) configMap.get(Server.CONFIG_PROPERTY_CONNECTION_TIMEOUT_PROP));
        m_connectionRequestLimit = (configMap.get(Server.CONFIG_PROPERTY_CONNECTION_REQUESTLIMIT_PROP) == null) ? Connection.DEFAULT_CONNECTION_REQUESTLIMIT
            : Integer.parseInt((String) configMap.get(Server.CONFIG_PROPERTY_CONNECTION_REQUESTLIMIT_PROP));
    }

    /**
     * Get the port the HTTP server listens on based on configuration map or default value.
     * 
     * @param configMap
     * @return port number that server listens on.
     */
    public static int getConfiguredPort(Map configMap)
    {
        return (configMap.get(Server.CONFIG_PROPERTY_HTTP_PORT) == null) ? DEFAULT_PORT
            : Integer.parseInt((String) configMap.get(Server.CONFIG_PROPERTY_HTTP_PORT));
    }

    /**
     * Get the address of the interface the HTTP server listens on based on configuration map or default value.
     * 
     * @param configMap
     * @return Address of the interface to bind to or null if no interface is specified.
     */
    public InetAddress getConfiguredBindAddr(Map configMap)
    {
        try
        {
            return (configMap.get(Server.CONFIG_PROPERTY_HTTP_HOST) == null) ? null
                : InetAddress.getByName((String) configMap.get(Server.CONFIG_PROPERTY_HTTP_HOST));
        }
        catch(UnknownHostException ex)
        {
            m_logger.log(Logger.LOG_ERROR,
                "Unable to resolve " + configMap.get(Server.CONFIG_PROPERTY_HTTP_HOST) + " to address of interface to bind to. Binding to all interfaces.", ex);
	    return null;
        }
    }

    /**
     * This method returns the current state of the web server, which is one
     * of the following values:
     * <ul>
     *   <li><tt>HttpServer.INACTIVE_STATE</tt> - the web server is currently
     *       not active.
     *   </li>
     *   <li><tt>HttpServer.ACTIVE_STATE</tt> - the web server is active and
     *       serving files.
     *   </li>
     *   <li><tt>HttpServer.STOPPING_STATE</tt> - the web server is in the
     *       process of shutting down.
     *   </li>
     * </li>
     * @return The current state of the web server.
    **/
    public synchronized int getState()
    {
        return m_state;
    }

    /**
     * Returns the hostname associated with the web server.
     * @return The hostname associated with the web server.
    **/
    public synchronized String getHostname()
    {
        if (m_hostname == null)
        {
            try
            {
                m_hostname = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException ex)
            {
                m_logger.log(Logger.LOG_ERROR,
                    "Unable to get hostname, setting to localhost.", ex);
                m_hostname = "localhost";
            }
        }
        return m_hostname;
    }

    /**
     * Returns the port associated with the web server.
     * @return The port associated with the web server.
    **/
    public synchronized int getPort()
    {
        return m_port;
    }

    /**
     * This method starts the web server if it is not already active.
     * @param resolver Resolver is able to get Servlet or Resource based on request URI.
     * 
     * @throws java.io.IOException If there are any networking issues.
     * @throws java.lang.IllegalStateException If the server is in the
     *         <tt>HttpServer.STOPPING_STATE</tt> state.
    **/
    public synchronized void start(final ServiceRegistrationResolver resolver)
        throws IOException
    {
        m_resolver = resolver;
        if (m_state == INACTIVE_STATE)
        {
            // If inactive, then create server socket, server thread, and
            // set state to active.
            if (m_bindAddr == null)
            {
                m_serverSocket = new ServerSocket(m_port);
            }
            else
            {
                m_serverSocket = new ServerSocket(m_port, 0, m_bindAddr);
            }

            m_serverThread = new Thread(new Runnable()
            {
				public void run()
                {
                    acceptConnections();
                }
            }, "HttpServer");
            m_state = ACTIVE_STATE;
            m_serverThread.start();
        }
        else if (m_state == STOPPING_STATE)
        {
            throw new IllegalStateException("Server is in process of stopping.");
        }
    }

    /**
     * This method stops the web server if it is currently active. This method
     * will block the calling thread until the web server is completely stopped.
     * This can potentially take a long time, since it allows all existing
     * connections to be processed before shutting down. Subsequent calls to
     * this method will also block the caller. If a blocked thread is interrupted,
     * the method will release the blocked thread by throwing an interrupted
     * exception. In such a case, the web server will still continue its
     * shutdown process.
     * @throws java.lang.InterruptedException If the calling thread is interrupted.
    **/
    public void stop() throws InterruptedException
    {
        ThreadGate gate = null;

        synchronized (this)
        {
            // If we are active or stopping, allow the caller to shutdown the
            // server socket and grab a local copy of the shutdown gate to
            // wait for the server to stop.
            if (m_state != INACTIVE_STATE)
            {
                m_logger.log(Logger.LOG_INFO,
                    "Shutting down, be patient...waiting for all threads to finish.");

                // If there is no shutdown gate, create one and save its
                // reference both in the field and locally. All threads
                // that call stop() while the server is stopping will wait
                // on this gate.
                if (m_shutdownGate == null)
                {
                    m_shutdownGate = new ThreadGate();
                }
                gate = m_shutdownGate;

                // Close the server socket, which will cause the server thread
                // to exit its accept() loop.
                try
                {
                    m_serverSocket.close();
                }
                catch (IOException ex)
                {
                }
            }
        }

        // Wait on gate for server thread to shutdown.
        if (gate != null)
        {
            gate.await();
        }
    }

    /**
     * This method is the main server loop for accepting connection. This is
     * only ever called by the server thread.
    **/
    private void acceptConnections()
    {
        // Start the thread pool.
        m_threadPool.start();

        Socket socket;

        m_logger.log(Logger.LOG_DEBUG, "Waiting for connections.");
        boolean socketConnected = true;

        // Now listen for connections until interrupted.
        while (socketConnected)
        {
            try
            {
                socket = m_serverSocket.accept();
                try
                {
                    // Create connection object and add it to the thread pool
                    // to be serviced.
                    Connection connection = new Connection(socket, m_connectionTimeout,
                        m_connectionRequestLimit, m_resolver, m_logger);
                    m_logger.log(Logger.LOG_DEBUG, "Accepted a new connection.");
                    m_threadPool.addConnection(connection);
                }
                catch (IOException ex)
                {
                    // If we have any difficulty creating the connection
                    // then just ignore it, because the socket will be
                    // closed in the connection constructor.
                    m_logger.log(Logger.LOG_ERROR, "Error creating connection.", ex);
                }
            }
            catch (IOException ex)
            {
                socketConnected = false;
            	if (!m_stopping)
            	{
	                m_logger.log(Logger.LOG_ERROR,
	                    "The call to accept() terminated with an exception.", ex);
            	}
            }
        }

        // Shutdown the server.
        shutdown();
    }

    /**
     * This method shuts down the server; it is only ever called by the
     * server thread.
    **/
    private void shutdown()
    {
        m_logger.log(Logger.LOG_DEBUG, "Waiting for thread pool threads to stop.");

        while (true)
        {
            try
            {
                // Wait for thread pool to stop servicing connections.
                m_threadPool.stop();
                break;
            }
            catch (InterruptedException ex)
            {
                // Only the server thread will call this and we don't interrupt
                // it, so this should never happen, but just in case we will loop
                // until the thread pool is actually stopped.
            }
        }

        synchronized (this)
        {
            // Now that the thread pool is stopped, open the shutdown
            // gate and set the state to inactive.
            m_shutdownGate.open();
            m_shutdownGate = null;
            m_state = INACTIVE_STATE;
        }
        m_logger.log(Logger.LOG_DEBUG, "Shutdown complete.");
    }

	/**
	 * Sets the stopping flag to true, meaning that further socket exceptions will not be logged as errors.
	 */
	public void setStopping() {		
		m_stopping  = true;
	}
}
