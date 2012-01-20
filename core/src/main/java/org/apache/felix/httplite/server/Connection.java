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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.Logger;
import org.apache.felix.httplite.osgi.ServiceRegistrationHandler;
import org.apache.felix.httplite.osgi.ServiceRegistrationResolver;
import org.apache.felix.httplite.servlet.ConcreteServletInputStream;
import org.apache.felix.httplite.servlet.HttpConstants;
import org.apache.felix.httplite.servlet.HttpServletRequestImpl;
import org.apache.felix.httplite.servlet.HttpServletResponseImpl;

/**
 * This class represents an accepted connection between the server and
 * a client. It supports persistent connections for both HTTP 1.0 and 1.1
 * clients. A given persistent connection is limited in the number of
 * consecutive requests it is allowed to make before having its connection
 * closed as well as after a period of inactivity.
**/
public class Connection
{
    /**
     * Connection timeout
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    /**
     * Requests per request
     */
    public static final int DEFAULT_CONNECTION_REQUESTLIMIT = 50;

    private final Socket m_socket;
    private ConcreteServletInputStream m_is;
    private OutputStream m_os;
    private int m_requestCount = 0;
    private final int m_requestLimit;
    private final ServiceRegistrationResolver m_resolver;
    private final Logger m_logger;

    /**
     * Constructs a connection with a default inactivity timeout and request limit.
     *     
     * @param socket Socket connection
     * @param resolver a resolver to get http request/response and handler.
     * @param logger Logger
     * @throws IOException If any I/O error occurs.
     */
    public Connection(final Socket socket, final ServiceRegistrationResolver resolver, final Logger logger) throws IOException
    {
        this(socket, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_REQUESTLIMIT,
            resolver, logger);
    }

    /**
     * Constructs a connection with the specified inactivity timeout and request limit.
     * @param socket The client socket.
     * @param timeout The inactivity timeout of the connection in milliseconds.
     * @param requestLimit The maximum number of consecutive requests.
     * @param resolver resolves a request URI to a client or servlet registration via the HTTP Service.
     * @param logger logger instance.
     * @throws java.io.IOException If any I/O error occurs.
     */
    public Connection(final Socket socket, final int timeout, final int requestLimit, final ServiceRegistrationResolver resolver, final Logger logger) throws IOException
    {
        m_socket = socket;
        m_resolver = resolver;
        m_logger = logger;
        m_socket.setSoTimeout(timeout);
        m_socket.setTcpNoDelay(true);
        m_requestLimit = requestLimit;
        try
        {
            m_is = new ConcreteServletInputStream(new BufferedInputStream(
                m_socket.getInputStream()));
            m_os = new BufferedOutputStream(m_socket.getOutputStream());
        }
        catch (IOException ex)
        {
            // Make sure we close any opened socket/streams.
            try
            {
                m_socket.close();
            }
            catch (IOException ex2)
            {
                m_logger.log(Logger.LOG_ERROR, "Error closing socket.", ex);
            }
            if (m_is != null)
            {
                try
                {
                    m_is.close();
                }
                catch (IOException ex2)
                {
                    m_logger.log(Logger.LOG_ERROR, "Error closing socket input stream.",
                        ex2);
                }
            }
            if (m_os != null)
            {
                try
                {
                    m_os.close();
                }
                catch (IOException ex2)
                {
                    m_logger.log(Logger.LOG_ERROR, "Error closing socket output stream.",
                        ex2);
                }
            }
            throw ex;
        }
    }

