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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.PreprocessorInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.whiteboard.Preprocessor;

/**
 * The preprocessor handler handles the initialization and destruction of preprocessor
 * objects.
 */
public class PreprocessorHandler implements Comparable<PreprocessorHandler>
{
    private final PreprocessorInfo info;

    private final ServletContext context;

    private final BundleContext bundleContext;

    private volatile Preprocessor preprocessor;

    public PreprocessorHandler(final BundleContext bundleContext,
            final ServletContext context,
            final PreprocessorInfo info)
    {
        this.bundleContext = bundleContext;
        this.context = context;
        this.info = info;
    }

    @Override
    public int compareTo(final PreprocessorHandler other)
    {
        return this.info.compareTo(other.info);
    }

    public ServletContext getContext()
    {
        return this.context;
    }

    public PreprocessorInfo getPreprocessorInfo()
    {
        return this.info;
    }

    public int init()
    {
        final ServiceReference<Preprocessor> serviceReference = this.info.getServiceReference();
        this.preprocessor = this.bundleContext.getService(serviceReference);

        if (this.preprocessor == null)
        {
            return DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
        }

        try
        {
            this.preprocessor.init(new FilterConfigImpl(this.preprocessor.getClass().getName(),
                    getContext(),
                    getPreprocessorInfo().getInitParameters()));
        }
        catch (final Exception e)
        {
            SystemLogger.error(this.getPreprocessorInfo().getServiceReference(),
                    "Error during calling init() on preprocessor " + this.preprocessor,
                    e);

            this.preprocessor = null;
            this.bundleContext.ungetService(serviceReference);

            return DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
        }

        return -1;
    }

    public boolean destroy()
    {
        if (this.preprocessor == null)
        {
            return false;
        }

        try
        {
            preprocessor.destroy();
        }
        catch ( final Exception ignore )
        {
            // we ignore this
            SystemLogger.error(this.getPreprocessorInfo().getServiceReference(),
                    "Error during calling destroy() on preprocessor " + this.preprocessor,
                    ignore);
        }
        this.preprocessor = null;
        this.bundleContext.ungetService(this.info.getServiceReference());

        return true;
    }

    public void handle(@Nonnull final ServletRequest req,
            @Nonnull final ServletResponse res,
            @Nonnull final FilterChain chain) throws ServletException, IOException
    {
        this.preprocessor.doFilter(req, res, chain);
    }

    public boolean dispose()
    {
        // fully destroy the preprocessor
        return this.destroy();
    }

    @Override
    public int hashCode()
    {
        return 31 + info.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        final PreprocessorHandler other = (PreprocessorHandler) obj;
        return info.equals(other.info);
    }
}
