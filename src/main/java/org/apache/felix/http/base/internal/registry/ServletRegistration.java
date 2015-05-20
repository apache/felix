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
package org.apache.felix.http.base.internal.registry;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;

/**
 * A servlet is registered with a resolver and a servlet handler
 */
public class ServletRegistration
{
    private final ServletHandler handler;
    private final PathResolver resolver;

    public ServletRegistration(@Nonnull final ServletHandler handler, @Nonnull final PathResolver resolver)
    {
        this.handler = handler;
        this.resolver = resolver;
    }

    public ServletHandler getServletHandler()
    {
        return this.handler;
    }

    public PathResolver getPathResolver()
    {
        return this.resolver;
    }

    public PathResolution resolve(@Nonnull final String requestURI)
    {
        final PathResolution pr = this.resolver.match(requestURI);
        if ( pr != null )
        {
            pr.handler = this.handler;
        }
        return pr;
    }
}
