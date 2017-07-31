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
import org.osgi.framework.BundleContext;
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
    private static final String PROP_CHANGECOUNT = "service.changecount";

    private static final String PROP_CHANGECOUNTDELAY = "org.apache.felix.http.whiteboard.changecount.delay";
    
    private volatile Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    private final HandlerRegistry registry;
    private final WhiteboardManager contextManager;

    private volatile long changeCount;

    private volatile ServiceReference<HttpServiceRuntime> serviceReference;

    private volatile Timer timer;

    private final long updateChangeCountDelay;
    
    public HttpServiceRuntimeImpl(HandlerRegistry registry,
            WhiteboardManager contextManager,
            BundleContext bundleContext)
    {
        this.registry = registry;
        this.contextManager = contextManager;
        final Object val = bundleContext.getProperty(PROP_CHANGECOUNTDELAY);
        long value = 2000L;
        if ( val != null ) 
        {
        	try 
        	{
        		value = Long.parseLong(val.toString());
        	} 
        	catch ( final NumberFormatException nfe) 
        	{
        		// ignore
        	}
        	if ( value < 1 )
        	{
        		value = 0L;
        	}
        }
    	updateChangeCountDelay = value;
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
                if ( this.updateChangeCountDelay > 0 && this.timer == null )
                {
                    this.timer = new Timer();
                }
            }
            if ( this.updateChangeCountDelay == 0L ) 
            {
                reg.setProperties(getAttributes());
            } 
            else 
            {
	            timer.schedule(new TimerTask()
	            {
	
	                @Override
	                public void run() 
	                {
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
	            }, this.updateChangeCountDelay);
            }
        }
    }
}
