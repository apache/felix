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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletInfo;

public final class ErrorPageRegistry
{
    private final Map<Integer, ServletHolder> errorCodesMap = new ConcurrentHashMap<Integer, ServletHolder>();
    private final Map<String, ServletHolder> exceptionsMap = new ConcurrentHashMap<String, ServletHolder>();

    public void addServlet(@Nonnull final ServletHolder holder)
    {
        if ( holder.getServletInfo().getErrorPage() != null )
        {

        }
    }

    public void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        if ( info.getErrorPage() != null )
        {

        }
    }

    /**
     * Get the servlet handling the error
     * @param exception Optional exception
     * @param errorCode Error code
     * @return The servlet handling the error or {@code null}
     */
    public ServletHolder get(final Throwable exception, final int errorCode)
    {
        ServletHolder errorHandler = this.get(exception);
        if (errorHandler != null)
        {
            return errorHandler;
        }

        return get(errorCode);
    }

    private ServletHolder get(final int errorCode)
    {
        return this.errorCodesMap.get(errorCode);
    }

    private ServletHolder get(final Throwable exception)
    {
        if (exception == null)
        {
            return null;
        }

        ServletHolder servletHandler = null;
        Class<?> throwableClass = exception.getClass();
        while ( servletHandler == null && throwableClass != null )
        {
            servletHandler = this.exceptionsMap.get(throwableClass.getName());
            if ( servletHandler == null )
            {
                throwableClass = throwableClass.getSuperclass();
                if ( !Throwable.class.isAssignableFrom(throwableClass) )
                {
                    throwableClass = null;
                }
            }

        }
        return servletHandler;
    }
}
