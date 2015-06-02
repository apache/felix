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

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.dto.DTO;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Provides registration information for a {@link Filter}, and is used to programmatically register {@link Filter}s.
 * <p>
 * This class only provides information used at registration time, and as such differs slightly from {@link DTO}s like, {@link FilterDTO}.
 * </p>
 */
public final class FilterInfo extends WhiteboardServiceInfo<Filter>
{
    /**
     * Properties starting with this prefix are passed as filter init parameters to the
     * {@code init()} method of the filter.
     */
    private static final String FILTER_INIT_PREFIX = "filter.init.";

    /**
     * The name of the filter.
     */
    private final String name;

    /**
     * The request mappings for the servlet.
     * <p>
     * The specified patterns are used to determine whether a request is mapped to the servlet filter.<br>
     * Note that these patterns should conform to the Servlet specification.
     * </p>
     */
    private final String[] patterns;

    /**
     * The servlet names for the servlet filter.
     * <p>
     * The specified names are used to determine the servlets whose requests are mapped to the servlet filter.
     * </p>
     */
    private final String[] servletNames;

    /**
     * The request mappings for the servlet filter.
     * <p>
     * The specified regular expressions are used to determine whether a request is mapped to the servlet filter.<br>
     * These regular expressions are a convenience extension allowing one to specify filters that match paths that are difficult to match with plain Servlet patterns alone.
     * </p>
     */
    private final String[] regexs;

    /**
     * Specifies whether the servlet filter supports asynchronous processing.
     */
    private final boolean asyncSupported;

    /**
     * The dispatcher associations for the servlet filter.
     * <p>
     * The specified names are used to determine in what occasions the servlet filter is called.
     * See {@link DispatcherType} and Servlet 3.0 specification, section 6.2.5.
     * </p>
     */
    private final DispatcherType[] dispatcher;

    /**
     * The filter initialization parameters as provided during registration of the filter.
     */
    private final Map<String, String> initParams;

    public FilterInfo(final ServiceReference<Filter> ref)
    {
        super(ref);
        this.name = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME);
        this.asyncSupported = getBooleanProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
        this.servletNames = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET);
        this.patterns = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
        this.regexs = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX);
        this.initParams = getInitParams(ref, FILTER_INIT_PREFIX);
        String[] dispatcherNames = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
        if (dispatcherNames != null && dispatcherNames.length > 0)
        {
            DispatcherType[] dispatchers = new DispatcherType[dispatcherNames.length];
            for (int i = 0; i < dispatchers.length; i++)
            {
                try
                {
                    dispatchers[i] = DispatcherType.valueOf(dispatcherNames[i].toUpperCase());
                }
                catch ( final IllegalArgumentException iae)
                {
                    dispatchers = null;
                    break;
                }
            }
            this.dispatcher = dispatchers;
        }
        else
        {
            this.dispatcher = new DispatcherType[] {DispatcherType.REQUEST};
        }
    }

    /**
     * Constructor for Http Service
     */
    public FilterInfo(final String name,
            final String regex,
            final int serviceRanking,
            final Map<String, String> initParams)
    {
        super(serviceRanking);
        this.name = name;
        this.patterns = null;
        this.servletNames = null;
        this.regexs = new String[] {regex};
        this.initParams = Collections.unmodifiableMap(initParams);
        this.asyncSupported = false;
        this.dispatcher = new DispatcherType[] {DispatcherType.REQUEST};
    }

    @Override
    public boolean isValid()
    {
        boolean valid = super.isValid() && (!isEmpty(this.patterns) || !isEmpty(this.regexs) || !isEmpty(this.servletNames));
        if ( valid )
        {
            if ( this.patterns != null )
            {
                for(final String p : this.patterns)
                {
                    if ( !PatternUtil.isValidPattern(p) )
                    {
                        valid = false;
                        break;
                    }
                }
            }
            if ( valid && this.regexs != null )
            {
                for(final String p : this.regexs)
                {
                    try
                    {
                        Pattern.compile(p);
                    }
                    catch ( final PatternSyntaxException pse)
                    {
                        valid = false;
                        break;
                    }
                }
            }
        }
        if ( valid )
        {
            if ( this.dispatcher == null || this.dispatcher.length == 0 )
            {
                valid = false;
            }
        }
        return valid;
    }

    public String getName()
    {
        return name;
    }

    public String[] getPatterns()
    {
        return patterns;
    }

    public String[] getServletNames()
    {
        return servletNames;
    }

    public String[] getRegexs()
    {
        return regexs;
    }

    public boolean isAsyncSupported()
    {
        return asyncSupported;
    }

    public DispatcherType[] getDispatcher()
    {
        return dispatcher;
    }

    /**
     * Returns an immutable map of the init parameters.
     */
    public Map<String, String> getInitParameters()
    {
        return initParams;
    }
}
