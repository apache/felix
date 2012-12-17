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
package org.apache.felix.httplite.servlet;

import java.util.Collections;
import java.util.Enumeration;

/**
 * Defines some commonly used HTTP constants and headers.
 */
public class HttpConstants
{
    /**
     * HTTP line delimiter
     */
    public static final String HEADER_DELEMITER = "\r\n";
    /**
     * HTTP header delimiter
     */
    public static final String HEADER_TERMINATOR = HEADER_DELEMITER + HEADER_DELEMITER;

    /**
     * Content-Length header
     */
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    /**
     * Location header
     */
    public static final String HEADER_LOCATION = "Location";
    /**
     * Content-Type header
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    /**
     * Connection header
     */
    public static final String HEADER_CONNECTION = "Connection";

    /**
     * For building HTML error messages, this value is the default start of the html document for error message responses.
     */
    public static final String DEFAULT_HTML_HEADER = "<html>";

    /**
     * HTTP header delimiter.
     */
    public static final String HEADER_VALUE_DELIMITER = ": ";

    /**
     * HTTP GET Method
     */
    public static final String GET_REQUEST = "GET";
    /**
     * HTTP HEAD Method
     */
    public static final String HEAD_REQUEST = "HEAD";
    /**
     * HTTP POST Method
     */
    public static final String POST_REQUEST = "POST";
    /**
     * HTTP PUT Method
     */
    public static final String PUT_REQUEST = "PUT";
    /**
     * HTTP DELETE Method
     */
    public static final String DELETE_REQUEST = "DELETE";
    /**
     * HTTP OPTIONS Method
     */
    public static final Object OPTIONS_REQUEST = "OPTIONS";

    /**
     * HTTP v 1.0
     */
    public static final String HTTP10_VERSION = "HTTP/1.0";
    /**
     * HTTP v 1.1
     */
    public static final String HTTP11_VERSION = "HTTP/1.1";
    /**
     * Host header
     */
    public static final String HOST_HEADER = "Host";

    /**
     * Keep-alive value for Connection header.
     */
    public static final String KEEPALIVE_CONNECTION = "keep-alive";
    /**
     * Close value for Connection header.
     */
    public static final String CLOSE_CONNECTION = "close";
    /**
     * Date format for HTTP
     */
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    /**
     * Timezone specified for HTTP
     */
    public static final String HTTP_TIMEZONE = "GMT";

    /**
     * Felix HTTP service property to enable HTTP server
     */
    public static final Object SERVICE_PROPERTY_KEY_HTTP_ENABLE = "org.apache.felix.http.enable";
    /**
     * Felix HTTP service property to enable HTTPS server (unimplemented)
     */
    public static final Object SERVICE_PROPERTY_KEY_HTTPS_ENABLE = "org.apache.felix.https.enable";
    /**
     * Felix HTTP property to configure server port.
     */
    public static final Object SERVICE_PROPERTY_KEY_HTTP_PORT = "org.osgi.service.http.port";

    /**
     * HTTP response code 100
     */
    public static final int HTTP_RESPONSE_CONTINUE = 100;
    /**
     * Servlet implementation name.
     */
    public static final String SERVER_INFO = "Apache Felix Lightweight HTTP Service";
    /**
     * HTTP Scheme
     */
    public static final String HTTP_SCHEME = "http";

    /**
     * Servlet API requires passing empty enumerations.
     */
    public static final Enumeration EMPTY_ENUMERATION = Collections.enumeration(Collections.EMPTY_SET);

}
