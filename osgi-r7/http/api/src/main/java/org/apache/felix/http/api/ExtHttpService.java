/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.api;

import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * The {@link ExtHttpService} allows other bundles in the OSGi environment to dynamically
 * register resources, {@link Filter}s and {@link Servlet}s into the URI namespace of a
 * HTTP service implementation. A bundle may later unregister its resources, {@link Filter}s
 * or {@link Servlet}s.
 *
 * @see HttpContext
 * @deprecated The HTTP Whiteboard Service should be used instead.
 */
@Deprecated
@ProviderType
public interface ExtHttpService extends HttpService
{
    /**
     * Allows for programmatic registration of a {@link Filter} instance.
     *
     * @param filter the {@link Filter} to register, cannot be <code>null</code>;
     * @param pattern the filter pattern to register the for, cannot be <code>null</code> and
     *        should be a valid regular expression;
     * @param initParams the initialization parameters passed to the given filter during its
     *        initialization, can be <code>null</code> in case no additional parameters should
     *        be provided;
     * @param ranking defines the order in which filters are called. A higher ranking causes
     *        the filter to be placed earlier in the filter chain;
     * @param context the optional {@link HttpContext} to associate with this filter, can be
     *        <code>null</code> in case a default context should be associated.
     * @throws ServletException in case the registration failed, for example because the
     *         initialization failed or due any other problem;
     * @throws IllegalArgumentException in case the given filter was <code>null</code>.
     */
    void registerFilter(Filter filter, String pattern, Dictionary initParams, int ranking, HttpContext context) throws ServletException;

    /**
     * Unregisters a previously registered {@link Filter}.
     * <p>
     * In case the given filter is not registered, this method is essentially a no-op.
     * </p>
     *
     * @param filter the {@link Filter} to unregister, cannot be <code>null</code>.
     */
    void unregisterFilter(Filter filter);

    /**
     * Unregisters a previously registered {@link Servlet}.
     * <p>
     * In case the given servlet is not registered, this method is essentially a no-op.
     * </p>
     *
     * @param servlet the {@link Servlet} to unregister, cannot be <code>null</code>.
     */
    void unregisterServlet(Servlet servlet);
}
