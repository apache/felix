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
package org.apache.felix.bundlerepository.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class OSGiResourceImpl implements Resource
{
    private final List<Capability> capabilities;
    private final List<Requirement> requirements;

    @SuppressWarnings("unchecked")
    public OSGiResourceImpl(List<? extends Capability> caps, List<? extends Requirement> reqs)
    {
        capabilities = (List<Capability>) caps;
        requirements = (List<Requirement>) reqs;
    }

    public List<Capability> getCapabilities(String namespace)
    {
        if (namespace == null)
            return capabilities;

        List<Capability> caps = new ArrayList<Capability>();
        for(Capability cap : capabilities)
        {
            if (namespace.equals(cap.getNamespace()))
            {
                caps.add(cap);
            }
        }
        return caps;
    }

    public List<Requirement> getRequirements(String namespace)
    {
        if (namespace == null)
            return requirements;

        List<Requirement> reqs = new ArrayList<Requirement>();
        for(Requirement req : requirements)
        {
            if (namespace.equals(req.getNamespace()))
            {
                reqs.add(req);
            }
        }
        return reqs;
    }

    // TODO implement equals and hashcode
}
