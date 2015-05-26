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
package org.apache.felix.http.base.internal.handler;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;

public interface ContextHandler
{

    ServletContextHelperInfo getContextInfo();

    /**
     * Create a servlet handler
     * @param servletInfo The servlet info
     * @return {@code null} if the servlet context could not be created, a handler otherwise
     */
    ServletHandler getServletContextAndCreateServletHandler(@Nonnull final ServletInfo servletInfo);

    /**
     * Create a filter handler
     * @param info The filter info
     * @return {@code null} if the servlet context could not be created, a handler otherwise
     */
    FilterHandler getServletContextAndCreateFilterHandler(@Nonnull final FilterInfo info);

    /**
     * Create a listener handler
     * @param info The listener info
     * @return {@code null} if the servlet context could not be created, a handler otherwise
     */
    ListenerHandler getServletContextAndCreateListenerHandler(@Nonnull final ListenerInfo info);

    void ungetServletContext(@Nonnull final WhiteboardServiceInfo<?> info);

    PerContextHandlerRegistry getRegistry();
}
