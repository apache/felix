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

public final class FilterMapping
    extends AbstractMapping
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

    Filter getFilter()
    {
        return filter;
    }

    int getRanking()
    {
        return ranking;
    }

    String getPattern()
    {
        return pattern;
    }

    public void register(HttpService httpService)
    {
       if (httpService instanceof ExtHttpService) {
            register((ExtHttpService)httpService);
        }
    }

    private void register(ExtHttpService httpService)
    {
        if (!this.isRegistered() && getContext() != null)
        {
            try
            {
                httpService.registerFilter(this.filter, this.pattern, getInitParams(), ranking, getContext());
                setRegistered(true);
            }
            catch (Exception e)
            {
                SystemLogger.error("Failed to register filter", e);
            }
        }
    }

    public void unregister(HttpService httpService)
    {
        if (httpService instanceof ExtHttpService && this.isRegistered())
        {
            unregister((ExtHttpService) httpService);
            setRegistered(false);
        }
    }

    private void unregister(ExtHttpService httpService)
    {
        httpService.unregisterFilter(this.filter);
    }
}
