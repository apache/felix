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
package org.apache.felix.http.whiteboard;

/**
 * The <code>HttpWhiteboardConstants</code> defines constants for values
 * used by the Http Whiteboard registration support.
 *
 * @since Http Whiteboard Bundle 2.3.0
 */
public class HttpWhiteboardConstants
{

    /**
     * The service registration property indicating the name of a
     * <code>HttpContext</code> service.
     * <p>
     * If the property is set to a non-empty string for an
     * <code>HttpContext</code> service it indicates the name by which it may be
     * referred to by <code>Servlet</code> and <code>Filter</code> services.
     * This is also a required registration property for
     * <code>HttpService</code> services to be accepted by the Http Whiteboard
     * registration.
     * <p>
     * If the property is set for a <code>Servlet</code> or <code>Filter</code>
     * services it indicates the name of a registered <code>HttpContext</code>
     * which is to be used for the registration with the Http Service. If the
     * property is not set for a <code>Servlet</code> or <code>Filter</code>
     * services or its value is the empty string, a default HttpContext is used
     * which does no security handling and has no MIME type support and which
     * returns resources from the servlet's or the filter's bundle.
     * <p>
     * The value of this service registration property is a single string.
     */
    public static final String CONTEXT_ID = "contextId";

    /**
     * The service registration property indicating whether a
     * <code>HttpContext</code> service registered with the {@link #CONTEXT_ID}
     * service registration
     * property is shared across bundles or not. By default
     * <code>HttpContext</code> services are only available to
     * <code>Servlet</code> and <code>Filter</code> services registered by the
     * same bundle.
     * <p>
     * If this property is set to <code>true</code> for <code>HttpContext</code>
     * service, it may be referred to by <code>Servlet</code> or
     * <code>Filter</code> services from different bundles.
     * <p>
     * <b>Recommendation:</b> Shared <code>HttpContext</code> services should
     * either not implement the <code>getResource</code> at all or be registered
     * as service factories to ensure no access to foreign bundle resources is
     * not allowed through this backdoor.
     * <p>
     * The value of this service registration is a single boolean or string.
     * Only if the boolean value is <code>true</code> (either by
     * <code>Boolean.booleanValue()</code> or by
     * <code>Boolean.valueOf(String)</code>) will the <code>HttpContext</code>
     * be shared.
     */
    public static final String CONTEXT_SHARED = "context.shared";

    /**
     * The service registration property indicating the registration alias
     * for a <code>Servlet</code> service. This value is used as the
     * alias parameter for the <code>HttpService.registerServlet</code> call.
     * <p>
     * A <code>Servlet</code> service registered with this service property may
     * also provide a {@link #CONTEXT_ID} property which referrs to a
     * <code>HttpContext</code> service. If such a service is not registered
     * (yet), the servlet will not be registered with the Http Service. Once the
     * <code>HttpContext</code> service becomes available, the servlet is
     * registered.
     * <p>
     * The value of this service registration property is a single string
     * starting with a slash.
     */
    public static final String ALIAS = "alias";

    /**
     * The service registration property indicating the URL patter
     * for a <code>Filter</code> service. This value is used as the
     * pattern parameter for the <code>ExtHttpService.registerFilter</code>
     * call.
     * <p>
     * A <code>Filter</code> service registered with this service property may
     * also provide a {@link #CONTEXT_ID} property which referrs to a
     * <code>HttpContext</code> service. If such a service is not registered
     * (yet), the filter will not be registered with the Http Service. Once the
     * <code>HttpContext</code> service becomes available, the filter is
     * registered.
     * <p>
     * The value of this service registration property is a single string being
     * a regular expression.
     * <p>
     * <b>Note:</b> <code>Filter</code> services are only supported if the Http
     * Service implements the
     * <code>org.apache.felix.http.api.ExtHttpService</code> interface.
     */
    public static final String PATTERN = "pattern";

    /**
     * Prefix for service registration properties being used as init parameters
     * for the <code>Servlet</code> and <code>Filter</code> initialization.
     */
    public static final String INIT_PREFIX = "init.";

    // no instances
    private HttpWhiteboardConstants()
    {
    }
}
