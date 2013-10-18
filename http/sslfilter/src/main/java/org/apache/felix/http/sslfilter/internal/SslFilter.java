/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.sslfilter.internal;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class SslFilter implements Filter
{

    // request header indicating an SSL endpoint proxy
    private static final String X_FORWARD_SSL_HEADER = "X-Forwarded-SSL";

    // value indicating an SSL endpoint proxy
    private static final String X_FORWARD_SSL_VALUE = "on";

    // request header indicating an SSL client certificate (if available)
    private static final String X_FORWARD_SSL_CERTIFICATE_HEADER = "X-Forwarded-SSL-Certificate";

    public void init(FilterConfig config)
    {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException
    {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        if (X_FORWARD_SSL_VALUE.equalsIgnoreCase(httpReq.getHeader(X_FORWARD_SSL_HEADER)))
        {
            httpReq = new SslFilterRequest(httpReq, httpReq.getHeader(X_FORWARD_SSL_CERTIFICATE_HEADER));
        }

        // forward the request making sure any certificate is removed
        // again after the request processing gets back here
        try
        {
            chain.doFilter(httpReq, res);
        }
        finally
        {
            if (httpReq instanceof SslFilterRequest)
            {
                ((SslFilterRequest) httpReq).done();
            }
        }
    }

    public void destroy()
    {
    }

}
