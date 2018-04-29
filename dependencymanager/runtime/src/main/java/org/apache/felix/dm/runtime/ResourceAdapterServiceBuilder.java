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
 * Class used to build a resource adapter service using metadata found from DependencyManager runtime
 * meta-inf descriptor.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceAdapterServiceBuilder extends AbstractBuilder
{
    private final static String TYPE = "ResourceAdapterService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void build(MetaData srvMeta, List<MetaData> depsMeta, Bundle b, DependencyManager dm) 
        throws Exception
    {
        String filter = srvMeta.getString(Params.filter, null);
        Class<?> implClass = b.loadClass(srvMeta.getString(Params.impl));
        String[] provides = srvMeta.getStrings(Params.provides, null);
        Dictionary<String, Object> properties = srvMeta.getDictionary(Params.properties, null);
        boolean propagate = "true".equals(srvMeta.getString(Params.propagate, "false"));
        String changed = srvMeta.getString(Params.changed, null /* no change callback if not specified explicitly */);
        Component c = dm.createResourceAdapterService(filter, propagate, null, changed);
        c.setInterface(provides, properties);
        String factoryMethod = srvMeta.getString(Params.factoryMethod, null);
        if (factoryMethod == null)
        {
            c.setImplementation(implClass);
        } 
        else
        {
            c.setFactory(implClass, factoryMethod);
        }
        setCommonServiceParams(c, srvMeta);
        c.setComposition(srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(c, b, dm, srvMeta, depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        c.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
        // Adds dependencies (except named dependencies, which are managed by the lifecycle handler).
        addUnamedDependencies(b, dm, c, srvMeta, depsMeta);
        dm.add(c);
    }    
}
