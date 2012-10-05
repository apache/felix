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

public class SslFilterRequest extends HttpServletRequestWrapper
{

    private String requestURL;

    public SslFilterRequest(HttpServletRequest request)
    {
        super(request);

        // TODO: check request headers for SSL attribute information
    }

    public String getScheme()
    {
        return "https";
    }

    public boolean isSecure()
    {
        return true;
    }

    public StringBuffer getRequestURL()
    {
        if (this.requestURL == null) {
            // insert an 's' after the http scheme
            StringBuffer tmp = super.getRequestURL();
            tmp.insert(4, 's');
            this.requestURL = tmp.toString();
        }

        return new StringBuffer(this.requestURL);
    }
}
