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

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.LocalResource;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import java.util.Map;

public class LazyLocalResourceImpl implements LocalResource
{
    private final Bundle m_bundle;
    private final Logger m_logger;
    private volatile Resource m_resource = null;

    LazyLocalResourceImpl(Bundle bundle, Logger logger)
    {
        m_bundle = bundle;
        m_logger = logger;
    }

    public boolean isLocal()
    {
        return true;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public String toString()
    {
        return m_bundle.toString();
    }

    private final Resource getResource() {
        if (m_resource == null) {
            synchronized (this) {
                try {
                    m_resource = new LocalResourceImpl(m_bundle);
                } catch (InvalidSyntaxException ex) {
                    // This should never happen since we are generating filters,
                    // but ignore the resource if it does occur.
                    m_logger.log(Logger.LOG_WARNING, ex.getMessage(), ex);
                    m_resource = new ResourceImpl();
                }
            }
        }
        return m_resource;
    }

    public Map getProperties() {
        return getResource().getProperties();
    }

    public String getId() {
        return getResource().getId();
    }

    public String getSymbolicName() {
        return getResource().getSymbolicName();
    }

    public Version getVersion() {
        return getResource().getVersion();
    }

    public String getPresentationName() {
        return getResource().getPresentationName();
    }

    public String getURI() {
        return getResource().getURI();
    }

    public Long getSize() {
        return getResource().getSize();
    }

    public String[] getCategories() {
        return getResource().getCategories();
    }

    public Capability[] getCapabilities() {
        return getResource().getCapabilities();
    }

    public Requirement[] getRequirements() {
        return getResource().getRequirements();
    }
}
