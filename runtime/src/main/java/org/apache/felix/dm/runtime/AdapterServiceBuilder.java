/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.runtime;

import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

/**
 * Builded called when the JSON parser find an adapter service descriptor.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterServiceBuilder extends AbstractBuilder
{
    /** The type attribute specified in the JSON descriptor */
    private final static String TYPE = "AdapterService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void build(MetaData srvMeta, List<MetaData> depsMeta, Bundle b, DependencyManager dm)
        throws Exception
    {
        Class<?> adapterImplClass = b.loadClass(srvMeta.getString(Params.impl));
        String[] provides = srvMeta.getStrings(Params.provides, null);
        Dictionary<String, Object> adapterProperties = srvMeta.getDictionary(Params.properties, null);
        Class<?> adapteeService = b.loadClass(srvMeta.getString(Params.adapteeService));
        String adapteeFilter = srvMeta.getString(Params.adapteeFilter, null);   
        String field = srvMeta.getString(Params.field, null);
        String added = srvMeta.getString(Params.added, null);
        String changed = srvMeta.getString(Params.changed, null);
        String removed = srvMeta.getString(Params.removed, null);
        
        if (field != null && (added != null || changed != null || removed != null))
        {
            throw new IllegalArgumentException("autoconfig field " + field + " cant be defined with both added/changed/removed calllbacks");
        }
        if (field == null && added == null && (changed != null || removed != null))
        {
            throw new IllegalArgumentException("missing added callback");
        }
        
        Component service;
        
        if (field != null)
        {
            service = dm.createAdapterService(adapteeService, adapteeFilter, field);
        }
        else
        {
            if (added != null)
            {
                service = dm.createAdapterService(adapteeService, adapteeFilter, added, changed, removed);

            }
            else
            {
                service = dm.createAdapterService(adapteeService, adapteeFilter);
            }
        }
        
        service.setInterface(provides, adapterProperties);
        
        String factoryMethod = srvMeta.getString(Params.factoryMethod, null);
        if (factoryMethod == null)
        {
            service.setImplementation(adapterImplClass);
        } 
        else
        {
            service.setFactory(adapterImplClass, factoryMethod);
        }
        service.setComposition(srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(service, b, dm, srvMeta, depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        service.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
        // Adds dependencies (except named dependencies, which are managed by the lifecycle handler).
        addUnamedDependencies(b, dm, service, srvMeta, depsMeta);
        dm.add(service);
    }
}
