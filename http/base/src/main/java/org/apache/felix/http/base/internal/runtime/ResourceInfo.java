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

import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Info object for a resource registration
 */
public final class ResourceInfo extends WhiteboardServiceInfo<Object>
{
    /**
     * The request mappings for the resource.
     */
    private final String[] patterns;

    /**
     * The error pages and/or codes.
     */
    private final String prefix;

    private final ServletInfo servletInfo;

    private static final class ResourceServletInfo extends ServletInfo {

        public ResourceServletInfo(ResourceInfo resource) {
            super(resource);
        }
    };

    public ResourceInfo(final ServiceReference<Object> ref)
    {
        super(ref);
        this.patterns = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
        this.prefix = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
        this.servletInfo = new ResourceServletInfo(this);
    }

    @Override
    public boolean isValid()
    {
        // TODO - do we need to check the prefix?
        boolean valid = super.isValid() && !isEmpty(this.patterns) && !isEmpty(this.prefix);
        if ( valid ) {
            for(final String p : patterns)
            {
                if ( !PatternUtil.isValidPattern(p) )
                {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    public String getPrefix()
    {
        return this.prefix;
    }

    public String[] getPatterns()
    {
        return patterns;
    }

    public ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }
}