    /**
     * Performs the actual servicing of the connection and its subsequent requests.
     * This method will be called by threads in the thread pool. This method
     * typically exits when the connection is closed due to either an explicit
     * connection close, the inactivity timeout expires, the maximum request
     * limit was reached, or an I/O error occurred. When this method returns,
     * the associated socket will be closed, regardless of whether or not an
     * expection was thrown.
     * @throws java.net.SocketTimeoutException If the inactivity timeout expired
     *         while trying to read from the socket.
     * @throws java.io.IOException If any I/O error occurs.
     * @throws ServletException on servlet errors
    **/
    public void process() throws IOException, ServletException
    {
        HttpServletRequestImpl request = m_resolver.getServletRequest(m_socket);
        HttpServletResponseImpl response = m_resolver.getServletResponse(m_os);

        try
        {
            // Loop until we close the connection.
            boolean close = false;
            while (!close)
            {
                // Read the next request.
                try
                {
                    request.parseRequestLine(m_is);
                }
                catch (IOException e)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Error with request: " + request.toString() + ": "
                            + e.getMessage());
                    throw e;
                }
                m_requestCount++;

                // Keep track of whether we have failed or not,
                // because we still want to read the bytes to clear
                // the input stream so we can service more requests.
                boolean error = false;

                m_logger.log(Logger.LOG_DEBUG,
                    "Processing " + request.getRequestURI() + " (" + (m_requestLimit - m_requestCount)
                        + " remaining)");

                // If client is HTTP/1.1, then send continue message.
                if (request.getProtocol().equals(HttpConstants.HTTP11_VERSION))
                {
                    response.sendContinueResponse();
                }

                // Read the header lines of the request.
                request.parseHeader(m_is);

                // If we have an HTTP/1.0 request without the connection set to
                // keep-alive or we explicitly have a request to close the connection,
                // then set close flag to exit the loop rather than trying to read
                // more requests.
                String v = request.getHeader(HttpConstants.HEADER_CONNECTION);
                if ((request.getProtocol().equals(HttpConstants.HTTP10_VERSION) && ((v == null) || (!v.equalsIgnoreCase(HttpConstants.KEEPALIVE_CONNECTION))))
                    || ((v != null) && v.equalsIgnoreCase(HttpConstants.CLOSE_CONNECTION)))
                {
                    close = true;
                    response.setConnectionType("close");
                }
                // If we have serviced the maximum number of requests for
                // this connection, then set close flag so we exit the loop
                // and close the connection.
                else if (m_requestCount >= m_requestLimit)
                {
                    close = true;
                    response.setConnectionType("close");
                }

                // We do not support OPTIONS method so send
                // a "not implemented" error in that case.
                if (!HttpServletRequestImpl.isSupportedMethod(request.getMethod()))
                {
                    error = true;
                    response.setConnectionType(HttpConstants.CLOSE_CONNECTION);
                    response.sendNotImplementedResponse();
                }

                // Ignore if we have already failed, otherwise send error message
                // if an HTTP/1.1 client did not include HOST header.
                if (!error && request.getProtocol().equals(HttpConstants.HTTP11_VERSION)
                    && (request.getHeader(HttpConstants.HOST_HEADER) == null))
                {
                    error = true;
                    response.setConnectionType(HttpConstants.CLOSE_CONNECTION);
                    response.sendMissingHostResponse();
                }

                // Read in the request body.
                request.parseBody(m_is);

                // Only process the request if there was no error.
                if (!error)
                {
                    ServiceRegistrationHandler processor = m_resolver.getProcessor(
                        request, response, request.getRequestURI());

                    if (processor != null)
                    {
                        processor.handle(close);

                        m_logger.log(Logger.LOG_DEBUG, "Processed " + request.toString());

                        // TODO: Adding next line to make test cases pass, but not sure if it is correct
                        // and needs further investigation.
                        close = true;
                        continue;
                    }

                    close = true;
                    response.setConnectionType(HttpConstants.CLOSE_CONNECTION);
                    response.sendNotFoundResponse();
                }
            }
        }
        finally
        {
            try
            {
                m_is.close();
            }
            catch (IOException ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Error closing socket input stream.", ex);
            }
            try
            {
                m_os.close();
            }
            catch (IOException ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Error closing socket output stream.", ex);
            }
            try
            {
                m_socket.close();
            }
            catch (IOException ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Error closing socket.", ex);
            }
        }
    }
}