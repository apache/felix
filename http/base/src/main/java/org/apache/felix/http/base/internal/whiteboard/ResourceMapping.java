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

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.runtime.dto.ResourceDTO;

public final class ResourceMapping extends AbstractMapping
{
    private final ResourceDTO dto;

    public ResourceMapping(final Bundle bundle, final ResourceDTO resourceDTO)
    {
        super(bundle);
        this.dto = resourceDTO;
    }

    @Override
    public void register(final HttpService httpService)
    {
        if (!isRegistered() && getContext() != null)
        {
            try {
                httpService.registerResources(this.dto.patterns[0], this.dto.prefix, this.getContext());
                this.setRegistered(true);
            }
            catch (final NamespaceException e)
            {
                // TODO Handle exception
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unregister(final HttpService httpService)
    {
        if (isRegistered())
        {
            httpService.unregister(this.dto.patterns[0]);
        }
    }
}
