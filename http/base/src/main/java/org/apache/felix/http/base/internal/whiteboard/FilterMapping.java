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
package org.apache.felix.http.base.internal.whiteboard;

import javax.servlet.Filter;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

public final class FilterMapping extends AbstractMapping
{
    private final Filter filter;
    private final FilterInfo filterInfo;

    public FilterMapping(Bundle bundle, Filter filter, FilterInfo filterInfo)
    {
        super(bundle);
        this.filter = filter;
        this.filterInfo = filterInfo;
    }

    @Override
    public void register(HttpService httpService)
    {
        if (!isRegistered() && (httpService instanceof HttpServiceImpl) && getContext() != null)
        {
            register((HttpServiceImpl) httpService);
        }
        else
        {
            // Warn the user that something strange is going on...
            SystemLogger.warning("Unable to register filter for " + this.filterInfo.name + ", as no ExtHttpService seems to be present!", null);
        }
    }

    @Override
    public void unregister(HttpService httpService)
    {
        if (isRegistered() && (httpService instanceof HttpServiceImpl))
        {
            unregister((HttpServiceImpl) httpService);
        }
        else
        {
            // Warn the user that something strange is going on...
            SystemLogger.warning("Unable to unregister filter for " + this.filterInfo.name + ", as no ExtHttpService seems to be present!", null);
        }
    }

    Filter getFilter()
    {
        return filter;
    }

    private void register(HttpServiceImpl httpService)
    {
        if (!isRegistered() && getContext() != null)
        {
            try
            {
                httpService.registerFilter(this.filter, this.filterInfo);
                setRegistered(true);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.warning("Failed to register filter for " + this.filterInfo.name, null);
                SystemLogger.debug("Failed to register filter for " + this.filterInfo.name + "; details:", e);
            }
        }
    }

    private void unregister(HttpServiceImpl httpService)
    {
        if (isRegistered())
        {
            try
            {
                httpService.unregisterFilter(this.filter);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.debug("Failed to unregister filter for " + this.filterInfo.name, e);
            }
            finally
            {
                // Best effort: avoid mappings that are registered which is reality aren't registered...
                setRegistered(false);
            }
        }
    }
}
