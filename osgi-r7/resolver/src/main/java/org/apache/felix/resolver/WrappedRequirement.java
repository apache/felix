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
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class WrappedRequirement implements Requirement
{
    private final Resource m_host;
    private final Requirement m_req;

    public WrappedRequirement(Resource host, Requirement req)
    {
        m_host = host;
        m_req = req;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final WrappedRequirement other = (WrappedRequirement) obj;
        if (m_host != other.m_host && (m_host == null || !m_host.equals(other.m_host)))
        {
            return false;
        }
        if (m_req != other.m_req && (m_req == null || !m_req.equals(other.m_req)))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 37 * hash + (m_host != null ? m_host.hashCode() : 0);
        hash = 37 * hash + (m_req != null ? m_req.hashCode() : 0);
        return hash;
    }

    public Requirement getDeclaredRequirement()
    {
        return m_req;
    }

    public Resource getResource()
    {
        return m_host;
    }

    public String getNamespace()
    {
        return m_req.getNamespace();
    }

    public Map<String, String> getDirectives()
    {
        return m_req.getDirectives();
    }

    public Map<String, Object> getAttributes()
    {
        return m_req.getAttributes();
    }

    @Override
    public String toString()
    {
        return "[" + m_host + "] "
            + getNamespace()
            + "; "
            + getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
    }
}