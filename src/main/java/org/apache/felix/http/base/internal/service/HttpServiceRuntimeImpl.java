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
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.dto.RequestInfoDTOBuilder;
import org.apache.felix.http.base.internal.runtime.dto.RuntimeDTOBuilder;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

public final class HttpServiceRuntimeImpl implements HttpServiceRuntime
{
    /**
     * Service property for change count. This constant is defined here to avoid
     * a dependency on R7 of the framework.
     * The value of the property is of type {@code Long}.
     */
    private static String PROP_CHANGECOUNT = "service.changecount";

    private volatile Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    private final HandlerRegistry registry;
    private final WhiteboardManager contextManager;

    private volatile long changeCount;

    private volatile ServiceReference<HttpServiceRuntime> serviceReference;

    private volatile Timer timer;

    public HttpServiceRuntimeImpl(HandlerRegistry registry,
            WhiteboardManager contextManager)
    {
        this.registry = registry;
        this.contextManager = contextManager;
    }

    @Override
    public RuntimeDTO getRuntimeDTO()
    {
        final RuntimeDTOBuilder runtimeDTOBuilder = new RuntimeDTOBuilder(contextManager.getRuntimeInfo(),
                this.serviceReference);
        return runtimeDTOBuilder.build();
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(final String path)
    {
        return new RequestInfoDTOBuilder(registry, path).build();
    }

    public synchronized void setAttribute(String name, Object value)
    {
        Hashtable<String, Object> newAttributes = new Hashtable<String, Object>(attributes);
        newAttributes.put(name, value);
        attributes = newAttributes;
    }

    public synchronized void setAllAttributes(Dictionary<String, Object> newAttributes)
    {
        Hashtable<String, Object> replacement = new Hashtable<String, Object>();
        for (String key : list(newAttributes.keys()))
        {
            replacement.put(key, newAttributes.get(key));
        }
        replacement.put(PROP_CHANGECOUNT, this.changeCount);
        attributes = replacement;
    }

    public Dictionary<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setServiceReference(
            final ServiceReference<HttpServiceRuntime> reference)
    {
        this.serviceReference = reference;
    }

    public void updateChangeCount(final ServiceRegistration<HttpServiceRuntime> reg)
    {
        if ( reg != null )
        {
            final long count;
            synchronized ( this )
            {
                this.changeCount++;
                count = this.changeCount;
                this.setAttribute(PROP_CHANGECOUNT, this.changeCount);
                if ( this.timer == null )
                {
                    this.timer = new Timer();
                }
            }
            timer.schedule(new TimerTask()
            {

                @Override
                public void run() {
                    synchronized ( HttpServiceRuntimeImpl.this )
                    {
                        if ( changeCount == count )
                        {
                            reg.setProperties(getAttributes());
                            timer.cancel();
                            timer = null;
                        }
                    }
                }
            }, 2000L);
        }
    }
}
