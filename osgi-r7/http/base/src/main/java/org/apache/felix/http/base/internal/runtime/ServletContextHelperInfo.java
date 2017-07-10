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

import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Provides registration information for a {@link ServletContextHelper}
 */
public final class ServletContextHelperInfo extends AbstractInfo<ServletContextHelper>
{

    /**
     * Properties starting with this prefix are passed as context init parameters.
     */
    private static final String CONTEXT_INIT_PREFIX = "context.init.";

    private final String name;

    private final String path;

    /**
     * The filter initialization parameters as provided during registration of the filter.
     */
    private final Map<String, String> initParams;

    public ServletContextHelperInfo(final ServiceReference<ServletContextHelper> ref)
    {
        super(ref);
        this.name = this.getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
        this.path = this.getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
        this.initParams = getInitParams(ref, CONTEXT_INIT_PREFIX);
    }

    public ServletContextHelperInfo(final int serviceRanking,
            final long serviceId,
            final String name,
            final String path,
            final Map<String, String> initParams)
    {
        super(serviceRanking, serviceId);
        this.name = name;
        this.path = path;
        this.initParams = initParams == null ? Collections.<String, String>emptyMap(): Collections.unmodifiableMap(initParams);
    }

    private boolean isValidPath()
    {
        if (!this.isEmpty(path))
        {
            if (path.equals("/"))
            {
                return true;
            }
            // TODO we need more validation
            if (path.startsWith("/") && !path.endsWith("/"))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValid()
    {
        return super.isValid()
                && PatternUtil.isValidSymbolicName(this.name)
                && !HttpServiceFactory.HTTP_SERVICE_CONTEXT_NAME.equals(this.name)
                && isValidPath();
    }

    public String getName()
    {
        return this.name;
    }

    public String getPath()
    {
        return this.path;
    }

    /**
     * Returns an unmodifiable map of the parameters.
     * @return
     */
    public Map<String, String> getInitParameters()
    {
        return initParams;
    }
}
