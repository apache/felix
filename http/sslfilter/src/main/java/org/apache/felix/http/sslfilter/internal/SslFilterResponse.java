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
package org.apache.felix.http.sslfilter.internal;

import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_LOCATION;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_PORT;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_PROTO;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTP;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS_PORT;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTP_PORT;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Provides a custom {@link HttpServletResponse} for use in SSL filter.
 */
class SslFilterResponse extends HttpServletResponseWrapper
{
    private final URL requestURL;
    private final String serverName;
    private final String serverProto;
    private final int serverPort;
    private final String clientProto;
    private final int clientPort;

    public SslFilterResponse(HttpServletResponse response, HttpServletRequest request) throws MalformedURLException
    {
        super(response);

        this.requestURL = new URL(request.getRequestURL().toString());

        // Only rewrite URLs for the host & port the request was sent to...
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();

        String proto = request.getHeader(HDR_X_FORWARDED_PROTO);
        if (HTTP.equalsIgnoreCase(proto))
        {
            // Not really a useful scenario: client is talking HTTP to proxy, and we should rewrite all HTTPS-based URLs...
            this.clientProto = HTTP;
            this.serverProto = HTTPS;
        }
        else
        {
            // Client is talking HTTPS to proxy, so we should rewrite all HTTP-based URLs... 
            this.clientProto = HTTPS;
            this.serverProto = HTTP;
        }

        int port;
        try
        {
            String fwdPort = request.getHeader(HDR_X_FORWARDED_PORT);
            port = Integer.valueOf(fwdPort);
        }
        catch (Exception e)
        {
            // Use default port for the used protocol...
            port = -1;
        }
        // Normalize the protocol port...
        if ((port > 0) && ((HTTPS.equals(this.clientProto) && (port == HTTPS_PORT)) || (HTTP.equals(this.clientProto) && (port == HTTP_PORT))))
        {
            // Port is the default one, do not use it...
            port = -1;
        }

        this.clientPort = port;
    }

    @Override
    public void setHeader(String name, String value)
    {
        if (HDR_LOCATION.equalsIgnoreCase(name))
        {
            URL rewritten = rewriteUrlIfNeeded(value);
            // Trying to set a redirect location to the original client-side URL, which should be https...
            if (rewritten != null)
            {
                value = rewritten.toExternalForm();
            }
        }
        super.setHeader(name, value);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        URL rewritten = rewriteUrlIfNeeded(location);
        if (rewritten != null)
        {
            location = rewritten.toExternalForm();
        }
        super.sendRedirect(location);
    }

    private int normalizePort(String protocol, int port)
    {
        if (port > 0)
        {
            return port;
        }
        if (HTTPS.equalsIgnoreCase(protocol))
        {
            return HTTPS_PORT;
        }
        return HTTP_PORT;
    }

    private URL rewriteUrlIfNeeded(String value)
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            URL url;
            if (value.startsWith(this.serverProto.concat("://")))
            {
                url = new URL(value);
            }
            else
            {
                url = new URL(this.requestURL, value);
            }

            String actualProto = url.getProtocol();

            if (!this.serverProto.equalsIgnoreCase(actualProto))
            {
                return null;
            }

            if (!this.serverName.equals(url.getHost()))
            {
                return null;
            }

            if (normalizePort(this.serverProto, this.serverPort) != normalizePort(actualProto, url.getPort()))
            {
                return null;
            }

            return new URL(this.clientProto, this.serverName, this.clientPort, url.getFile());
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }
}
