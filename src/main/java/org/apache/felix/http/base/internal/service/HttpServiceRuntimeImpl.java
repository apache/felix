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
package org.apache.felix.http.base.internal.service;

import static java.util.Collections.list;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.dto.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.RuntimeDTOBuilder;
import org.apache.felix.http.base.internal.whiteboard.ServletContextHelperManager;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

public final class HttpServiceRuntimeImpl implements HttpServiceRuntime
{
    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    private final HandlerRegistry registry;
    private final ServletContextHelperManager contextManager;

    public HttpServiceRuntimeImpl(HandlerRegistry registry,
            ServletContextHelperManager contextManager)
    {
        this.registry = registry;
        this.contextManager = contextManager;
    }

    public synchronized RuntimeDTO getRuntimeDTO()
    {
        RegistryRuntime runtime = contextManager.getRuntime(registry);
        RuntimeDTOBuilder runtimeDTOBuilder = new RuntimeDTOBuilder(runtime, attributes);
        return runtimeDTOBuilder.build();
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path)
    {
        return null;
    }

    public synchronized void setAttribute(String name, Object value)
    {
        attributes.put(name, value);
    }

    public synchronized void setAllAttributes(Dictionary<String, Object> attributes)
    {
        this.attributes.clear();
        for (String key :list(attributes.keys()))
        {
            this.attributes.put(key, attributes.get(key));
        }
    }

    public synchronized Dictionary<String, Object> getAttributes()
    {
        return attributes;
    }
}
