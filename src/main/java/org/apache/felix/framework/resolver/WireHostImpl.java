/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.felix.framework.resolver;

import java.net.URL;
import java.util.Enumeration;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;

class WireHostImpl implements Wire
{
    private final Module m_importer;
    private final Requirement m_req;
    private final Module m_exporter;
    private final Capability m_cap;

    public WireHostImpl(Module importer, Requirement ip, Module exporter, Capability ep)
    {
        m_importer = importer;
        m_req = ip;
        m_exporter = exporter;
        m_cap = ep;
    }

    public Module getImporter()
    {
        return m_importer;
    }

    public Requirement getRequirement()
    {
        return m_req;
    }

    public Module getExporter()
    {
        return m_exporter;
    }

    public Capability getCapability()
    {
        return m_cap;
    }

    public String toString()
    {
        return m_importer
            + " -> hosted by -> "
            + m_exporter;
    }

    public boolean hasPackage(String pkgName)
    {
        return false;
    }

    public Class getClass(String name) throws ClassNotFoundException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URL getResource(String name) throws ResourceNotFoundException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Enumeration getResources(String name) throws ResourceNotFoundException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}