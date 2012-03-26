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
package org.apache.felix.resolver;

import java.util.Map;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.HostedCapability;

class SimpleHostedCapability implements HostedCapability
{
    private final Resource m_host;
    private final Capability m_cap;

    SimpleHostedCapability(Resource host, Capability cap)
    {
        m_host = host;
        m_cap = cap;
    }

    public Resource getResource()
    {
        return m_host;
    }

    public Capability getDeclaredCapability()
    {
        return m_cap;
    }

    public String getNamespace()
    {
        return m_cap.getNamespace();
    }

    public Map<String, String> getDirectives()
    {
        return m_cap.getDirectives();
    }

    public Map<String, Object> getAttributes()
    {
        return m_cap.getAttributes();
    }
}