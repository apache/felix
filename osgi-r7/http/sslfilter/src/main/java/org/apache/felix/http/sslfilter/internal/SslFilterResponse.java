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
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_SSL;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTP;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS_PORT;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTP_PORT;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.felix.http.sslfilter.internal.SslFilter.ConfigHolder;

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

    private final boolean rewriteAbsoluteUrls;

    public SslFilterResponse(HttpServletResponse response, HttpServletRequest request, ConfigHolder config) throws MalformedURLException
    {
        super(response);

        this.requestURL = new URL(request.getRequestURL().toString());

        // Only rewrite URLs for the host & port the request was sent to...
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();

        String value = request.getHeader(config.sslHeader);

        if ((HDR_X_FORWARDED_PROTO.equalsIgnoreCase(config.sslHeader) && HTTP.equalsIgnoreCase(value)) ||
                (HDR_X_FORWARDED_SSL.equalsIgnoreCase(config.sslHeader) && !config.sslValue.equalsIgnoreCase(value)))
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
        this.rewriteAbsoluteUrls = config.rewriteAbsoluteUrls;
    }

    @Override
    public void setHeader(String name, String value)
    {
        if (HDR_LOCATION.equalsIgnoreCase(name))
        {
        	String rewritten = null;
        	try {
        		rewritten = rewriteUrlIfNeeded(value);
        	} catch (URISyntaxException e) {
        		// ignore
        	}
            // Trying to set a redirect location to the original client-side URL, which should be https...
            if (rewritten != null)
            {
                value = rewritten;
            }
        }
        super.setHeader(name, value);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
    	String rewritten = null;
    	try {
    		rewritten = rewriteUrlIfNeeded(location);
    	} catch (URISyntaxException e) {
    		throw new IOException (e);
    	}
        if (rewritten != null)
        {
            location = rewritten;
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

    private String rewriteUrlIfNeeded(String value) throws URISyntaxException
    {
        if (value == null || (!this.rewriteAbsoluteUrls && value.contains("://")) )
        {
            return null;
        }

        try
        {
            URI uri;
            if (value.startsWith(this.serverProto.concat("://")))
            {

                uri = new URI (value);
            }
            else
            {
                URL url = new URL(this.requestURL, value);
                uri = url.toURI();
            }

            String actualProto = uri.getScheme();

            if (!this.serverName.equals(uri.getHost()))
            {
                // going to a different host
                return null;
            }

            if (normalizePort(this.serverProto, this.serverPort) != normalizePort(actualProto, uri.getPort()))
            {
                // not to default port
                return null;
            }

            final StringBuilder sb = new StringBuilder();
            sb.append(this.clientProto);
            sb.append("://");
            sb.append(this.serverName);
            if ( this.clientPort != -1 )
            {
                sb.append(':');
                sb.append(this.clientPort);
            }
            if ( uri.getRawPath() != null )
            {
                sb.append(uri.getRawPath());
            }
            if ( uri.getRawQuery() != null )
            {
                sb.append('?');
                sb.append(uri.getRawQuery());
            }
            if ( uri.getRawFragment() != null )
            {
                sb.append('#');
                sb.append(uri.getRawFragment());
            }
            return sb.toString();
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }



}
