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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class SslFilterRequest extends HttpServletRequestWrapper
{

    // The HTTPS scheme name
    private static final String HTTPS_SCHEME = "https";

    // The HTTP scheme prefix in an URL
    private static final String HTTP_SCHEME_PREFIX = "http://";

    private String requestURL;

    SslFilterRequest(HttpServletRequest request)
    {
        super(request);
    }

    public String getScheme()
    {
        return HTTPS_SCHEME;
    }

    public boolean isSecure()
    {
        return true;
    }

    public StringBuffer getRequestURL()
    {
        if (this.requestURL == null)
        {
            StringBuffer tmp = super.getRequestURL();
            if (tmp.indexOf(HTTP_SCHEME_PREFIX) == 0)
            {
                this.requestURL = HTTPS_SCHEME.concat(tmp.substring(4));
            }
            else
            {
                this.requestURL = tmp.toString();
            }
        }

        return new StringBuffer(this.requestURL);
    }
}
