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
package org.apache.felix.http.base.internal.runtime;

import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.osgi.dto.DTO;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.runtime.dto.FilterDTO;

import aQute.bnd.annotation.ConsumerType;

/**
 * Provides registration information for a {@link Filter}, and is used to programmatically register {@link Filter}s.
 * <p>
 * This class only provides information used at registration time, and as such differs slightly from {@link DTO}s like, {@link FilterDTO}.
 * </p>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@ConsumerType
public final class FilterInfo
{
    /**
     * The name of the servlet.
     */
    public String name;

    /**
     * The request mappings for the servlet.
     * <p>
     * The specified patterns are used to determine whether a request is mapped to the servlet filter.<br>
     * Note that these patterns should conform to the Servlet specification.
     * </p>
     */
    public String[] patterns;

    /**
     * The servlet names for the servlet filter.
     * <p>
     * The specified names are used to determine the servlets whose requests are mapped to the servlet filter.
     * </p>
     */
    public String[] servletNames;

    /**
     * The request mappings for the servlet filter.
     * <p>
     * The specified regular expressions are used to determine whether a request is mapped to the servlet filter.<br>
     * These regular expressions are a convenience extension allowing one to specify filters that match paths that are difficult to match with plain Servlet patterns alone.
     * </p>
     */
    public String[] regexs;

    /**
     * Specifies whether the servlet filter supports asynchronous processing.
     */
    public boolean asyncSupported = false;

    /**
     * Specifies the ranking order in which this filter should be called. Higher rankings are called first.
     */
    public int ranking = 0;
    public long serviceId;

    /**
     * The dispatcher associations for the servlet filter.
     * <p>
     * The specified names are used to determine in what occasions the servlet filter is called.
     * See {@link DispatcherType} and Servlet 3.0 specification, section 6.2.5.
     * </p>
     */
    public DispatcherType[] dispatcher = { DispatcherType.REQUEST };

    /**
     * The filter initialization parameters as provided during registration of the filter.
     */
    public Map<String, String> initParams;

    /**
     * The {@link HttpContext} for the servlet.
     */
    public HttpContext context;

}
