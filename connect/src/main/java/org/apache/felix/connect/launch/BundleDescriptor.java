/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect.launch;

import java.util.Map;

import org.apache.felix.connect.Revision;

public class BundleDescriptor
{
    private final ClassLoader m_loader;
    private final String m_url;
    private final Map<String, String> m_headers;
    private final Revision m_revision;
    private final Map<Class, Object> m_services;

    public BundleDescriptor(ClassLoader loader, String url,
                            Map<String, String> headers)
    {
        this(loader, url, headers, null, null);
    }

    public BundleDescriptor(ClassLoader loader, String url,
                            Map<String, String> headers,
                            Revision revision,
                            Map<Class, Object> services)
    {
        m_loader = loader;
        m_url = url;
        m_headers = headers;
        m_revision = revision;
        m_services = services;
    }

    public ClassLoader getClassLoader()
    {
        return m_loader;
    }

    public String getUrl()
    {
        return m_url;
    }

    public String toString()
    {
        return m_url;
    }

    public Map<String, String> getHeaders()
    {
        return m_headers;
    }

    public Revision getRevision()
    {
        return m_revision;
    }

    public Map<Class, Object> getServices()
    {
        return m_services;
    }
}
