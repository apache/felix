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
 * Class used to build an aspect service using metadata found from DependencyManager runtime
 * meta-inf descriptor.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectServiceBuilder extends AbstractBuilder
{
    private final static String TYPE = "AspectService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void build(MetaData srvMeta, List<MetaData> depsMeta, Bundle b, DependencyManager dm)
        throws Exception
    {
        Log.instance().info("AspectServiceBuilder: building aspect service: %s with dependencies %s",
                            srvMeta,
                            depsMeta);

        Class<?> serviceInterface = b.loadClass(srvMeta.getString(Params.service));
        String serviceFilter = srvMeta.getString(Params.filter, null);
        Dictionary<String, Object> aspectProperties = srvMeta.getDictionary(Params.properties, null);
        int ranking = srvMeta.getInt(Params.ranking, 1);
        String implClassName = srvMeta.getString(Params.impl);
        Object implClass = b.loadClass(implClassName);
        String field = srvMeta.getString(Params.field, null);
        String added = srvMeta.getString(Params.added, null);
        String changed = srvMeta.getString(Params.changed, null);
        String removed = srvMeta.getString(Params.removed, null);
        String swap = srvMeta.getString(Params.swap, null);

        if (field != null && (added != null || changed != null || removed != null || swap != null))
        {
            throw new IllegalArgumentException("autoconfig field " + field + " can't be defined with both added/changed/removed/swap calllbacks");
        }

        Component c;
        if (field != null)
        {
            c = dm.createAspectService(serviceInterface, serviceFilter, ranking, field)
                  .setServiceProperties(aspectProperties);
        } 
        else
        {
            if (added != null || changed != null || removed != null || swap != null)
            {
                c = dm.createAspectService(serviceInterface, serviceFilter, ranking, added, changed, removed, swap)
                      .setServiceProperties(aspectProperties);
            } 
            else
            {
                c = dm.createAspectService(serviceInterface, serviceFilter, ranking)
                      .setServiceProperties(aspectProperties);
            }
 
        }
        
        setCommonServiceParams(c, srvMeta);
        String factoryMethod = srvMeta.getString(Params.factoryMethod, null);
        if (factoryMethod == null)
        {
            c.setImplementation(implClass);
        }
        else
        {
            c.setFactory(implClass, factoryMethod);
        }

        c.setComposition(srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(c, b, dm, srvMeta, depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        c.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
        // Adds dependencies (except named dependencies, which are managed by the lifecycle
        // handler).
        addUnamedDependencies(b, dm, c, srvMeta, depsMeta);
        dm.add(c);
    }
}
