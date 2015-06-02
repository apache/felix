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

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.dto.DTO;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Provides registration information for a {@link Servlet}, and is used to programmatically register {@link Servlet}s.
 * <p>
 * This class only provides information used at registration time, and as such differs slightly from {@link DTO}s like, {@link ServletDTO}.
 * </p>
 */
public class ServletInfo extends WhiteboardServiceInfo<Servlet>
{
    /**
     * Properties starting with this prefix are passed as servlet init parameters to the
     * {@code init()} method of the servlet.
     */
    private static final String SERVLET_INIT_PREFIX = "servlet.init.";

    /**
     * The name of the servlet.
     */
    private final String name;

    /**
     * The request mappings for the servlet.
     * <p>
     * The specified patterns are used to determine whether a request is mapped to the servlet.
     * </p>
     */
    private final String[] patterns;

    /**
     * The error pages and/or codes.
     */
    private final String[] errorPage;

    /**
     * Specifies whether the servlet supports asynchronous processing.
     */
    private final boolean asyncSupported;

    /**
     * Specifies whether the info represents a resource.
     */
    private final boolean isResource;

    /**
     * The servlet initialization parameters as provided during registration of the servlet.
     */
    private final Map<String, String> initParams;

    private final String prefix;

    public ServletInfo(final ServiceReference<Servlet> ref)
    {
        super(ref);
        this.name = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
        this.errorPage = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);
        this.patterns = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
        this.asyncSupported = getBooleanProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
        this.initParams = getInitParams(ref, SERVLET_INIT_PREFIX);
        this.isResource = false;
        this.prefix = null;
    }

    @SuppressWarnings("unchecked")
    public ServletInfo(final ResourceInfo resource)
    {
        super(getRef(resource.getServiceReference()));
        this.name = null;
        this.patterns = resource.getPatterns();
        this.errorPage = null;
        this.asyncSupported = false;
        this.initParams = Collections.emptyMap();
        this.isResource = true;
        this.prefix = resource.getPrefix();
    }

    @SuppressWarnings("rawtypes")
    private static ServiceReference getRef(ServiceReference ref)
    {
        return ref;
    }

    /**
     * Constructor for Http Service
     */
    public ServletInfo(final String name,
            final String pattern,
            final Map<String, String> initParams)
    {
        super(Integer.MAX_VALUE);
        this.name = name;
        this.patterns = new String[] {pattern};
        this.initParams = Collections.unmodifiableMap(initParams);
        this.asyncSupported = true;
        this.errorPage = null;
        this.isResource = false;
        this.prefix = null;
    }

    @Override
    public boolean isValid()
    {
        boolean valid = super.isValid() && !(isEmpty(this.patterns) && isEmpty(this.errorPage));
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

    public String[] getErrorPage()
    {
        return errorPage;
    }

    public boolean isAsyncSupported()
    {
        return asyncSupported;
    }

    /**
     * Returns an unmodifiable map of the init parameters.
     * @return
     */
    public Map<String, String> getInitParameters()
    {
        return initParams;
    }

    public boolean isResource()
    {
        return isResource;
    }

    public String getPrefix()
    {
        return prefix;
    }
}
