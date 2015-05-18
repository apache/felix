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
package org.apache.felix.http.base.internal.runtime.dto.state;

import java.util.Comparator;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;

public class ServletState
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    public static final Comparator<ServletState> COMPARATOR = new Comparator<ServletState>()
    {
        @Override
        public int compare(ServletState o1, ServletState o2)
        {
            return o1.getServletInfo().compareTo(o2.getServletInfo());
        }
    };

    private final ServletHandler handler;

    private final ServletInfo info;

    private String[] patterns = EMPTY_STRING_ARRAY;

    private String[] exceptions = EMPTY_STRING_ARRAY;

    private long[] errorCodes = EMPTY_LONG_ARRAY;

    public ServletState(final ServletHandler handler)
    {
        this.handler = handler;
        this.info = handler.getServletInfo();
    }

    public ServletState(final ServletInfo info)
    {
        this.handler = null;
        this.info = info;
    }

    public Servlet getServlet()
    {
        return (handler != null ? this.handler.getServlet() : null);
    }

    public ServletInfo getServletInfo()
    {
        return this.info;
    }

    public String[] getPatterns()
    {
        return this.patterns;
    }

    public long[] getErrorCodes()
    {
        return this.errorCodes;
    }

    public String[] getErrorExceptions()
    {
        return this.exceptions;
    }

    public void setPatterns(final String[] value)
    {
        this.patterns = value;
    }

    public void setErrorCodes(final long[] value)
    {
        this.errorCodes = value;
    }

    public void setErrorExceptions(final String[] value)
    {
        this.exceptions = value;
    }
}
