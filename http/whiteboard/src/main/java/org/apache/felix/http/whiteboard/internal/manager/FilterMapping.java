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
package org.apache.felix.http.whiteboard.internal.manager;

import javax.servlet.Filter;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

public final class FilterMapping extends AbstractMapping
{
    private final Filter filter;
    private final int ranking;
    private final String pattern;

    public FilterMapping(Bundle bundle, Filter filter, String pattern, int ranking)
    {
        super(bundle);
        this.filter = filter;
        this.pattern = pattern;
        this.ranking = ranking;
    }

    public void register(HttpService httpService)
    {
        if (httpService instanceof ExtHttpService)
        {
            register((ExtHttpService) httpService);
        }
        else
        {
            // Warn the user that something strange is going on...
            SystemLogger.warning("Unable to register filter for " + this.pattern + ", as no ExtHttpService seems to be present!", null);
        }
    }

    public void unregister(HttpService httpService)
    {
        if (httpService instanceof ExtHttpService)
        {
            unregister((ExtHttpService) httpService);
        }
        else
        {
            // Warn the user that something strange is going on...
            SystemLogger.warning("Unable to unregister filter for " + this.pattern + ", as no ExtHttpService seems to be present!", null);
        }
    }

    Filter getFilter()
    {
        return filter;
    }

    String getPattern()
    {
        return pattern;
    }

    int getRanking()
    {
        return ranking;
    }

    private void register(ExtHttpService httpService)
    {
        if (!isRegistered() && getContext() != null)
        {
            try
            {
                httpService.registerFilter(this.filter, this.pattern, getInitParams(), ranking, getContext());
                setRegistered(true);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.warning("Failed to register filter for " + this.pattern, null);
                SystemLogger.debug("Failed to register filter for " + this.pattern + "; details:", e);
            }
        }
    }

    private void unregister(ExtHttpService httpService)
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
                SystemLogger.debug("Failed to unregister filter for " + this.pattern, e);
            }
            finally
            {
                // Best effort: avoid mappings that are registered which is reality aren't registered...
                setRegistered(false);
            }
        }
    }
}
