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

/**
 * Provides constants used in the SSL filter.
 */
interface SslFilterConstants
{
    /**
     * If there is an SSL certificate associated with the request, it must be exposed by the servlet container to the 
     * servlet programmer as an array of objects of type java.security.cert.X509Certificate and accessible via a 
     * ServletRequest attribute of <tt>javax.servlet.request.X509Certificate</tt>.
     * <p>
     * The order of this array is defined as being in ascending order of trust. The first certificate in the chain is
     * the one set by the client, the next is the one used to authenticate the first, and so on.
     */
    String ATTR_SSL_CERTIFICATE = "javax.servlet.request.X509Certificate";

    /**
     * De-facto header used to inform what protocol the forwarded client used to connect to the proxy, such as "https".
     */
    String HDR_X_FORWARDED_PROTO = "X-Forwarded-Proto";
    /**
     * De-facto header used to inform what port the forwarded client used to connect to the proxy, such as "433".
     */
    String HDR_X_FORWARDED_PORT = "X-Forwarded-Port";
    /**
     * De-facto header used to inform that the proxy is forwarding a SSL request.
     */
    String HDR_X_FORWARDED_SSL = "X-Forwarded-SSL";
    /**
     * De-facto(?) header used to pass the certificate the client used to connect to the proxy, in X.509 format.
     */
    String HDR_X_FORWARDED_SSL_CERTIFICATE = "X-Forwarded-SSL-Certificate";

    /**
     * HTTP header used to explain the client it should redirect to another URL.
     */
    String HDR_LOCATION = "Location";

    /**
     * HTTP protocol/scheme.
     */
    String HTTP = "http";
    /**
     * Default port used for HTTP.
     */
    int HTTP_PORT = 80;

    /**
     * HTTPS protocol/scheme.
     */
    String HTTPS = "https";
    /**
     * Default port used for HTTPS.
     */
    int HTTPS_PORT = 443;

    String UTF_8 = "UTF-8";
    String X_509 = "X.509";
}
